package com.devops.agent.service;

import org.eclipse.jgit.api.Git;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

@Service
public class GitService {

    private static final Logger log = LoggerFactory.getLogger(GitService.class);

    public Path cloneRepo(String repoUrl) throws Exception {
        String sanitized = sanitizeUrl(repoUrl);
        Path tempDir = Files.createTempDirectory("devops-agent-");

        log.info("Cloning {} into {}", sanitized, tempDir);

        Git.cloneRepository()
                .setURI(sanitized)
                .setDirectory(tempDir.toFile())
                .setDepth(1)
                .setTimeout(120)
                .call()
                .close();

        log.info("Clone complete: {}", tempDir);
        return tempDir;
    }

    public void cleanup(Path dir) {
        if (dir == null) return;

        try {
            Files.walk(dir)
                    .sorted((a, b) -> b.compareTo(a)) // delete children first
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            log.warn("Failed deleting {}", path);
                        }
                    });
        } catch (IOException e) {
            log.warn("Cleanup failed: {}", e.getMessage());
        }
    }

    private String sanitizeUrl(String url) {
        if (url == null || url.isBlank()) throw new IllegalArgumentException("Repository URL is required");
        url = url.strip();
        if (!url.startsWith("https://github.com/")) {
            throw new IllegalArgumentException("Only public GitHub HTTPS URLs are supported");
        }
        if (url.contains("..") || url.contains(" ") || url.contains(";") || url.contains("&")) {
            throw new IllegalArgumentException("Invalid repository URL");
        }
        if (!url.endsWith(".git")) url += ".git";
        return url;
    }
}
