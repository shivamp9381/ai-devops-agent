package com.devops.agent.service;

import com.devops.agent.model.AnalysisResult;
import com.devops.agent.model.ProjectContext;

import java.util.*;

public class SecurityScanner {

    public static void scan(ProjectContext ctx, AnalysisResult result) {

        List<String> risks = new ArrayList<>();
        int score = 100;

        if (!ctx.getFileNames().contains(".gitignore")) {
            risks.add("Missing .gitignore");
            score -= 15;
        }

        for (String content : ctx.getFileContents().values()) {

            String lower = content.toLowerCase();

            if (lower.contains("password=")) {
                risks.add("Hardcoded password detected");
                score -= 25;
            }

            if (lower.contains("api_key") || lower.contains("secret_key")) {
                risks.add("Possible API key exposed");
                score -= 25;
            }
        }

        if (score < 0) score = 0;

        result.setSecurityScore(score);
        result.setRisks(risks);

        if (score >= 80) result.setSecurityStatus("🟢 Safe");
        else if (score >= 50) result.setSecurityStatus("🟡 Medium Risk");
        else result.setSecurityStatus("🔴 High Risk");
    }
}