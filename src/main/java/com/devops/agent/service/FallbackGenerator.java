package com.devops.agent.service;

import com.devops.agent.model.AnalysisResult;
import com.devops.agent.model.ProjectContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FallbackGenerator {

    public AnalysisResult generate(ProjectContext ctx) {
        AnalysisResult result = new AnalysisResult();
        String type = ctx.getProjectType();
        String port = ctx.getDetectedPort();

        result.setStack(type);
        result.setDockerfile(generateDockerfile(type, port, ctx));
        result.setCompose(generateCompose(type, port));
        result.setEnv(generateEnv(type, port));
        result.setGithubActions(generateGithubActions(type, ctx));
        result.setDeploySteps(generateDeploySteps(type));
        result.setRecommendations(generateRecommendations(type, ctx));

        return result;
    }

    private String generateDockerfile(String type, String port, ProjectContext ctx) {
        if (type.contains("Spring Boot") || type.contains("Java Maven")) {
            return """
                    FROM eclipse-temurin:17-jdk-alpine AS build
                    WORKDIR /app
                    COPY pom.xml .
                    COPY src ./src
                    RUN apk add --no-cache maven && mvn clean package -DskipTests
                    
                    FROM eclipse-temurin:17-jre-alpine
                    WORKDIR /app
                    COPY --from=build /app/target/*.jar app.jar
                    EXPOSE %s
                    HEALTHCHECK --interval=30s --timeout=3s CMD wget -qO- http://localhost:%s/actuator/health || exit 1
                    ENTRYPOINT ["java", "-jar", "app.jar"]
                    """.formatted(port, port);
        }
        if (type.contains("Java Gradle")) {
            return """
                    FROM eclipse-temurin:17-jdk-alpine AS build
                    WORKDIR /app
                    COPY . .
                    RUN ./gradlew build -x test
                    
                    FROM eclipse-temurin:17-jre-alpine
                    WORKDIR /app
                    COPY --from=build /app/build/libs/*.jar app.jar
                    EXPOSE %s
                    ENTRYPOINT ["java", "-jar", "app.jar"]
                    """.formatted(port);
        }
        if (type.contains("Next.js")) {
            return """
                    FROM node:20-alpine AS build
                    WORKDIR /app
                    COPY package*.json ./
                    RUN npm ci
                    COPY . .
                    RUN npm run build
                    
                    FROM node:20-alpine
                    WORKDIR /app
                    COPY --from=build /app/.next ./.next
                    COPY --from=build /app/node_modules ./node_modules
                    COPY --from=build /app/package.json ./
                    COPY --from=build /app/public ./public
                    EXPOSE %s
                    CMD ["npm", "start"]
                    """.formatted(port);
        }
        if (type.contains("React")) {
            return """
                    FROM node:20-alpine AS build
                    WORKDIR /app
                    COPY package*.json ./
                    RUN npm ci
                    COPY . .
                    RUN npm run build
                    
                    FROM nginx:alpine
                    COPY --from=build /app/build /usr/share/nginx/html
                    EXPOSE 80
                    CMD ["nginx", "-g", "daemon off;"]
                    """;
        }
        if (type.contains("Node.js")) {
            return """
                    FROM node:20-alpine
                    WORKDIR /app
                    COPY package*.json ./
                    RUN npm ci --only=production
                    COPY . .
                    EXPOSE %s
                    HEALTHCHECK --interval=30s --timeout=3s CMD wget -qO- http://localhost:%s/health || exit 1
                    CMD ["node", "server.js"]
                    """.formatted(port, port);
        }
        if (type.contains("Django")) {
            return """
                    FROM python:3.11-slim
                    WORKDIR /app
                    COPY requirements.txt .
                    RUN pip install --no-cache-dir -r requirements.txt
                    COPY . .
                    RUN python manage.py collectstatic --noinput || true
                    EXPOSE %s
                    CMD ["gunicorn", "--bind", "0.0.0.0:%s", "config.wsgi:application"]
                    """.formatted(port, port);
        }
        if (type.contains("FastAPI")) {
            return """
                    FROM python:3.11-slim
                    WORKDIR /app
                    COPY requirements.txt .
                    RUN pip install --no-cache-dir -r requirements.txt
                    COPY . .
                    EXPOSE %s
                    CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "%s"]
                    """.formatted(port, port);
        }
        if (type.contains("Flask")) {
            return """
                    FROM python:3.11-slim
                    WORKDIR /app
                    COPY requirements.txt .
                    RUN pip install --no-cache-dir -r requirements.txt
                    COPY . .
                    EXPOSE %s
                    CMD ["python", "app.py"]
                    """.formatted(port);
        }
        if (type.contains("Go")) {
            return """
                    FROM golang:1.22-alpine AS build
                    WORKDIR /app
                    COPY go.mod go.sum ./
                    RUN go mod download
                    COPY . .
                    RUN CGO_ENABLED=0 go build -o main .
                    
                    FROM alpine:3.19
                    WORKDIR /app
                    COPY --from=build /app/main .
                    EXPOSE %s
                    CMD ["./main"]
                    """.formatted(port);
        }
        if (type.contains("Static")) {
            return """
                    FROM nginx:alpine
                    COPY . /usr/share/nginx/html
                    EXPOSE 80
                    CMD ["nginx", "-g", "daemon off;"]
                    """;
        }
        return """
                FROM ubuntu:22.04
                WORKDIR /app
                COPY . .
                EXPOSE %s
                CMD ["bash"]
                """.formatted(port);
    }

    private String generateCompose(String type, String port) {
        return """
                version: '3.8'
                services:
                  app:
                    build: .
                    ports:
                      - "%s:%s"
                    env_file:
                      - .env
                    restart: unless-stopped
                    healthcheck:
                      test: ["CMD", "wget", "-qO-", "http://localhost:%s/"]
                      interval: 30s
                      timeout: 10s
                      retries: 3
                """.formatted(port, port, port);
    }

    private String generateEnv(String type, String port) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Application Configuration\n");
        sb.append("PORT=").append(port).append("\n");
        sb.append("NODE_ENV=production\n");
        sb.append("APP_ENV=production\n");

        if (type.contains("Spring Boot") || type.contains("Java")) {
            sb.append("SPRING_PROFILES_ACTIVE=prod\n");
            sb.append("SERVER_PORT=").append(port).append("\n");
            sb.append("JAVA_OPTS=-Xmx512m\n");
        }
        if (type.contains("Django")) {
            sb.append("DJANGO_SECRET_KEY=change-me-in-production\n");
            sb.append("DJANGO_DEBUG=false\n");
            sb.append("ALLOWED_HOSTS=*\n");
        }
        if (type.contains("FastAPI") || type.contains("Flask")) {
            sb.append("FLASK_ENV=production\n");
            sb.append("SECRET_KEY=change-me-in-production\n");
        }

        sb.append("\n# Database (if needed)\n");
        sb.append("DATABASE_URL=postgresql://user:password@localhost:5432/dbname\n");
        sb.append("\n# Logging\n");
        sb.append("LOG_LEVEL=info\n");

        return sb.toString();
    }

    private String generateGithubActions(String type, ProjectContext ctx) {
        String buildSteps;
        if (type.contains("Spring Boot") || type.contains("Java Maven")) {
            buildSteps = """
                        - name: Set up JDK 17
                          uses: actions/setup-java@v4
                          with:
                            java-version: '17'
                            distribution: 'temurin'
                            cache: maven
                        - name: Build with Maven
                          run: mvn clean package -DskipTests
                        - name: Run tests
                          run: mvn test
                    """;
        } else if (type.contains("Java Gradle")) {
            buildSteps = """
                        - name: Set up JDK 17
                          uses: actions/setup-java@v4
                          with:
                            java-version: '17'
                            distribution: 'temurin'
                            cache: gradle
                        - name: Build with Gradle
                          run: ./gradlew build
                    """;
        } else if (type.contains("Node") || type.contains("React") || type.contains("Next") || type.contains("Vue") || type.contains("Angular")) {
            buildSteps = """
                        - name: Set up Node.js
                          uses: actions/setup-node@v4
                          with:
                            node-version: '20'
                            cache: 'npm'
                        - name: Install dependencies
                          run: npm ci
                        - name: Build
                          run: npm run build
                        - name: Test
                          run: npm test --if-present
                    """;
        } else if (type.contains("Python") || type.contains("Django") || type.contains("FastAPI") || type.contains("Flask")) {
            buildSteps = """
                        - name: Set up Python
                          uses: actions/setup-python@v5
                          with:
                            python-version: '3.11'
                        - name: Install dependencies
                          run: pip install -r requirements.txt
                        - name: Run tests
                          run: python -m pytest || true
                    """;
        } else if (type.contains("Go")) {
            buildSteps = """
                        - name: Set up Go
                          uses: actions/setup-go@v5
                          with:
                            go-version: '1.22'
                        - name: Build
                          run: go build -v ./...
                        - name: Test
                          run: go test -v ./...
                    """;
        } else {
            buildSteps = """
                        - name: Build
                          run: echo "Add build steps here"
                    """;
        }

        return """
                name: CI/CD Pipeline
                
                on:
                  push:
                    branches: [ main, develop ]
                  pull_request:
                    branches: [ main ]
                
                jobs:
                  build:
                    runs-on: ubuntu-latest
                    steps:
                      - name: Checkout code
                        uses: actions/checkout@v4
                %s
                  docker:
                    needs: build
                    runs-on: ubuntu-latest
                    if: github.ref == 'refs/heads/main'
                    steps:
                      - name: Checkout code
                        uses: actions/checkout@v4
                      - name: Set up Docker Buildx
                        uses: docker/setup-buildx-action@v3
                      - name: Login to Docker Hub
                        uses: docker/login-action@v3
                        with:
                          username: ${{ secrets.DOCKER_USERNAME }}
                          password: ${{ secrets.DOCKER_PASSWORD }}
                      - name: Build and push
                        uses: docker/build-push-action@v5
                        with:
                          context: .
                          push: true
                          tags: ${{ secrets.DOCKER_USERNAME }}/app:latest
                """.formatted(buildSteps);
    }

    private String generateDeploySteps(String type) {
        return """
                ## Deployment Steps
                
                ### 1. Local Development
                ```bash
                # Clone the repository
                git clone <repo-url>
                cd <project-name>
                
                # Copy environment file
                cp .env.example .env
                # Edit .env with your values
                ```
                
                ### 2. Docker Deployment
                ```bash
                # Build and run with Docker Compose
                docker-compose up -d --build
                
                # Check logs
                docker-compose logs -f
                
                # Stop
                docker-compose down
                ```
                
                ### 3. Production Deployment
                ```bash
                # Build Docker image
                docker build -t myapp:latest .
                
                # Push to registry
                docker tag myapp:latest registry.example.com/myapp:latest
                docker push registry.example.com/myapp:latest
                
                # Deploy (example with Docker)
                docker run -d --name myapp -p 80:PORT --env-file .env myapp:latest
                ```
                
                ### 4. Cloud Deployment Options
                - **AWS**: ECS, EKS, or Elastic Beanstalk
                - **GCP**: Cloud Run, GKE
                - **Azure**: Container Apps, AKS
                - **Railway / Render / Fly.io**: Connect GitHub repo for auto-deploy
                """;
    }

    private List<String> generateRecommendations(String type, ProjectContext ctx) {
        List<String> recs = new ArrayList<>();

        if (!ctx.isHasDockerfile()) {
            recs.add("Add a Dockerfile to containerize your application");
        }
        if (ctx.getFileNames().stream().noneMatch(f -> f.contains(".github/workflows"))) {
            recs.add("Set up CI/CD with GitHub Actions");
        }
        if (ctx.getFileNames().stream().noneMatch(f -> f.equals(".env.example"))) {
            recs.add("Add .env.example to document required environment variables");
        }
        if (ctx.getFileNames().stream().noneMatch(f -> f.equals(".gitignore"))) {
            recs.add("Add a .gitignore file to exclude build artifacts and secrets");
        }
        if (ctx.getFileNames().stream().noneMatch(f -> f.equals("README.md"))) {
            recs.add("Add a README.md with setup and deployment instructions");
        }

        recs.add("Use multi-stage Docker builds to reduce image size");
        recs.add("Add health check endpoints for container orchestration");
        recs.add("Use Docker secrets or a vault for sensitive configuration");
        recs.add("Set up monitoring with Prometheus/Grafana or cloud-native tools");
        recs.add("Enable HTTPS with a reverse proxy (nginx/traefik) in production");
        recs.add("Implement rate limiting and input validation for security");

        return recs;
    }
}
