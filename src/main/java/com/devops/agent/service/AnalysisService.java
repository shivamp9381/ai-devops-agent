package com.devops.agent.service;

import com.devops.agent.model.AnalysisResult;
import com.devops.agent.model.ProjectContext;
import com.devops.agent.util.ProjectScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class AnalysisService {

    private static final Logger log =
            LoggerFactory.getLogger(AnalysisService.class);

    private final GitService gitService;
    private final AiService aiService;
    private final FallbackGenerator fallbackGenerator;

    public AnalysisService(
            GitService gitService,
            AiService aiService,
            FallbackGenerator fallbackGenerator
    ) {
        this.gitService = gitService;
        this.aiService = aiService;
        this.fallbackGenerator = fallbackGenerator;
    }

    // ----------------------------------------
    // Analyze GitHub Repo
    // ----------------------------------------
    public AnalysisResult analyzeRepo(String repoUrl) {

        Path dir = null;

        try {
            log.info("Starting repo analysis: {}", repoUrl);

            dir = gitService.cloneRepo(repoUrl);

            ProjectContext ctx = ProjectScanner.scan(dir);

            return runAnalysis(ctx);

        } catch (Exception e) {
            log.error("Repo analysis failed", e);
            throw new RuntimeException("Repo analysis failed");
        } finally {
            gitService.cleanup(dir);
        }
    }

    // ----------------------------------------
    // Analyze ZIP Upload
    // ----------------------------------------
    public AnalysisResult analyzeUpload(MultipartFile file) {

        Path temp = null;

        try {
            temp = Files.createTempDirectory("upload-");

            unzip(file.getInputStream(), temp);

            ProjectContext ctx = ProjectScanner.scan(temp);

            return runAnalysis(ctx);

        } catch (Exception e) {
            log.error("Upload analysis failed", e);
            throw new RuntimeException("Upload analysis failed");
        } finally {
            gitService.cleanup(temp);
        }
    }

    // ----------------------------------------
    // MAIN ANALYSIS FLOW
    // ----------------------------------------
    private AnalysisResult runAnalysis(ProjectContext ctx) {

        AnalysisResult result = aiService.analyze(ctx);

        if (result != null) {
            log.info("AI analysis successful");
        } else {
            log.warn("AI failed. Using fallback generator.");
            result = fallbackGenerator.generate(ctx);
        }

        result.setReadme(aiService.generateReadme(ctx));

        SecurityScanner.scan(ctx, result);

        return result;
    }

    // ----------------------------------------
    // unzip
    // ----------------------------------------
    private void unzip(InputStream is, Path target) throws Exception {

        try (ZipInputStream zis = new ZipInputStream(is)) {

            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {

                Path newPath = target.resolve(entry.getName()).normalize();

                if (!newPath.startsWith(target)) {
                    continue;
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(newPath);
                } else {
                    Files.createDirectories(newPath.getParent());
                    Files.copy(zis, newPath);
                }

                zis.closeEntry();
            }
        }
    }
}