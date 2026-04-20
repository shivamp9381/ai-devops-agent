package com.devops.agent.util;

import com.devops.agent.model.ProjectContext;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

public class ProjectScanner {

    private static final Set<String> KEY_FILES = Set.of(
            "package.json", "pom.xml", "build.gradle", "requirements.txt",
            "Pipfile", "go.mod", "Cargo.toml", "Gemfile", "Dockerfile",
            "docker-compose.yml", "docker-compose.yaml", ".env", ".env.example",
            "next.config.js", "next.config.mjs", "nuxt.config.js", "angular.json",
            "tsconfig.json", "webpack.config.js", "vite.config.js", "vite.config.ts",
            "manage.py", "app.py", "main.py", "server.js", "index.js", "app.js"
    );

    private static final Set<String> SKIP_DIRS = Set.of(
            "node_modules", ".git", "target", "build", "dist", ".idea",
            "__pycache__", ".gradle", "vendor", ".next", ".nuxt"
    );

    public static ProjectContext scan(Path root) throws IOException {
        ProjectContext ctx = new ProjectContext();
        List<String> fileNames = new ArrayList<>();
        Map<String, String> fileContents = new HashMap<>();

        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (SKIP_DIRS.contains(dir.getFileName().toString())) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String name = file.getFileName().toString();
                String relative = root.relativize(file).toString().replace("\\", "/");
                fileNames.add(relative);

                if (KEY_FILES.contains(name) && attrs.size() < 50_000) {
                    try {
                        fileContents.put(relative, Files.readString(file));
                    } catch (IOException ignored) {}
                }
                return FileVisitResult.CONTINUE;
            }
        });

        ctx.setFileNames(fileNames);
        ctx.setFileContents(fileContents);
        ctx.setHasDockerfile(fileNames.stream().anyMatch(f -> f.endsWith("Dockerfile")));
        ctx.setHasPackageJson(fileContents.containsKey("package.json"));
        ctx.setHasPomXml(fileContents.containsKey("pom.xml"));
        ctx.setHasRequirementsTxt(fileContents.containsKey("requirements.txt"));
        ctx.setHasGradleBuild(fileNames.stream().anyMatch(f -> f.endsWith("build.gradle")));
        ctx.setHasGoMod(fileContents.containsKey("go.mod"));

        ctx.setProjectType(detectType(ctx, fileContents));
        ctx.setDetectedPort(detectPort(ctx, fileContents));
        ctx.setStartCommand(detectStartCommand(ctx));
        ctx.setBuildCommand(detectBuildCommand(ctx));

        return ctx;
    }

    private static String detectType(ProjectContext ctx, Map<String, String> contents) {
        if (ctx.isHasPomXml()) {
            String pom = contents.getOrDefault("pom.xml", "");
            if (pom.contains("spring-boot")) return "Spring Boot";
            return "Java Maven";
        }
        if (ctx.isHasGradleBuild()) return "Java Gradle";
        if (ctx.isHasPackageJson()) {
            String pkg = contents.getOrDefault("package.json", "");
            if (pkg.contains("\"next\"")) return "Next.js";
            if (pkg.contains("\"nuxt\"")) return "Nuxt.js";
            if (pkg.contains("\"react\"")) return "React";
            if (pkg.contains("\"vue\"")) return "Vue.js";
            if (pkg.contains("\"angular\"")) return "Angular";
            if (pkg.contains("\"express\"")) return "Node.js Express";
            if (pkg.contains("\"fastify\"")) return "Node.js Fastify";
            return "Node.js";
        }
        if (ctx.isHasRequirementsTxt() || contents.containsKey("Pipfile")) {
            String reqs = contents.getOrDefault("requirements.txt", "");
            if (reqs.contains("django") || contents.containsKey("manage.py")) return "Django";
            if (reqs.contains("fastapi")) return "FastAPI";
            if (reqs.contains("flask")) return "Flask";
            return "Python";
        }
        if (ctx.isHasGoMod()) return "Go";
        if (ctx.getFileNames().stream().anyMatch(f -> f.endsWith(".html") || f.endsWith(".css"))) {
            return "Static Site";
        }
        return "Unknown";
    }

    private static String detectPort(ProjectContext ctx, Map<String, String> contents) {
        String type = ctx.getProjectType();
        if (type.contains("Spring Boot") || type.contains("Java")) return "8080";
        if (type.contains("Next.js")) return "3000";
        if (type.contains("React") || type.contains("Vue") || type.contains("Angular")) return "3000";
        if (type.contains("Node.js")) return "3000";
        if (type.contains("Django")) return "8000";
        if (type.contains("FastAPI")) return "8000";
        if (type.contains("Flask")) return "5000";
        if (type.contains("Go")) return "8080";
        if (type.contains("Static")) return "80";
        return "8080";
    }

    private static String detectStartCommand(ProjectContext ctx) {
        String type = ctx.getProjectType();
        if (type.contains("Spring Boot")) return "java -jar app.jar";
        if (type.contains("Java")) return "java -jar app.jar";
        if (type.contains("Next.js")) return "npm start";
        if (type.contains("Node.js")) return "node server.js";
        if (type.contains("React")) return "npm start";
        if (type.contains("Django")) return "python manage.py runserver 0.0.0.0:8000";
        if (type.contains("FastAPI")) return "uvicorn main:app --host 0.0.0.0 --port 8000";
        if (type.contains("Flask")) return "python app.py";
        if (type.contains("Go")) return "./main";
        return "echo 'start command not detected'";
    }

    private static String detectBuildCommand(ProjectContext ctx) {
        String type = ctx.getProjectType();
        if (type.contains("Spring Boot") || type.contains("Java Maven")) return "mvn clean package -DskipTests";
        if (type.contains("Java Gradle")) return "./gradlew build -x test";
        if (type.contains("Next.js") || type.contains("React") || type.contains("Vue") || type.contains("Angular")) return "npm run build";
        if (type.contains("Node.js")) return "npm install";
        if (type.contains("Go")) return "go build -o main .";
        return "";
    }
}
