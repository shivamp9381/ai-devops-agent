package com.devops.agent.controller;

import com.devops.agent.model.ProjectContext;
import com.devops.agent.model.RepoRequest;
import com.devops.agent.service.AiService;
import com.devops.agent.service.GitService;
import com.devops.agent.util.ProjectScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@RestController
@RequestMapping("/api/diagram")
public class ArchitectureController {

    private static final Logger log =
            LoggerFactory.getLogger(ArchitectureController.class);

    private final AiService aiService;
    private final GitService gitService;

    public ArchitectureController(
            AiService aiService,
            GitService gitService
    ) {
        this.aiService = aiService;
        this.gitService = gitService;
    }

    /* ── GitHub URL ─────────────────────────────────────── */

    @PostMapping("/repo")
    public ResponseEntity<?> diagramRepo(
            @RequestBody RepoRequest request
    ) {
        if (request.getRepoUrl() == null || request.getRepoUrl().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Repository URL is required"));
        }

        Path dir = null;

        try {
            log.info("[DIAGRAM] Cloning repo: {}", request.getRepoUrl());
            dir = gitService.cloneRepo(request.getRepoUrl());

            ProjectContext ctx = ProjectScanner.scan(dir);

            String mermaid = aiService.generateArchitectureDiagram(ctx);

            return ResponseEntity.ok(Map.of(
                    "mermaid", mermaid,
                    "projectType", ctx.getProjectType(),
                    "services", ctx.getDetectedServices() != null
                            ? ctx.getDetectedServices()
                            : java.util.List.of()
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("[DIAGRAM] Failed: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Diagram generation failed: " + e.getMessage()));
        } finally {
            gitService.cleanup(dir);
        }
    }

    /* ── ZIP Upload ─────────────────────────────────────── */

    @PostMapping("/upload")
    public ResponseEntity<?> diagramUpload(
            @RequestParam("file") MultipartFile file
    ) {
        Path temp = null;

        try {
            log.info("[DIAGRAM] Processing upload: {}", file.getOriginalFilename());
            temp = Files.createTempDirectory("diagram-upload-");

            unzip(file.getInputStream(), temp);

            ProjectContext ctx = ProjectScanner.scan(temp);

            String mermaid = aiService.generateArchitectureDiagram(ctx);

            return ResponseEntity.ok(Map.of(
                    "mermaid", mermaid,
                    "projectType", ctx.getProjectType(),
                    "services", ctx.getDetectedServices() != null
                            ? ctx.getDetectedServices()
                            : java.util.List.of()
            ));

        } catch (Exception e) {
            log.error("[DIAGRAM] Upload failed: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Diagram generation failed: " + e.getMessage()));
        } finally {
            gitService.cleanup(temp);
        }
    }

    /* ── Unzip helper ───────────────────────────────────── */

    private void unzip(InputStream is, Path target) throws Exception {

        try (ZipInputStream zis = new ZipInputStream(is)) {

            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {

                Path newPath = target.resolve(entry.getName()).normalize();

                if (!newPath.startsWith(target)) {
                    zis.closeEntry();
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