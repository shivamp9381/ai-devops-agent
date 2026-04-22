////package com.devops.agent.service;
////
////import com.devops.agent.model.AnalysisResult;
////import com.devops.agent.model.ProjectContext;
////import com.fasterxml.jackson.databind.JsonNode;
////import com.fasterxml.jackson.databind.ObjectMapper;
////import org.slf4j.Logger;
////import org.slf4j.LoggerFactory;
////import org.springframework.beans.factory.annotation.Value;
////import org.springframework.stereotype.Service;
////
////import java.net.URI;
////import java.net.http.HttpClient;
////import java.net.http.HttpRequest;
////import java.net.http.HttpResponse;
////import java.time.Duration;
////import java.util.ArrayList;
////import java.util.List;
////import java.util.Map;
////import java.util.stream.Collectors;
////
////@Service
////public class AiService {
////
////    private static final Logger log =
////            LoggerFactory.getLogger(AiService.class);
////
////    private final ObjectMapper mapper = new ObjectMapper();
////
////    private final HttpClient httpClient =
////            HttpClient.newBuilder()
////                    .connectTimeout(Duration.ofSeconds(15))
////                    .build();
////
////    @Value("${groq.api.key:}")
////    private String groqApiKey;
////
////    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
////    private String groqApiUrl;
////
////    @Value("${groq.model:llama-3.3-70b-versatile}")
////    private String groqModel;
////
////    public AnalysisResult analyze(ProjectContext ctx) {
////
////        if (groqApiKey == null || groqApiKey.isBlank()) {
////            log.warn("No GROQ API key found");
////            return null;
////        }
////
////        try {
////            String prompt = buildPrompt(ctx);
////            String response = callGroq(prompt);
////            AnalysisResult result = parseResponse(response, ctx);
////
////            if (result != null) {
////                log.info("AI analysis successful");
////            }
////
////            return result;
////
////        } catch (Exception e) {
////            log.error("AI request failed: {}", e.getMessage());
////            return null;
////        }
////    }
////
////    private String buildPrompt(ProjectContext ctx) {
////
////        String files = ctx.getFileNames()
////                .stream()
////                .limit(80)
////                .collect(Collectors.joining("\n"));
////
////        return """
////You are a senior DevOps engineer.
////
////Analyze this repository carefully.
////
////Generate production-ready DevOps configuration.
////
////Return ONLY valid JSON.
////
////Rules:
////1. deploySteps must NEVER be empty
////2. Give real deployment commands
////3. Generate production Dockerfile
////4. Generate CI/CD YAML
////5. Generate environment variables
////6. Use real values based on repo structure
////
////Project Type: %s
////Port: %s
////
////Files:
////%s
////
////Return JSON:
////
////{
////  "stack":"",
////  "dockerfile":"",
////  "compose":"",
////  "env":"",
////  "githubActions":"",
////  "deploySteps":"",
////  "recommendations":[]
////}
////""".formatted(
////                ctx.getProjectType(),
////                ctx.getDetectedPort(),
////                files
////        );
////    }
////
////    private String callGroq(String prompt) throws Exception {
////
////        Map<String, Object> body = Map.of(
////                "model", groqModel,
////                "messages", List.of(
////                        Map.of(
////                                "role", "system",
////                                "content",
////                                "Return ONLY raw JSON object. No markdown. No explanation."
////                        ),
////                        Map.of(
////                                "role", "user",
////                                "content", prompt
////                        )
////                ),
////                "temperature", 0.2,
////                "max_tokens", 4096
////        );
////
////        String json = mapper.writeValueAsString(body);
////
////        HttpRequest request = HttpRequest.newBuilder()
////                .uri(URI.create(groqApiUrl))
////                .header("Authorization", "Bearer " + groqApiKey)
////                .header("Content-Type", "application/json")
////                .timeout(Duration.ofSeconds(60))
////                .POST(HttpRequest.BodyPublishers.ofString(json))
////                .build();
////
////        HttpResponse<String> response =
////                httpClient.send(
////                        request,
////                        HttpResponse.BodyHandlers.ofString()
////                );
////
////        JsonNode root = mapper.readTree(response.body());
////
////        return root.path("choices")
////                .path(0)
////                .path("message")
////                .path("content")
////                .asText();
////    }
////
////    private AnalysisResult parseResponse(
////            String response,
////            ProjectContext ctx
////    ) {
////
////        try {
////
////            String cleaned = response.trim();
////
////            cleaned = cleaned.replace("```json", "");
////            cleaned = cleaned.replace("```", "");
////
////            int first = cleaned.indexOf("{");
////            int last = cleaned.lastIndexOf("}");
////
////            if (first == -1 || last == -1 || last <= first) {
////                log.error("No JSON found in AI response");
////                return null;
////            }
////
////            cleaned = cleaned.substring(first, last + 1);
////
////            log.info("AI RAW JSON:\n{}", cleaned);
////
////            JsonNode json = mapper.readTree(cleaned);
////
////            AnalysisResult result = new AnalysisResult();
////
////            result.setStack(
////                    json.path("stack")
////                            .asText(ctx.getProjectType())
////            );
////
////            result.setDockerfile(
////                    formatNode(json.path("dockerfile"))
////            );
////
////            result.setCompose(
////                    formatNode(json.path("compose"))
////            );
////
////            result.setEnv(
////                    formatNode(json.path("env"))
////            );
////
////            result.setGithubActions(
////                    formatNode(json.path("githubActions"))
////            );
////
////            result.setDeploySteps(
////                    formatNode(json.path("deploySteps"))
////            );
////
////            List<String> recs = new ArrayList<>();
////
////            JsonNode arr = json.path("recommendations");
////
////            if (arr.isArray()) {
////                for (JsonNode n : arr) {
////                    recs.add(n.asText());
////                }
////            }
////
////            result.setRecommendations(recs);
////
////            return result;
////
////        } catch (Exception e) {
////            log.error("AI parse failed: {}", e.getMessage());
////            return null;
////        }
////    }
////
////    private String formatNode(JsonNode node) {
////
////        try {
////
////            if (node == null || node.isMissingNode()) {
////                return "";
////            }
////
////            if (node.isTextual()) {
////                return node.asText();
////            }
////
////            if (node.isArray()) {
////
////                StringBuilder sb = new StringBuilder();
////
////                int i = 1;
////
////                for (JsonNode item : node) {
////                    sb.append(i++)
////                            .append(". ")
////                            .append(item.asText())
////                            .append("\n");
////                }
////
////                return sb.toString();
////            }
////
////            if (node.isObject()) {
////
////                StringBuilder sb = new StringBuilder();
////
////                node.fields().forEachRemaining(entry ->
////                        sb.append(entry.getKey())
////                                .append("=")
////                                .append(entry.getValue().asText())
////                                .append("\n")
////                );
////
////                return sb.toString();
////            }
////
////            return node.toString();
////
////        } catch (Exception e) {
////            return "";
////        }
////    }
////
////    public String generateReadme(ProjectContext ctx) {
////
////        return """
////# %s
////
////## Overview
////Generated by DevOpsPilot AI.
////
////## Stack
////%s
////
////## Setup
////docker-compose up --build
////
////## Deployment
////Use Docker / Render / Railway / VPS.
////
////## API
////Generated automatically.
////""".formatted(
////                ctx.getProjectType(),
////                ctx.getProjectType()
////        );
////    }
////
////    public String debugError(String error) {
////
////        if (groqApiKey == null || groqApiKey.isBlank()) {
////            return localDebug(error);
////        }
////
////        try {
////
////            String prompt = """
////You are an expert DevOps debugger.
////
////Analyze the deployment/build/runtime error below.
////
////Return concise response in this format:
////
////Problem:
////Cause:
////Fix:
////Commands:
////Prevention:
////
////Error:
////%s
////""".formatted(error);
////
////            Map<String, Object> body = Map.of(
////                    "model", groqModel,
////                    "messages", List.of(
////                            Map.of(
////                                    "role", "system",
////                                    "content",
////                                    "You are a senior DevOps engineer."
////                            ),
////                            Map.of(
////                                    "role", "user",
////                                    "content", prompt
////                            )
////                    ),
////                    "temperature", 0.2,
////                    "max_tokens", 1000
////            );
////
////            String json = mapper.writeValueAsString(body);
////
////            HttpRequest request = HttpRequest.newBuilder()
////                    .uri(URI.create(groqApiUrl))
////                    .header("Authorization", "Bearer " + groqApiKey)
////                    .header("Content-Type", "application/json")
////                    .POST(HttpRequest.BodyPublishers.ofString(json))
////                    .timeout(Duration.ofSeconds(45))
////                    .build();
////
////            HttpResponse<String> response =
////                    httpClient.send(
////                            request,
////                            HttpResponse.BodyHandlers.ofString()
////                    );
////
////            JsonNode root = mapper.readTree(response.body());
////
////            return root.path("choices")
////                    .path(0)
////                    .path("message")
////                    .path("content")
////                    .asText();
////
////        } catch (Exception e) {
////            return localDebug(error);
////        }
////    }
////
////    private String localDebug(String error) {
////
////        String txt = error.toLowerCase();
////
////        if (txt.contains("port")) {
////            return """
////Problem: Port conflict
////
////Fix:
////netstat -ano | findstr :8080
////taskkill /PID <PID> /F
////""";
////        }
////
////        if (txt.contains("maven")) {
////            return """
////Problem: Maven build issue
////
////Fix:
////mvn clean install
////""";
////        }
////
////        if (txt.contains("docker")) {
////            return """
////Problem: Docker issue
////
////Fix:
////docker system prune -a
////docker compose up --build
////""";
////        }
////
////        return """
////Problem: Unknown issue
////
////Fix:
////Check logs and rebuild.
////""";
////    }
////
////    // ------------------------------------------------------------------------------------
////    public String explainRepo(ProjectContext ctx) {
////
////        if (groqApiKey == null || groqApiKey.isBlank()) {
////            return fallbackExplain(ctx);
////        }
////
////        try {
////
////            String files = String.join("\n", ctx.getFileNames());
////
////            String prompt = """
////You are a senior software architect.
////
////Analyze this code repository deeply.
////
////Generate professional explanation in clean markdown format.
////
////Include:
////
////1. Project Overview
////2. Main Purpose
////3. Technologies Used
////4. Folder / File Structure
////5. Core Working Flow
////6. Important Components
////7. Backend / Frontend Logic
////8. Deployment Process
////9. Security Notes
////10. Suggestions to Improve
////
////Repository Type: %s
////
////Files:
////%s
////""".formatted(ctx.getProjectType(), files);
////
////            Map<String, Object> body = Map.of(
////                    "model", groqModel,
////                    "messages", List.of(
////                            Map.of("role", "user", "content", prompt)
////                    ),
////                    "temperature", 0.4,
////                    "max_tokens", 3000
////            );
////
////            String json = mapper.writeValueAsString(body);
////
////            HttpRequest request = HttpRequest.newBuilder()
////                    .uri(URI.create(groqApiUrl))
////                    .header("Authorization", "Bearer " + groqApiKey)
////                    .header("Content-Type", "application/json")
////                    .POST(HttpRequest.BodyPublishers.ofString(json))
////                    .timeout(Duration.ofSeconds(60))
////                    .build();
////
////            HttpResponse<String> response =
////                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
////
////            JsonNode root = mapper.readTree(response.body());
////
////            return root.path("choices")
////                    .path(0)
////                    .path("message")
////                    .path("content")
////                    .asText();
////
////        } catch (Exception e) {
////            return fallbackExplain(ctx);
////        }
////    }
////
////    private String fallbackExplain(ProjectContext ctx) {
////
////        return """
////# Project Overview
////
////This repository appears to be a %s application.
////
////## Main Purpose
////Handles core business logic and application workflows.
////
////## Technologies
////- %s
////
////## Files Found
////%s
////
////## Deployment
////Docker + CI/CD possible.
////
////## Suggestions
////- Add tests
////- Improve logging
////- Add monitoring
////- Improve security
////""".formatted(
////                ctx.getProjectType(),
////                ctx.getProjectType(),
////                String.join(", ", ctx.getFileNames())
////        );
////    }
////
////    //-------------------------------------------------------------------------------------
////}
//
//
//
//
//package com.devops.agent.service;
//
//import com.devops.agent.model.AnalysisResult;
//import com.devops.agent.model.ProjectContext;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import java.net.URI;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//import java.time.Duration;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//import java.util.stream.Collectors;
//
//@Service
//public class AiService {
//
//    private static final Logger log =
//            LoggerFactory.getLogger(AiService.class);
//
//    private final ObjectMapper mapper =
//            new ObjectMapper();
//
//    private final HttpClient httpClient =
//            HttpClient.newBuilder()
//                    .connectTimeout(Duration.ofSeconds(20))
//                    .build();
//
//    @Value("${groq.api.key:}")
//    private String groqApiKey;
//
//    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
//    private String groqApiUrl;
//
//    @Value("${groq.model:llama-3.3-70b-versatile}")
//    private String groqModel;
//
//    // Max seconds to wait on a TPM rate limit before giving up
//    private static final int MAX_RETRY_WAIT_SECONDS = 15;
//
//    /* ========================================================
//       MAIN ANALYSIS
//       ======================================================== */
//
//    public AnalysisResult analyze(ProjectContext ctx) {
//
//        if (groqApiKey == null || groqApiKey.isBlank()) {
//            log.warn("[AI] No GROQ_API_KEY set -- skipping AI analysis");
//            return null;
//        }
//
//        try {
//            log.info("[AI] Sending analysis prompt to Groq...");
//            String prompt = buildAnalysisPrompt(ctx);
//            String raw = callGroqWithRetry(prompt, 8000);
//
//            log.info("[AI] Raw response length: {} chars", raw.length());
//
//            AnalysisResult result = parseAnalysisResponse(raw, ctx);
//
//            if (result != null) {
//                log.info("[AI] Main analysis --> GENERATED BY AI (Groq)");
//            }
//
//            return result;
//
//        } catch (Exception e) {
//            log.error("[AI] Analysis failed: {}", e.getMessage());
//            return null;
//        }
//    }
//
//    /* ========================================================
//       REPO EXPLAIN
//       ======================================================== */
//
//    public String explainRepo(ProjectContext ctx) {
//        return explainRepo(ctx, false);
//    }
//
//    public String explainRepo(
//            ProjectContext ctx,
//            boolean aiAlreadyWorked
//    ) {
//
//        if (groqApiKey == null || groqApiKey.isBlank()) {
//            log.warn("[AI] No GROQ_API_KEY set -- using fallback explanation");
//            return fallbackExplain(ctx);
//        }
//
//        try {
//            log.info("[AI] Sending explain prompt to Groq...");
//            String prompt = buildExplainPrompt(ctx);
//
//            // Use retry — the explain call often hits TPM right after the
//            // main analysis call. A short wait is all that's needed.
//            String output = callGroqWithRetry(prompt, 3000);
//
//            if (output == null || output.isBlank()) {
//                log.warn("[AI] explainRepo returned blank -- using fallback");
//                return fallbackExplain(ctx);
//            }
//
//            log.info("[AI] Repo explanation --> GENERATED BY AI (Groq) ({} chars)", output.length());
//            return output;
//
//        } catch (Exception e) {
//            log.warn("[AI] explainRepo failed: {} -- using fallback", e.getMessage());
//            return fallbackExplain(ctx);
//        }
//    }
//
//    /* ========================================================
//       DEBUG ERROR
//       ======================================================== */
//
//    public String debugError(String error) {
//
//        if (groqApiKey == null || groqApiKey.isBlank()) {
//            return "Problem:\nUnknown issue\n\nFix:\nCheck logs and rebuild.";
//        }
//
//        try {
//            String prompt =
//                    "You are a senior DevOps debugger.\n\n"
//                            + "Analyze this error:\n\n"
//                            + error
//                            + "\n\nReturn in this format:\n\n"
//                            + "Problem:\nCause:\nFix:\nCommands:\nPrevention:";
//
//            return callGroqWithRetry(prompt, 1200);
//
//        } catch (Exception e) {
//            return "Problem:\nUnknown issue\n\nFix:\nCheck logs and rebuild.";
//        }
//    }
//
//    /* ========================================================
//       README
//       ======================================================== */
//
//    public String generateReadme(ProjectContext ctx) {
//        return "# " + ctx.getProjectType() + "\n\n"
//                + "## Overview\nGenerated by DevOpsPilot AI\n\n"
//                + "## Stack\n" + ctx.getProjectType() + "\n\n"
//                + "## Run Locally\n\n```bash\ndocker-compose up --build\n```\n\n"
//                + "## Deployment\nUse Docker / Railway / Render / VPS\n\n"
//                + "## Notes\nGenerated automatically from repository analysis.\n";
//    }
//
//    /* ========================================================
//       PROMPTS
//       ======================================================== */
//
//    private String buildAnalysisPrompt(ProjectContext ctx) {
//
//        String files = ctx.getFileNames()
//                .stream()
//                .limit(120)
//                .collect(Collectors.joining("\n"));
//
//        return "You are a world-class DevOps architect.\n\n"
//                + "Analyze this repository carefully.\n\n"
//                + "Return ONLY valid JSON. No markdown. No explanation.\n"
//                + "Fill EVERY field. Do NOT leave any blank.\n\n"
//                + "Output keys in exactly this order:\n"
//                + "{\n"
//                + "  \"stack\": \"\",\n"
//                + "  \"deploySteps\": \"\",\n"
//                + "  \"env\": \"\",\n"
//                + "  \"recommendations\": [],\n"
//                + "  \"compose\": \"\",\n"
//                + "  \"dockerfile\": \"\",\n"
//                + "  \"githubActions\": \"\"\n"
//                + "}\n\n"
//                + "Rules:\n"
//                + "1. stack       - detected technology stack\n"
//                + "2. deploySteps - real shell commands: git clone, build, docker run\n"
//                + "3. env         - .env.example content matching the stack\n"
//                + "4. recommendations - array of 3-5 practical tips\n"
//                + "5. compose     - complete runnable docker-compose.yml\n"
//                + "6. dockerfile  - realistic multi-stage Dockerfile\n"
//                + "7. githubActions - production CI/CD workflow yaml\n\n"
//                + "Project Type: " + ctx.getProjectType() + "\n"
//                + "Port: " + ctx.getDetectedPort() + "\n\n"
//                + "Files:\n" + files;
//    }
//
//    private String buildExplainPrompt(ProjectContext ctx) {
//
//        String files = ctx.getFileNames()
//                .stream()
//                .limit(150)
//                .collect(Collectors.joining("\n"));
//
//        return "You are a senior software architect.\n\n"
//                + "Analyze this repository deeply.\n\n"
//                + "Write a premium markdown explanation covering:\n"
//                + "- Project Overview\n"
//                + "- Business Purpose\n"
//                + "- Technologies Used\n"
//                + "- Key Modules\n"
//                + "- Folder Structure\n"
//                + "- How Request Flows\n"
//                + "- Security Layer\n"
//                + "- Deployment Design\n"
//                + "- Improvements\n"
//                + "- Scalability Suggestions\n\n"
//                + "Project Type: " + ctx.getProjectType() + "\n\n"
//                + "Files:\n" + files;
//    }
//
//    /* ========================================================
//       GROQ CALL WITH RETRY
//       Automatically waits and retries once on TPM rate limits.
//       TPD (daily) limits are not retried — they need hours.
//       ======================================================== */
//
//    private String callGroqWithRetry(String prompt, int maxTokens) throws Exception {
//
//        try {
//            return callGroq(prompt, maxTokens);
//
//        } catch (RateLimitException e) {
//
//            if (e.isTPM() && e.getRetryAfterSeconds() <= MAX_RETRY_WAIT_SECONDS) {
//                // Short TPM window — wait and retry once
//                int waitMs = (int) (e.getRetryAfterSeconds() * 1000) + 500; // +500ms buffer
//                log.warn("[AI] TPM rate limit hit. Waiting {}ms then retrying...", waitMs);
//                Thread.sleep(waitMs);
//                log.info("[AI] Retrying after TPM wait...");
//                return callGroq(prompt, maxTokens);
//
//            } else {
//                // TPD (daily) limit or long wait — not worth retrying
//                throw e;
//            }
//        }
//    }
//
//    /* ========================================================
//       GROQ CALL
//       ======================================================== */
//
//    private String callGroq(String prompt, int maxTokens) throws Exception {
//
//        Map<String, Object> body = Map.of(
//                "model", groqModel,
//                "messages", List.of(
//                        Map.of(
//                                "role", "user",
//                                "content", prompt
//                        )
//                ),
//                "temperature", 0.2,
//                "max_tokens", maxTokens
//        );
//
//        String json = mapper.writeValueAsString(body);
//
//        HttpRequest request = HttpRequest.newBuilder()
//                .uri(URI.create(groqApiUrl))
//                .header("Authorization", "Bearer " + groqApiKey)
//                .header("Content-Type", "application/json")
//                .POST(HttpRequest.BodyPublishers.ofString(json))
//                .timeout(Duration.ofSeconds(60))
//                .build();
//
//        HttpResponse<String> response = httpClient.send(
//                request,
//                HttpResponse.BodyHandlers.ofString()
//        );
//
//        JsonNode root = mapper.readTree(response.body());
//
//        // ── Detect Groq API errors ─────────────────────────────────────
//        if (root.has("error")) {
//            String errorType    = root.path("error").path("type").asText("unknown");
//            String errorMessage = root.path("error").path("message").asText("no message");
//            log.error("[GROQ ERROR] type={} | message={}", errorType, errorMessage);
//
//            // Parse retry-after from message for smart retry logic
//            double retryAfter = parseRetryAfterSeconds(errorMessage);
//            boolean isTPM = errorMessage.contains("tokens per minute");
//
//            throw new RateLimitException(errorType, errorMessage, retryAfter, isTPM);
//        }
//
//        // ── Detect empty choices ───────────────────────────────────────
//        JsonNode choices = root.path("choices");
//        if (!choices.isArray() || choices.isEmpty()) {
//            log.error("[GROQ ERROR] Response has no choices. Full response: {}", response.body());
//            throw new RuntimeException("Groq returned no choices in response");
//        }
//
//        String content = root.path("choices")
//                .path(0)
//                .path("message")
//                .path("content")
//                .asText();
//
//        if (content == null || content.isBlank()) {
//            log.error("[GROQ ERROR] Content field is blank. Full response: {}", response.body());
//            throw new RuntimeException("Groq returned blank content");
//        }
//
//        // ── Log finish reason ─────────────────────────────────────────
//        String finishReason = root.path("choices")
//                .path(0)
//                .path("finish_reason")
//                .asText("unknown");
//
//        if ("length".equals(finishReason)) {
//            log.warn("[GROQ WARN] Response truncated (finish_reason=length).");
//        } else {
//            log.info("[GROQ] finish_reason={}", finishReason);
//        }
//
//        return content;
//    }
//
//    /* ========================================================
//       PARSER
//       ======================================================== */
//
//    private AnalysisResult parseAnalysisResponse(
//            String raw,
//            ProjectContext ctx
//    ) throws Exception {
//
//        raw = raw.replace("```json", "")
//                .replace("```", "")
//                .trim();
//
//        int first = raw.indexOf("{");
//        int last = raw.lastIndexOf("}");
//
//        if (first == -1 || last == -1) {
//            log.error("[PARSER] No JSON braces found. Raw:\n{}", raw);
//            throw new RuntimeException("JSON not found in AI response");
//        }
//
//        raw = raw.substring(first, last + 1);
//
//        JsonNode json = mapper.readTree(raw);
//
//        AnalysisResult result = new AnalysisResult();
//
//        result.setStack(json.path("stack").asText(ctx.getProjectType()));
//        result.setDeploySteps(json.path("deploySteps").asText());
//        result.setEnv(json.path("env").asText());
//        result.setCompose(json.path("compose").asText());
//        result.setDockerfile(json.path("dockerfile").asText());
//        result.setGithubActions(json.path("githubActions").asText());
//
//        List<String> recs = new ArrayList<>();
//        JsonNode arr = json.path("recommendations");
//        if (arr.isArray()) {
//            for (JsonNode item : arr) {
//                recs.add(item.asText());
//            }
//        }
//        result.setRecommendations(recs);
//
//        patchBlanks(result, ctx);
//
//        return result;
//    }
//
//    /* ========================================================
//       BLANK FIELD PATCHER
//       ======================================================== */
//
//    private void patchBlanks(AnalysisResult result, ProjectContext ctx) {
//
//        String type = ctx.getProjectType();
//        String port = ctx.getDetectedPort() != null ? ctx.getDetectedPort() : "8080";
//
//        if (isBlank(result.getDeploySteps())) {
//            log.warn("[PATCH] deploySteps was blank -- filling with fallback");
//            result.setDeploySteps(buildFallbackDeploySteps());
//        }
//
//        if (isBlank(result.getEnv())) {
//            log.warn("[PATCH] env was blank -- filling with fallback");
//            result.setEnv(buildFallbackEnv(type, port));
//        }
//
//        if (isBlank(result.getDockerfile())) {
//            log.warn("[PATCH] dockerfile was blank -- filling with fallback");
//            result.setDockerfile(buildFallbackDockerfile(type, port));
//        }
//
//        if (isBlank(result.getCompose())) {
//            log.warn("[PATCH] compose was blank -- filling with fallback");
//            result.setCompose(buildFallbackCompose(port));
//        }
//
//        if (isBlank(result.getGithubActions())) {
//            log.warn("[PATCH] githubActions was blank -- filling with fallback");
//            result.setGithubActions(buildFallbackGithubActions(type));
//        }
//    }
//
//    private boolean isBlank(String s) {
//        return s == null || s.isBlank();
//    }
//
//    /* ========================================================
//       RETRY HELPER — parse "Please try again in Xs" from message
//       ======================================================== */
//
//    private double parseRetryAfterSeconds(String message) {
//        // Groq message: "Please try again in 3.08s"  or  "in 1h56m19.392s"
//        // We only auto-retry short waits (seconds only, no hours/minutes)
//        try {
//            Pattern p = Pattern.compile("try again in ([0-9.]+)s");
//            Matcher m = p.matcher(message);
//            if (m.find()) {
//                return Double.parseDouble(m.group(1));
//            }
//        } catch (Exception ignored) {}
//        return Double.MAX_VALUE; // unknown wait — don't retry
//    }
//
//    /* ========================================================
//       CUSTOM EXCEPTION FOR RATE LIMITS
//       ======================================================== */
//
//    private static class RateLimitException extends RuntimeException {
//        private final double retryAfterSeconds;
//        private final boolean tpm;
//
//        RateLimitException(String type, String message, double retryAfterSeconds, boolean tpm) {
//            super("Groq API error [" + type + "]: " + message);
//            this.retryAfterSeconds = retryAfterSeconds;
//            this.tpm = tpm;
//        }
//
//        double getRetryAfterSeconds() { return retryAfterSeconds; }
//        boolean isTPM() { return tpm; }
//    }
//
//    /* ========================================================
//       FALLBACK TEMPLATES
//       ======================================================== */
//
//    private String buildFallbackDeploySteps() {
//        return "## Deployment Steps\n\n"
//                + "### 1. Clone and Setup\n"
//                + "```bash\n"
//                + "git clone <your-repo-url>\n"
//                + "cd <project-name>\n"
//                + "cp .env.example .env\n"
//                + "# Edit .env with your values\n"
//                + "```\n\n"
//                + "### 2. Run with Docker Compose\n"
//                + "```bash\n"
//                + "docker-compose up -d --build\n"
//                + "docker-compose logs -f\n"
//                + "```\n\n"
//                + "### 3. Stop\n"
//                + "```bash\n"
//                + "docker-compose down\n"
//                + "```\n\n"
//                + "### 4. Cloud Options\n"
//                + "- Railway / Render / Fly.io -- connect GitHub repo for auto-deploy\n"
//                + "- AWS ECS / GCP Cloud Run / Azure Container Apps\n";
//    }
//
//    private String buildFallbackEnv(String type, String port) {
//        StringBuilder sb = new StringBuilder();
//        sb.append("PORT=").append(port).append("\n");
//        sb.append("APP_ENV=production\n");
//        if (type.contains("Spring Boot") || type.contains("Java")) {
//            sb.append("SPRING_PROFILES_ACTIVE=prod\n");
//            sb.append("SERVER_PORT=").append(port).append("\n");
//        }
//        if (type.contains("Django")) {
//            sb.append("DJANGO_SECRET_KEY=change-me\n");
//            sb.append("DJANGO_DEBUG=false\n");
//        }
//        sb.append("DATABASE_URL=postgresql://user:password@localhost:5432/dbname\n");
//        sb.append("LOG_LEVEL=info\n");
//        return sb.toString();
//    }
//
//    private String buildFallbackDockerfile(String type, String port) {
//        if (type.contains("Spring Boot") || type.contains("Java")) {
//            return "FROM eclipse-temurin:17-jdk-alpine AS build\n"
//                    + "WORKDIR /app\n"
//                    + "COPY pom.xml .\n"
//                    + "COPY src ./src\n"
//                    + "RUN apk add --no-cache maven && mvn clean package -DskipTests\n"
//                    + "FROM eclipse-temurin:17-jre-alpine\n"
//                    + "WORKDIR /app\n"
//                    + "COPY --from=build /app/target/*.jar app.jar\n"
//                    + "EXPOSE " + port + "\n"
//                    + "ENTRYPOINT [\"java\", \"-jar\", \"app.jar\"]\n";
//        }
//        if (type.contains("Node")) {
//            return "FROM node:20-alpine\n"
//                    + "WORKDIR /app\n"
//                    + "COPY package*.json ./\n"
//                    + "RUN npm ci --only=production\n"
//                    + "COPY . .\n"
//                    + "EXPOSE " + port + "\n"
//                    + "CMD [\"node\", \"server.js\"]\n";
//        }
//        return "FROM ubuntu:22.04\n"
//                + "WORKDIR /app\n"
//                + "COPY . .\n"
//                + "EXPOSE " + port + "\n"
//                + "CMD [\"bash\"]\n";
//    }
//
//    private String buildFallbackCompose(String port) {
//        return "version: '3.8'\n"
//                + "services:\n"
//                + "  app:\n"
//                + "    build: .\n"
//                + "    ports:\n"
//                + "      - \"" + port + ":" + port + "\"\n"
//                + "    env_file:\n"
//                + "      - .env\n"
//                + "    restart: unless-stopped\n";
//    }
//
//    private String buildFallbackGithubActions(String type) {
//        return "name: CI/CD Pipeline\n"
//                + "on:\n"
//                + "  push:\n"
//                + "    branches: [ main ]\n"
//                + "jobs:\n"
//                + "  build:\n"
//                + "    runs-on: ubuntu-latest\n"
//                + "    steps:\n"
//                + "      - uses: actions/checkout@v4\n"
//                + "      - name: Build Docker image\n"
//                + "        run: docker build -t app:latest .\n";
//    }
//
//    /* ========================================================
//       FALLBACK EXPLAIN
//       ======================================================== */
//
//    private String fallbackExplain(ProjectContext ctx) {
//        return "## Project Overview\nThis is a "
//                + ctx.getProjectType()
//                + " application.\n\n"
//                + "## Technologies Used\n- "
//                + ctx.getProjectType()
//                + "\n\n"
//                + "## Improvements\n"
//                + "- Add tests\n"
//                + "- Add monitoring\n"
//                + "- Add CI/CD\n"
//                + "- Improve logging\n";
//    }
//
// }


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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AiService {

    private static final Logger log =
            LoggerFactory.getLogger(AiService.class);

    private final ObjectMapper mapper =
            new ObjectMapper();

    private final HttpClient httpClient =
            HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(20))
                    .build();

    @Value("${groq.api.key:}")
    private String groqApiKey;

    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String groqApiUrl;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String groqModel;

    // Max seconds to wait on a TPM rate limit before giving up
    private static final int MAX_RETRY_WAIT_SECONDS = 15;

    /* ========================================================
       MAIN ANALYSIS
       ======================================================== */

    public AnalysisResult analyze(ProjectContext ctx) {

        if (groqApiKey == null || groqApiKey.isBlank()) {
            log.warn("[AI] No GROQ_API_KEY set -- skipping AI analysis");
            return null;
        }

        try {
            log.info("[AI] Sending analysis prompt to Groq...");
            String prompt = buildAnalysisPrompt(ctx);
            String raw = callGroqWithRetry(prompt, 8000);

            log.info("[AI] Raw response length: {} chars", raw.length());

            AnalysisResult result = parseAnalysisResponse(raw, ctx);

            if (result != null) {
                log.info("[AI] Main analysis --> GENERATED BY AI (Groq)");
            }

            return result;

        } catch (Exception e) {
            log.error("[AI] Analysis failed: {}", e.getMessage());
            return null;
        }
    }

    /* ========================================================
       REPO EXPLAIN
       ======================================================== */

    public String explainRepo(ProjectContext ctx) {
        return explainRepo(ctx, false);
    }

    public String explainRepo(
            ProjectContext ctx,
            boolean aiAlreadyWorked
    ) {

        if (groqApiKey == null || groqApiKey.isBlank()) {
            log.warn("[AI] No GROQ_API_KEY set -- using fallback explanation");
            return fallbackExplain(ctx);
        }

        try {
            log.info("[AI] Sending explain prompt to Groq...");
            String prompt = buildExplainPrompt(ctx);
            String output = callGroqWithRetry(prompt, 3000);

            if (output == null || output.isBlank()) {
                log.warn("[AI] explainRepo returned blank -- using fallback");
                return fallbackExplain(ctx);
            }

            log.info("[AI] Repo explanation --> GENERATED BY AI (Groq) ({} chars)", output.length());
            return output;

        } catch (Exception e) {
            log.warn("[AI] explainRepo failed: {} -- using fallback", e.getMessage());
            return fallbackExplain(ctx);
        }
    }

    /* ========================================================
       ARCHITECTURE DIAGRAM
       ======================================================== */

    public String generateArchitectureDiagram(ProjectContext ctx) {

        if (groqApiKey == null || groqApiKey.isBlank()) {
            log.warn("[AI] No GROQ_API_KEY -- using fallback architecture diagram");
            return buildFallbackDiagram(ctx);
        }

        try {
            log.info("[AI] Generating architecture diagram for: {}", ctx.getProjectType());
            String prompt = buildDiagramPrompt(ctx);
            String output = callGroqWithRetry(prompt, 1500);

            if (output == null || output.isBlank()) {
                log.warn("[AI] Diagram generation returned blank -- using fallback");
                return buildFallbackDiagram(ctx);
            }

            // Strip markdown fences if AI wrapped it
            output = output.replace("```mermaid", "").replace("```", "").trim();

            // Validate it starts with a Mermaid graph declaration
            if (!output.startsWith("graph") && !output.startsWith("flowchart")) {
                log.warn("[AI] Diagram response doesn't look like Mermaid -- using fallback");
                log.debug("[AI] Bad diagram response: {}", output);
                return buildFallbackDiagram(ctx);
            }

            log.info("[AI] Architecture diagram --> GENERATED BY AI (Groq) ({} chars)", output.length());
            return output;

        } catch (Exception e) {
            log.warn("[AI] Diagram generation failed: {} -- using fallback", e.getMessage());
            return buildFallbackDiagram(ctx);
        }
    }

    /* ========================================================
       DEBUG ERROR
       ======================================================== */

    public String debugError(String error) {

        if (groqApiKey == null || groqApiKey.isBlank()) {
            return "Problem:\nUnknown issue\n\nFix:\nCheck logs and rebuild.";
        }

        try {
            String prompt =
                    "You are a senior DevOps debugger.\n\n"
                            + "Analyze this error:\n\n"
                            + error
                            + "\n\nReturn in this format:\n\n"
                            + "Problem:\nCause:\nFix:\nCommands:\nPrevention:";

            return callGroqWithRetry(prompt, 1200);

        } catch (Exception e) {
            return "Problem:\nUnknown issue\n\nFix:\nCheck logs and rebuild.";
        }
    }

    /* ========================================================
       README
       ======================================================== */

    public String generateReadme(ProjectContext ctx) {
        return "# " + ctx.getProjectType() + "\n\n"
                + "## Overview\nGenerated by DevOpsPilot AI\n\n"
                + "## Stack\n" + ctx.getProjectType() + "\n\n"
                + "## Run Locally\n\n```bash\ndocker-compose up --build\n```\n\n"
                + "## Deployment\nUse Docker / Railway / Render / VPS\n\n"
                + "## Notes\nGenerated automatically from repository analysis.\n";
    }

    /* ========================================================
       PROMPTS
       ======================================================== */

    private String buildAnalysisPrompt(ProjectContext ctx) {

        String files = ctx.getFileNames()
                .stream()
                .limit(120)
                .collect(Collectors.joining("\n"));

        return "You are a world-class DevOps architect.\n\n"
                + "Analyze this repository carefully.\n\n"
                + "Return ONLY valid JSON. No markdown. No explanation.\n"
                + "Fill EVERY field. Do NOT leave any blank.\n\n"
                + "Output keys in exactly this order:\n"
                + "{\n"
                + "  \"stack\": \"\",\n"
                + "  \"deploySteps\": \"\",\n"
                + "  \"env\": \"\",\n"
                + "  \"recommendations\": [],\n"
                + "  \"compose\": \"\",\n"
                + "  \"dockerfile\": \"\",\n"
                + "  \"githubActions\": \"\"\n"
                + "}\n\n"
                + "Rules:\n"
                + "1. stack       - detected technology stack\n"
                + "2. deploySteps - real shell commands: git clone, build, docker run\n"
                + "3. env         - .env.example content matching the stack\n"
                + "4. recommendations - array of 3-5 practical tips\n"
                + "5. compose     - complete runnable docker-compose.yml\n"
                + "6. dockerfile  - realistic multi-stage Dockerfile\n"
                + "7. githubActions - production CI/CD workflow yaml\n\n"
                + "Project Type: " + ctx.getProjectType() + "\n"
                + "Port: " + ctx.getDetectedPort() + "\n\n"
                + "Files:\n" + files;
    }

    private String buildExplainPrompt(ProjectContext ctx) {

        String files = ctx.getFileNames()
                .stream()
                .limit(150)
                .collect(Collectors.joining("\n"));

        return "You are a senior software architect.\n\n"
                + "Analyze this repository deeply.\n\n"
                + "Write a premium markdown explanation covering:\n"
                + "- Project Overview\n"
                + "- Business Purpose\n"
                + "- Technologies Used\n"
                + "- Key Modules\n"
                + "- Folder Structure\n"
                + "- How Request Flows\n"
                + "- Security Layer\n"
                + "- Deployment Design\n"
                + "- Improvements\n"
                + "- Scalability Suggestions\n\n"
                + "Project Type: " + ctx.getProjectType() + "\n\n"
                + "Files:\n" + files;
    }

    private String buildDiagramPrompt(ProjectContext ctx) {

        String files = ctx.getFileNames()
                .stream()
                .limit(100)
                .collect(Collectors.joining("\n"));

        List<String> services = ctx.getDetectedServices();
        String servicesList = (services != null && !services.isEmpty())
                ? String.join(", ", services)
                : "none detected";

        return "You are a software architect. Generate a Mermaid.js flowchart diagram.\n\n"
                + "STRICT RULES:\n"
                + "1. Output ONLY the Mermaid diagram. No explanation. No markdown fences.\n"
                + "2. Start with: flowchart TD\n"
                + "3. Use clean node IDs like A, B, C or descriptive ones like FRONTEND, API, DB\n"
                + "4. Show the full system architecture: client, backend, database, cache, message queue, external services, CI/CD\n"
                + "5. Use subgraphs to group: Client Layer, Backend Layer, Data Layer, Infrastructure Layer\n"
                + "6. Node labels must be short (max 4 words)\n"
                + "7. Use these arrow styles:\n"
                + "   --> for HTTP/API calls\n"
                + "   ==> for data flow\n"
                + "   -.-> for async/event\n"
                + "8. Max 20 nodes total\n\n"
                + "Project Type: " + ctx.getProjectType() + "\n"
                + "Detected Services: " + servicesList + "\n\n"
                + "Files (use to infer components):\n" + files + "\n\n"
                + "Example of good output:\n"
                + "flowchart TD\n"
                + "  subgraph Client\n"
                + "    A[React Frontend]\n"
                + "  end\n"
                + "  subgraph Backend\n"
                + "    B[Spring Boot API]\n"
                + "    C[WebSocket Server]\n"
                + "  end\n"
                + "  subgraph Data\n"
                + "    D[(MongoDB)]\n"
                + "    E[(Redis Cache)]\n"
                + "  end\n"
                + "  subgraph Infra\n"
                + "    F[Docker]\n"
                + "    G[GitHub Actions]\n"
                + "  end\n"
                + "  A --> B\n"
                + "  A -.-> C\n"
                + "  B ==> D\n"
                + "  B ==> E\n"
                + "  F --> B\n"
                + "  G --> F\n\n"
                + "Now generate the diagram for this project:";
    }

    /* ========================================================
       FALLBACK DIAGRAM
       ======================================================== */

    private String buildFallbackDiagram(ProjectContext ctx) {

        String type = ctx.getProjectType();
        List<String> services = ctx.getDetectedServices();

        StringBuilder sb = new StringBuilder("flowchart TD\n");

        // Client layer
        sb.append("  subgraph Client[\"🖥️ Client Layer\"]\n");
        if (type.contains("React") || type.contains("Node")) {
            sb.append("    A[React Frontend]\n");
        } else {
            sb.append("    A[Web Browser]\n");
        }
        sb.append("  end\n\n");

        // Backend layer
        sb.append("  subgraph Backend[\"⚙️ Backend Layer\"]\n");
        if (type.contains("Spring Boot")) {
            sb.append("    B[Spring Boot API]\n");
            sb.append("    C[REST Controllers]\n");
        } else if (type.contains("Node")) {
            sb.append("    B[Node.js Server]\n");
            sb.append("    C[Express Routes]\n");
        } else if (type.contains("Django")) {
            sb.append("    B[Django API]\n");
            sb.append("    C[Django Views]\n");
        } else {
            sb.append("    B[Backend API]\n");
            sb.append("    C[Business Logic]\n");
        }
        sb.append("  end\n\n");

        // Data layer
        sb.append("  subgraph Data[\"🗄️ Data Layer\"]\n");
        boolean hasDB = false;
        if (services != null) {
            if (services.contains("MongoDB")) {
                sb.append("    D[(MongoDB)]\n"); hasDB = true;
            }
            if (services.contains("PostgreSQL")) {
                sb.append("    D[(PostgreSQL)]\n"); hasDB = true;
            }
            if (services.contains("MySQL")) {
                sb.append("    D[(MySQL)]\n"); hasDB = true;
            }
            if (services.contains("Redis")) {
                sb.append("    E[(Redis Cache)]\n");
            }
        }
        if (!hasDB) {
            sb.append("    D[(Database)]\n");
        }
        sb.append("  end\n\n");

        // Infra layer
        sb.append("  subgraph Infra[\"🚀 Infrastructure\"]\n");
        sb.append("    F[Docker]\n");
        sb.append("    G[GitHub Actions]\n");
        if (services != null && services.contains("Nginx")) {
            sb.append("    H[Nginx]\n");
        }
        sb.append("  end\n\n");

        // Connections
        sb.append("  A --> B\n");
        sb.append("  B --> C\n");
        sb.append("  C ==> D\n");
        if (services != null && services.contains("Redis")) {
            sb.append("  C ==> E\n");
        }
        if (services != null && services.contains("Kafka")) {
            sb.append("  B -.-> K[Kafka]\n");
        }
        sb.append("  G --> F\n");
        sb.append("  F --> B\n");

        return sb.toString();
    }

    /* ========================================================
       GROQ CALL WITH RETRY
       ======================================================== */

    private String callGroqWithRetry(String prompt, int maxTokens) throws Exception {

        try {
            return callGroq(prompt, maxTokens);

        } catch (RateLimitException e) {

            if (e.isTPM() && e.getRetryAfterSeconds() <= MAX_RETRY_WAIT_SECONDS) {
                int waitMs = (int) (e.getRetryAfterSeconds() * 1000) + 500;
                log.warn("[AI] TPM rate limit hit. Waiting {}ms then retrying...", waitMs);
                Thread.sleep(waitMs);
                log.info("[AI] Retrying after TPM wait...");
                return callGroq(prompt, maxTokens);
            } else {
                throw e;
            }
        }
    }

    /* ========================================================
       GROQ CALL
       ======================================================== */

    private String callGroq(String prompt, int maxTokens) throws Exception {

        Map<String, Object> body = Map.of(
                "model", groqModel,
                "messages", List.of(
                        Map.of(
                                "role", "user",
                                "content", prompt
                        )
                ),
                "temperature", 0.2,
                "max_tokens", maxTokens
        );

        String json = mapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(groqApiUrl))
                .header("Authorization", "Bearer " + groqApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .timeout(Duration.ofSeconds(60))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        JsonNode root = mapper.readTree(response.body());

        if (root.has("error")) {
            String errorType    = root.path("error").path("type").asText("unknown");
            String errorMessage = root.path("error").path("message").asText("no message");
            log.error("[GROQ ERROR] type={} | message={}", errorType, errorMessage);

            double retryAfter = parseRetryAfterSeconds(errorMessage);
            boolean isTPM = errorMessage.contains("tokens per minute");

            throw new RateLimitException(errorType, errorMessage, retryAfter, isTPM);
        }

        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            log.error("[GROQ ERROR] Response has no choices. Full response: {}", response.body());
            throw new RuntimeException("Groq returned no choices in response");
        }

        String content = root.path("choices")
                .path(0)
                .path("message")
                .path("content")
                .asText();

        if (content == null || content.isBlank()) {
            log.error("[GROQ ERROR] Content field is blank. Full response: {}", response.body());
            throw new RuntimeException("Groq returned blank content");
        }

        String finishReason = root.path("choices")
                .path(0)
                .path("finish_reason")
                .asText("unknown");

        if ("length".equals(finishReason)) {
            log.warn("[GROQ WARN] Response truncated (finish_reason=length).");
        } else {
            log.info("[GROQ] finish_reason={}", finishReason);
        }

        return content;
    }

    /* ========================================================
       PARSER
       ======================================================== */

    private AnalysisResult parseAnalysisResponse(
            String raw,
            ProjectContext ctx
    ) throws Exception {

        raw = raw.replace("```json", "")
                .replace("```", "")
                .trim();

        int first = raw.indexOf("{");
        int last = raw.lastIndexOf("}");

        if (first == -1 || last == -1) {
            log.error("[PARSER] No JSON braces found. Raw:\n{}", raw);
            throw new RuntimeException("JSON not found in AI response");
        }

        raw = raw.substring(first, last + 1);

        JsonNode json = mapper.readTree(raw);

        AnalysisResult result = new AnalysisResult();

        result.setStack(json.path("stack").asText(ctx.getProjectType()));
        result.setDeploySteps(json.path("deploySteps").asText());
        result.setEnv(json.path("env").asText());
        result.setCompose(json.path("compose").asText());
        result.setDockerfile(json.path("dockerfile").asText());
        result.setGithubActions(json.path("githubActions").asText());

        List<String> recs = new ArrayList<>();
        JsonNode arr = json.path("recommendations");
        if (arr.isArray()) {
            for (JsonNode item : arr) {
                recs.add(item.asText());
            }
        }
        result.setRecommendations(recs);

        patchBlanks(result, ctx);

        return result;
    }

    /* ========================================================
       BLANK FIELD PATCHER
       ======================================================== */

    private void patchBlanks(AnalysisResult result, ProjectContext ctx) {

        String type = ctx.getProjectType();
        String port = ctx.getDetectedPort() != null ? ctx.getDetectedPort() : "8080";

        if (isBlank(result.getDeploySteps())) {
            log.warn("[PATCH] deploySteps was blank -- filling with fallback");
            result.setDeploySteps(buildFallbackDeploySteps());
        }
        if (isBlank(result.getEnv())) {
            log.warn("[PATCH] env was blank -- filling with fallback");
            result.setEnv(buildFallbackEnv(type, port));
        }
        if (isBlank(result.getDockerfile())) {
            log.warn("[PATCH] dockerfile was blank -- filling with fallback");
            result.setDockerfile(buildFallbackDockerfile(type, port));
        }
        if (isBlank(result.getCompose())) {
            log.warn("[PATCH] compose was blank -- filling with fallback");
            result.setCompose(buildFallbackCompose(port));
        }
        if (isBlank(result.getGithubActions())) {
            log.warn("[PATCH] githubActions was blank -- filling with fallback");
            result.setGithubActions(buildFallbackGithubActions(type));
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /* ========================================================
       RETRY HELPER
       ======================================================== */

    private double parseRetryAfterSeconds(String message) {
        try {
            Pattern p = Pattern.compile("try again in ([0-9.]+)s");
            Matcher m = p.matcher(message);
            if (m.find()) {
                return Double.parseDouble(m.group(1));
            }
        } catch (Exception ignored) {}
        return Double.MAX_VALUE;
    }

    /* ========================================================
       RATE LIMIT EXCEPTION
       ======================================================== */

    private static class RateLimitException extends RuntimeException {
        private final double retryAfterSeconds;
        private final boolean tpm;

        RateLimitException(String type, String message, double retryAfterSeconds, boolean tpm) {
            super("Groq API error [" + type + "]: " + message);
            this.retryAfterSeconds = retryAfterSeconds;
            this.tpm = tpm;
        }

        double getRetryAfterSeconds() { return retryAfterSeconds; }
        boolean isTPM() { return tpm; }
    }

    /* ========================================================
       FALLBACK TEMPLATES
       ======================================================== */

    private String buildFallbackDeploySteps() {
        return "## Deployment Steps\n\n"
                + "### 1. Clone and Setup\n"
                + "```bash\n"
                + "git clone <your-repo-url>\n"
                + "cd <project-name>\n"
                + "cp .env.example .env\n"
                + "```\n\n"
                + "### 2. Run with Docker Compose\n"
                + "```bash\n"
                + "docker-compose up -d --build\n"
                + "docker-compose logs -f\n"
                + "```\n\n"
                + "### 3. Stop\n"
                + "```bash\n"
                + "docker-compose down\n"
                + "```\n\n"
                + "### 4. Cloud Options\n"
                + "- Railway / Render / Fly.io\n"
                + "- AWS ECS / GCP Cloud Run\n";
    }

    private String buildFallbackEnv(String type, String port) {
        StringBuilder sb = new StringBuilder();
        sb.append("PORT=").append(port).append("\n");
        sb.append("APP_ENV=production\n");
        if (type.contains("Spring Boot") || type.contains("Java")) {
            sb.append("SPRING_PROFILES_ACTIVE=prod\n");
            sb.append("SERVER_PORT=").append(port).append("\n");
        }
        sb.append("DATABASE_URL=postgresql://user:password@localhost:5432/dbname\n");
        sb.append("LOG_LEVEL=info\n");
        return sb.toString();
    }

    private String buildFallbackDockerfile(String type, String port) {
        if (type.contains("Spring Boot") || type.contains("Java")) {
            return "FROM eclipse-temurin:17-jdk-alpine AS build\n"
                    + "WORKDIR /app\n"
                    + "COPY pom.xml .\n"
                    + "COPY src ./src\n"
                    + "RUN apk add --no-cache maven && mvn clean package -DskipTests\n"
                    + "FROM eclipse-temurin:17-jre-alpine\n"
                    + "WORKDIR /app\n"
                    + "COPY --from=build /app/target/*.jar app.jar\n"
                    + "EXPOSE " + port + "\n"
                    + "ENTRYPOINT [\"java\", \"-jar\", \"app.jar\"]\n";
        }
        if (type.contains("Node")) {
            return "FROM node:20-alpine\n"
                    + "WORKDIR /app\n"
                    + "COPY package*.json ./\n"
                    + "RUN npm ci --only=production\n"
                    + "COPY . .\n"
                    + "EXPOSE " + port + "\n"
                    + "CMD [\"node\", \"server.js\"]\n";
        }
        return "FROM ubuntu:22.04\nWORKDIR /app\nCOPY . .\nEXPOSE " + port + "\nCMD [\"bash\"]\n";
    }

    private String buildFallbackCompose(String port) {
        return "version: '3.8'\nservices:\n  app:\n    build: .\n    ports:\n"
                + "      - \"" + port + ":" + port + "\"\n"
                + "    env_file:\n      - .env\n    restart: unless-stopped\n";
    }

    private String buildFallbackGithubActions(String type) {
        return "name: CI/CD Pipeline\non:\n  push:\n    branches: [ main ]\n"
                + "jobs:\n  build:\n    runs-on: ubuntu-latest\n    steps:\n"
                + "      - uses: actions/checkout@v4\n"
                + "      - name: Build Docker image\n        run: docker build -t app:latest .\n";
    }

    private String fallbackExplain(ProjectContext ctx) {
        return "## Project Overview\nThis is a " + ctx.getProjectType() + " application.\n\n"
                + "## Technologies Used\n- " + ctx.getProjectType() + "\n\n"
                + "## Improvements\n- Add tests\n- Add monitoring\n- Add CI/CD\n- Improve logging\n";
    }
}