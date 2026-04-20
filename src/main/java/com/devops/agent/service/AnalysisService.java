package com.devops.agent.service;

import com.devops.agent.model.AnalysisResult;
import com.devops.agent.model.ProjectContext;
import com.devops.agent.util.ProjectScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);
    private static final long MAX_UPLOAD_BYTES = 50 * 1024 * 1024;

    private final GitService gitService;
    private final AiService aiService;
    private final FallbackGenerator fallbackGenerator;

    public AnalysisService(GitService gitService, AiService aiService, FallbackGenerator fallbackGenerator) {
        this.gitService = gitService;
        this.aiService = aiService;
        this.fallbackGenerator = fallbackGenerator;
    }

    @Cacheable(value = "repoAnalysis", key = "#repoUrl")
    public AnalysisResult analyzeRepo(String repoUrl) {
        Path clonedDir = null;
        try {
            clonedDir = gitService.cloneRepo(repoUrl);
            ProjectContext ctx = ProjectScanner.scan(clonedDir);
            return runAnalysis(ctx);
        } catch (Exception e) {
            log.error("Repo analysis failed: {}", e.getMessage());
            throw new RuntimeException("Failed to analyze repository: " + e.getMessage());
        } finally {
            gitService.cleanup(clonedDir);
        }
    }

    public AnalysisResult analyzeUpload(MultipartFile file) {
        if (file.isEmpty()) throw new IllegalArgumentException("Uploaded file is empty");
        if (file.getSize() > MAX_UPLOAD_BYTES) throw new IllegalArgumentException("File too large (max 50MB)");

        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".zip")) {
            throw new IllegalArgumentException("Only ZIP files are supported");
        }

        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("devops-upload-");
            extractZip(file.getInputStream(), tempDir);

            Path projectRoot = findProjectRoot(tempDir);
            ProjectContext ctx = ProjectScanner.scan(projectRoot);
            return runAnalysis(ctx);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Upload analysis failed: {}", e.getMessage());
            throw new RuntimeException("Failed to analyze upload: " + e.getMessage());
        } finally {
            gitService.cleanup(tempDir);
        }
    }

    private AnalysisResult runAnalysis(ProjectContext ctx) {
        AnalysisResult aiResult = aiService.analyze(ctx);
        if (aiResult != null) {
            log.info("AI analysis succeeded for project type: {}", ctx.getProjectType());
            return aiResult;
        }
        log.info("Using fallback generator for project type: {}", ctx.getProjectType());
        return fallbackGenerator.generate(ctx);
    }

    private void extractZip(InputStream inputStream, Path destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            long totalSize = 0;
            int entryCount = 0;

            while ((entry = zis.getNextEntry()) != null) {
                if (entryCount++ > 10_000) throw new IOException("Too many entries in ZIP");

                Path resolved = destDir.resolve(entry.getName()).normalize();
                if (!resolved.startsWith(destDir)) {
                    throw new IOException("ZIP entry outside target directory (zip slip)");
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(resolved);
                } else {
                    Files.createDirectories(resolved.getParent());
                    try (OutputStream os = Files.newOutputStream(resolved)) {
                        byte[] buf = new byte[8192];
                        int len;
                        while ((len = zis.read(buf)) > 0) {
                            totalSize += len;
                            if (totalSize > MAX_UPLOAD_BYTES) throw new IOException("Extracted content too large");
                            os.write(buf, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private Path findProjectRoot(Path dir) throws IOException {
        if (hasProjectFiles(dir)) return dir;

        try (var stream = Files.list(dir)) {
            var subdirs = stream.filter(Files::isDirectory).toList();
            if (subdirs.size() == 1 && hasProjectFiles(subdirs.get(0))) {
                return subdirs.get(0);
            }
        }
        return dir;
    }

    private boolean hasProjectFiles(Path dir) {
        return Files.exists(dir.resolve("package.json"))
                || Files.exists(dir.resolve("pom.xml"))
                || Files.exists(dir.resolve("requirements.txt"))
                || Files.exists(dir.resolve("build.gradle"))
                || Files.exists(dir.resolve("go.mod"))
                || Files.exists(dir.resolve("Dockerfile"))
                || Files.exists(dir.resolve("index.html"));
    }
}
