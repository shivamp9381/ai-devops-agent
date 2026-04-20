package com.devops.agent.controller;

import com.devops.agent.model.AnalysisResult;
import com.devops.agent.model.RepoRequest;
import com.devops.agent.service.AnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/analyze")
public class AnalyzeController {

    private final AnalysisService analysisService;

    public AnalyzeController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping("/repo")
    public ResponseEntity<?> analyzeRepo(@RequestBody RepoRequest request) {
        try {
            if (request.getRepoUrl() == null || request.getRepoUrl().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Repository URL is required"));
            }
            AnalysisResult result = analysisService.analyzeRepo(request.getRepoUrl());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Analysis failed: " + e.getMessage()));
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> analyzeUpload(@RequestParam("file") MultipartFile file) {
        try {
            AnalysisResult result = analysisService.analyzeUpload(file);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Analysis failed: " + e.getMessage()));
        }
    }
}
