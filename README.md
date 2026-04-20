# AI DevOps Agent

AI-powered tool that analyzes any GitHub repository or uploaded project and generates production-ready DevOps assets including Dockerfiles, docker-compose configs, CI/CD pipelines, and deployment guides.

## Features

- **GitHub URL Analysis** — paste any public repo URL and get instant DevOps configs
- **ZIP Upload** — upload a project archive for analysis
- **Tech Stack Detection** — auto-detects Spring Boot, Node.js, React, Next.js, Python, Django, FastAPI, Flask, Go, and more
- **AI-Powered** — uses Groq AI (Llama 3) for intelligent analysis with rule-based fallback
- **Complete Output** — Dockerfile, docker-compose.yml, .env.example, GitHub Actions CI/CD, deployment steps, and security recommendations
- **Caching** — repeated analysis of the same repo returns cached results
- **Dark/Light Mode** — modern responsive UI

## Tech Stack

- Java 17, Spring Boot 3.2
- Thymeleaf (server-side rendered UI)
- Groq AI API (Llama 3 model)
- JGit for repository cloning
- Spring Cache, Actuator, Validation

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.8+
- Git
- (Optional) Groq API key for AI-powered analysis

### Setup

```bash
# Clone this project
git clone <this-repo-url>
cd ai-devops-agent

# Set your Groq API key (optional — fallback works without it)
export GROQ_API_KEY=your_groq_api_key_here

# Run
mvn spring-boot:run
```

Open **http://localhost:8080** in your browser.

### Getting a Groq API Key

1. Go to [https://console.groq.com](https://console.groq.com)
2. Sign up / log in
3. Create an API key
4. Set it as environment variable: `export GROQ_API_KEY=your_key`

> **Note:** The app works without an API key using the rule-based fallback generator. AI just makes the output smarter.

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/analyze/repo` | Analyze a GitHub repo by URL |
| POST | `/api/analyze/upload` | Analyze an uploaded ZIP file |
| GET | `/actuator/health` | Health check |

### Example: Analyze a repo

```bash
curl -X POST http://localhost:8080/api/analyze/repo \
  -H "Content-Type: application/json" \
  -d '{"repoUrl":"https://github.com/spring-projects/spring-petclinic"}'
```

## Project Structure

```
ai-devops-agent/
├── src/main/java/com/devops/agent/
│   ├── Application.java
│   ├── controller/
│   │   ├── AnalyzeController.java    # REST API
│   │   └── PageController.java       # Serves UI
│   ├── model/
│   │   ├── AnalysisResult.java       # Response model
│   │   ├── ProjectContext.java        # Scanned project data
│   │   └── RepoRequest.java          # Request model
│   ├── service/
│   │   ├── AiService.java            # Groq AI integration
│   │   ├── AnalysisService.java      # Orchestrator
│   │   ├── FallbackGenerator.java    # Rule-based fallback
│   │   └── GitService.java           # JGit clone
│   └── util/
│       └── ProjectScanner.java       # File system scanner
├── src/main/resources/
│   ├── application.properties
│   ├── templates/index.html          # Thymeleaf UI
│   └── static/
│       ├── css/style.css
│       └── js/app.js
├── pom.xml
├── .gitignore
├── .env.example
└── README.md
```

## Supported Project Types

| Type | Detection |
|------|-----------|
| Spring Boot | pom.xml with spring-boot dependency |
| Node.js | package.json |
| React | package.json with react dependency |
| Next.js | package.json with next dependency |
| Vue.js | package.json with vue dependency |
| Angular | package.json with angular dependency |
| Django | requirements.txt with django / manage.py |
| FastAPI | requirements.txt with fastapi |
| Flask | requirements.txt with flask |
| Go | go.mod |
| Static Site | HTML/CSS files only |

## Security

- API keys loaded from environment variables only, never hardcoded
- ZIP uploads validated for size (50MB max), entry count, and zip-slip attacks
- GitHub URLs sanitized (HTTPS only, no injection)
- Temp directories cleaned up after analysis
- `.gitignore` excludes all sensitive files

## Future Improvements

- Kubernetes manifest generation (Deployment, Service, Ingress)
- Terraform/Pulumi IaC output
- Multi-repo analysis
- Persistent analysis history with database
- User authentication
- Webhook integration for auto-analysis on push

## Screenshots

> *Screenshots placeholder — run the app and capture the UI*

## License

MIT
