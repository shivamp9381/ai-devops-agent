package com.devops.agent.util;

import com.devops.agent.model.ProjectContext;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class ProjectScanner {

    public static ProjectContext scan(Path root) throws IOException {

        ProjectContext ctx = new ProjectContext();

        List<String> files = new ArrayList<>();
        Map<String,String> contents = new HashMap<>();

        Files.walk(root).forEach(path -> {
            if (Files.isRegularFile(path)) {
                try {
                    String rel = root.relativize(path).toString().replace("\\","/");
                    files.add(rel);

                    if (Files.size(path) < 50000) {
                        contents.put(rel, Files.readString(path));
                    }
                } catch (Exception ignored){}
            }
        });

        ctx.setFileNames(files);
        ctx.setFileContents(contents);

        String type = detectType(files, contents);
        ctx.setProjectType(type);
        ctx.setDetectedPort("8080");
        ctx.setDetectedServices(detectServices(files, contents));

        return ctx;
    }

    private static String detectType(List<String> files, Map<String,String> c) {

        if (files.contains("pom.xml")) return "Spring Boot";
        if (files.contains("package.json")) return "Node.js";
        if (files.contains("requirements.txt")) return "Python";
        return "Unknown";
    }

    private static List<String> detectServices(List<String> files, Map<String,String> c) {

        List<String> services = new ArrayList<>();

        String all = c.values().toString().toLowerCase();

        if (all.contains("postgres")) services.add("PostgreSQL");
        if (all.contains("mysql")) services.add("MySQL");
        if (all.contains("redis")) services.add("Redis");
        if (all.contains("mongo")) services.add("MongoDB");
        if (all.contains("kafka")) services.add("Kafka");
        if (all.contains("nginx")) services.add("Nginx");

        boolean frontend = files.stream().anyMatch(f -> f.contains("package.json"));
        boolean backend = files.stream().anyMatch(f -> f.contains("pom.xml"));

        if (frontend && backend) services.add("Monorepo");

        return services;
    }
}