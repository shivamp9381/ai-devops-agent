package com.devops.agent.service;

import com.devops.agent.model.AnalysisResult;
import com.devops.agent.model.ProjectContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AiService {

    private static final Logger log =
            LoggerFactory.getLogger(AiService.class);

    private final ObjectMapper mapper = new ObjectMapper();

    private final HttpClient httpClient =
            HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();

    @Value("${groq.api.key:}")
    private String groqApiKey;

    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String groqApiUrl;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String groqModel;

    public AnalysisResult analyze(ProjectContext ctx) {

        if (groqApiKey == null || groqApiKey.isBlank()) {
            log.warn("No GROQ API key found");
            return null;
        }

        try {
            String prompt = buildPrompt(ctx);
            String response = callGroq(prompt);
            AnalysisResult result = parseResponse(response, ctx);

            if (result != null) {
                log.info("AI analysis successful");
            }

            return result;

        } catch (Exception e) {
            log.error("AI request failed: {}", e.getMessage());
            return null;
        }
    }

    private String buildPrompt(ProjectContext ctx) {

        String files = ctx.getFileNames()
                .stream()
                .limit(80)
                .collect(Collectors.joining("\n"));

        return """
You are a senior DevOps engineer.

Analyze this repository carefully.

Generate production-ready DevOps configuration.

Return ONLY valid JSON.

Rules:
1. deploySteps must NEVER be empty
2. Give real deployment commands
3. Generate production Dockerfile
4. Generate CI/CD YAML
5. Generate environment variables
6. Use real values based on repo structure

Project Type: %s
Port: %s

Files:
%s

Return JSON:

{
  "stack":"",
  "dockerfile":"",
  "compose":"",
  "env":"",
  "githubActions":"",
  "deploySteps":"",
  "recommendations":[]
}
""".formatted(
                ctx.getProjectType(),
                ctx.getDetectedPort(),
                files
        );
    }

    private String callGroq(String prompt) throws Exception {

        Map<String, Object> body = Map.of(
                "model", groqModel,
                "messages", List.of(
                        Map.of(
                                "role", "system",
                                "content",
                                "Return ONLY raw JSON object. No markdown. No explanation."
                        ),
                        Map.of(
                                "role", "user",
                                "content", prompt
                        )
                ),
                "temperature", 0.2,
                "max_tokens", 4096
        );

        String json = mapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(groqApiUrl))
                .header("Authorization", "Bearer " + groqApiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response =
                httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        JsonNode root = mapper.readTree(response.body());

        return root.path("choices")
                .path(0)
                .path("message")
                .path("content")
                .asText();
    }

    private AnalysisResult parseResponse(
            String response,
            ProjectContext ctx
    ) {

        try {

            String cleaned = response.trim();

            cleaned = cleaned.replace("```json", "");
            cleaned = cleaned.replace("```", "");

            int first = cleaned.indexOf("{");
            int last = cleaned.lastIndexOf("}");

            if (first == -1 || last == -1 || last <= first) {
                log.error("No JSON found in AI response");
                return null;
            }

            cleaned = cleaned.substring(first, last + 1);

            log.info("AI RAW JSON:\n{}", cleaned);

            JsonNode json = mapper.readTree(cleaned);

            AnalysisResult result = new AnalysisResult();

            result.setStack(
                    json.path("stack")
                            .asText(ctx.getProjectType())
            );

            result.setDockerfile(
                    formatNode(json.path("dockerfile"))
            );

            result.setCompose(
                    formatNode(json.path("compose"))
            );

            result.setEnv(
                    formatNode(json.path("env"))
            );

            result.setGithubActions(
                    formatNode(json.path("githubActions"))
            );

            result.setDeploySteps(
                    formatNode(json.path("deploySteps"))
            );

            List<String> recs = new ArrayList<>();

            JsonNode arr = json.path("recommendations");

            if (arr.isArray()) {
                for (JsonNode n : arr) {
                    recs.add(n.asText());
                }
            }

            result.setRecommendations(recs);

            return result;

        } catch (Exception e) {
            log.error("AI parse failed: {}", e.getMessage());
            return null;
        }
    }

    private String formatNode(JsonNode node) {

        try {

            if (node == null || node.isMissingNode()) {
                return "";
            }

            if (node.isTextual()) {
                return node.asText();
            }

            if (node.isArray()) {

                StringBuilder sb = new StringBuilder();

                int i = 1;

                for (JsonNode item : node) {
                    sb.append(i++)
                            .append(". ")
                            .append(item.asText())
                            .append("\n");
                }

                return sb.toString();
            }

            if (node.isObject()) {

                StringBuilder sb = new StringBuilder();

                node.fields().forEachRemaining(entry ->
                        sb.append(entry.getKey())
                                .append("=")
                                .append(entry.getValue().asText())
                                .append("\n")
                );

                return sb.toString();
            }

            return node.toString();

        } catch (Exception e) {
            return "";
        }
    }

    public String generateReadme(ProjectContext ctx) {

        return """
# %s

## Overview
Generated by DevOpsPilot AI.

## Stack
%s

## Setup
docker-compose up --build

## Deployment
Use Docker / Render / Railway / VPS.

## API
Generated automatically.
""".formatted(
                ctx.getProjectType(),
                ctx.getProjectType()
        );
    }

    // -------------------------------------------------------------------------------------

//    public String debugError(String error) {
//
//        String txt = error.toLowerCase();
//
//        if (txt.contains("port already in use")) {
//            return """
//Problem: Port already in use
//
//Fix:
//lsof -i :8080
//kill -9 PID
//""";
//        }
//
//        if (txt.contains("maven")) {
//            return "Run: mvn clean install";
//        }
//
//        if (txt.contains("docker")) {
//            return "Try: docker system prune -a";
//        }
//
//        return "Check logs and rebuild container.";
//    }

    public String debugError(String error) {

        if (groqApiKey == null || groqApiKey.isBlank()) {
            return localDebug(error);
        }

        try {

            String prompt = """
You are an expert DevOps debugger.

Analyze the deployment/build/runtime error below.

Return concise response in this format:

Problem:
Cause:
Fix:
Commands:
Prevention:

Error:
%s
""".formatted(error);

            Map<String, Object> body = Map.of(
                    "model", groqModel,
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                    "content",
                                    "You are a senior DevOps engineer."
                            ),
                            Map.of(
                                    "role", "user",
                                    "content", prompt
                            )
                    ),
                    "temperature", 0.2,
                    "max_tokens", 1000
            );

            String json = mapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(groqApiUrl))
                    .header("Authorization", "Bearer " + groqApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(Duration.ofSeconds(45))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            JsonNode root = mapper.readTree(response.body());

            return root.path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText();

        } catch (Exception e) {
            return localDebug(error);
        }
    }

    private String localDebug(String error) {

        String txt = error.toLowerCase();

        if (txt.contains("port")) {
            return """
Problem: Port conflict

Fix:
netstat -ano | findstr :8080
taskkill /PID <PID> /F
""";
        }

        if (txt.contains("maven")) {
            return """
Problem: Maven build issue

Fix:
mvn clean install
""";
        }

        if (txt.contains("docker")) {
            return """
Problem: Docker issue

Fix:
docker system prune -a
docker compose up --build
""";
        }

        return """
Problem: Unknown issue

Fix:
Check logs and rebuild.
""";
    }

    // ------------------------------------------------------------------------------------
}