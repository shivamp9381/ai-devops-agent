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

    private static final Logger log = LoggerFactory.getLogger(AiService.class);
    private static final int MAX_RETRIES = 3;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
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
            log.warn("No GROQ_API_KEY configured, using fallback generator");
            return null;
        }

        String prompt = buildPrompt(ctx);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                String response = callGroq(prompt);
                AnalysisResult result = parseResponse(response, ctx);
                if (result != null) return result;
            } catch (Exception e) {
                log.warn("AI attempt {}/{} failed: {}", attempt, MAX_RETRIES, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep((long) Math.pow(2, attempt) * 1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        log.warn("All AI attempts failed, returning null for fallback");
        return null;
    }

    private String buildPrompt(ProjectContext ctx) {
        String fileList = ctx.getFileNames().stream()
                .limit(100)
                .collect(Collectors.joining("\n"));

        StringBuilder keyContents = new StringBuilder();
        for (Map.Entry<String, String> entry : ctx.getFileContents().entrySet()) {
            String content = entry.getValue();
            if (content.length() > 2000) content = content.substring(0, 2000) + "\n... (truncated)";
            keyContents.append("--- ").append(entry.getKey()).append(" ---\n");
            keyContents.append(content).append("\n\n");
        }

        return """
                You are a DevOps expert. Analyze this project and generate deployment configurations.
                
                Project type detected: %s
                Port: %s
                
                File listing:
                %s
                
                Key file contents:
                %s
                
                Generate a JSON response with EXACTLY these fields:
                {
                  "stack": "detected technology stack description",
                  "dockerfile": "complete multi-stage Dockerfile content",
                  "compose": "complete docker-compose.yml content",
                  "env": "complete .env.example content with all needed variables",
                  "githubActions": "complete GitHub Actions CI/CD workflow YAML",
                  "deploySteps": "markdown formatted deployment steps",
                  "recommendations": ["array", "of", "security and config recommendations"]
                }
                
                Requirements:
                - Dockerfile must be production-ready with multi-stage builds where applicable
                - Include health checks in Dockerfile and compose
                - GitHub Actions should build, test, and build Docker image
                - Recommendations should include security best practices
                - All generated configs must be real and executable, no placeholders
                
                Return ONLY valid JSON, no markdown code fences, no extra text.
                """.formatted(ctx.getProjectType(), ctx.getDetectedPort(), fileList, keyContents);
    }

    private String callGroq(String prompt) throws Exception {
        Map<String, Object> body = Map.of(
                "model", groqModel,
                "messages", List.of(
                        Map.of("role", "system", "content", "You are a DevOps expert. Return only valid JSON."),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.3,
                "max_tokens", 4096
        );

        String jsonBody = mapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(groqApiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + groqApiKey)
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Groq API returned status " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = mapper.readTree(response.body());
        return root.path("choices").path(0).path("message").path("content").asText();
    }

    private AnalysisResult parseResponse(String response, ProjectContext ctx) {
        try {
            String cleaned = response.strip();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("^```[a-z]*\\n?", "").replaceAll("\\n?```$", "");
            }

            JsonNode json = mapper.readTree(cleaned);
            AnalysisResult result = new AnalysisResult();
            result.setStack(json.path("stack").asText(ctx.getProjectType()));
            result.setDockerfile(json.path("dockerfile").asText(""));
            result.setCompose(json.path("compose").asText(""));
            result.setEnv(json.path("env").asText(""));
            result.setGithubActions(json.path("githubActions").asText(""));
            result.setDeploySteps(json.path("deploySteps").asText(""));

            List<String> recs = new ArrayList<>();
            JsonNode recsNode = json.path("recommendations");
            if (recsNode.isArray()) {
                for (JsonNode r : recsNode) {
                    recs.add(r.asText());
                }
            }
            result.setRecommendations(recs);

            if (result.getDockerfile().isBlank()) return null;
            return result;
        } catch (Exception e) {
            log.warn("Failed to parse AI response: {}", e.getMessage());
            return null;
        }
    }
}
