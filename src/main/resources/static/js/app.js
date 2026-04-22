//let latestData = {};
//
//document.addEventListener("DOMContentLoaded", () => {
//    initTabs();
//    initRepoForm();
//    initUploadForm();
//    loadTheme();
//});
//
///* ----------------------------
//   Tabs
//---------------------------- */
//
//function initTabs() {
//
//    document.querySelectorAll(".tab").forEach(tab => {
//
//        tab.addEventListener("click", () => {
//
//            document.querySelectorAll(".tab")
//                .forEach(t => t.classList.remove("active"));
//
//            document.querySelectorAll(".tab-content")
//                .forEach(c => c.classList.remove("active"));
//
//            tab.classList.add("active");
//
//            document
//                .getElementById("tab-" + tab.dataset.tab)
//                .classList.add("active");
//        });
//    });
//}
//
///* ----------------------------
//   Repo Analyze
//---------------------------- */
//
//function initRepoForm() {
//
//    document.getElementById("repoForm")
//        .addEventListener("submit", async e => {
//
//            e.preventDefault();
//
//            const btn = document.getElementById("repoBtn");
//
//            setLoading(btn, true);
//
//            try {
//
//                const repoUrl =
//                    document.getElementById("repoUrl").value.trim();
//
//                const res = await fetch("/api/analyze/repo", {
//                    method: "POST",
//                    headers: {
//                        "Content-Type": "application/json"
//                    },
//                    body: JSON.stringify({
//                        repoUrl: repoUrl
//                    })
//                });
//
//                const data = await res.json();
//
//                if (!res.ok) {
//                    alert(data.error || "Analysis failed");
//                    return;
//                }
//
//                render(data);
//
//            } catch (e) {
//                alert("Analysis failed");
//            }
//
//            setLoading(btn, false);
//        });
//}
//
///* ----------------------------
//   Upload ZIP
//---------------------------- */
//
//function initUploadForm() {
//
//    document.getElementById("uploadForm")
//        .addEventListener("submit", async e => {
//
//            e.preventDefault();
//
//            const btn = document.getElementById("uploadBtn");
//
//            setLoading(btn, true);
//
//            try {
//
//                const file =
//                    document.getElementById("zipFile").files[0];
//
//                const fd = new FormData();
//
//                fd.append("file", file);
//
//                const res = await fetch("/api/analyze/upload", {
//                    method: "POST",
//                    body: fd
//                });
//
//                const data = await res.json();
//
//                if (!res.ok) {
//                    alert(data.error || "Upload failed");
//                    return;
//                }
//
//                render(data);
//
//            } catch (e) {
//                alert("Upload failed");
//            }
//
//            setLoading(btn, false);
//        });
//}
//
///* ----------------------------
//   Debug Error
//---------------------------- */
//
//async function debugError() {
//
//    const error =
//        document.getElementById("errorText").value;
//
//    const res = await fetch("/api/debug", {
//        method: "POST",
//        headers: {
//            "Content-Type": "application/json"
//        },
//        body: JSON.stringify({
//            error: error
//        })
//    });
//
//    const txt = await res.text();
//
//    document.getElementById("debugOutput")
//        .textContent = txt;
//}
//
///* ----------------------------
//   Loading
//---------------------------- */
//
//function setLoading(button, loading) {
//
//    const text =
//        button.querySelector(".btn-text");
//
//    const spinner =
//        button.querySelector(".spinner");
//
//    if (loading) {
//        button.disabled = true;
//        text.classList.add("hidden");
//        spinner.classList.remove("hidden");
//    } else {
//        button.disabled = false;
//        text.classList.remove("hidden");
//        spinner.classList.add("hidden");
//    }
//}
//
///* ----------------------------
//   Render Results
//---------------------------- */
//
//function render(data) {
//
//    latestData = data;
//
//    document.getElementById("results")
//        .classList.remove("hidden");
//
//    document.getElementById("stackBadge")
//        .textContent = data.stack || "Detected Stack";
//
//    document.getElementById("security")
//        .textContent =
//        (data.securityStatus || "") +
//        " (" +
//        (data.securityScore || 0) +
//        "/100)";
//
//    setText("readme", data.readme);
//    setText("dockerfile", data.dockerfile);
//    setText("compose", data.compose);
//    setText("env", data.env);
//    setText("githubActions", data.githubActions);
//    setText("deploySteps", data.deploySteps);
//
//    /* NEW FEATURE */
//    setText("repoExplanation", data.repoExplanation);
//
//    window.scrollTo({
//        top:
//            document.getElementById("results")
//                .offsetTop - 20,
//        behavior: "smooth"
//    });
//}
//
//function setText(id, value) {
//
//    document.getElementById(id)
//        .textContent = value || "";
//}
//
///* ----------------------------
//   Theme
//---------------------------- */
//
//function toggleTheme() {
//
//    document.body.classList.toggle("light");
//
//    const mode =
//        document.body.classList.contains("light")
//            ? "light"
//            : "dark";
//
//    localStorage.setItem("theme", mode);
//}
//
//function loadTheme() {
//
//    const mode =
//        localStorage.getItem("theme");
//
//    if (mode === "light") {
//        document.body.classList.add("light");
//    }
//}
//
///* ----------------------------
//   Downloads
//---------------------------- */
//
//function downloadSingle(fileName, id) {
//
//    const text =
//        document.getElementById(id).textContent;
//
//    const blob = new Blob(
//        [text],
//        { type: "text/plain" }
//    );
//
//    const a =
//        document.createElement("a");
//
//    a.href = URL.createObjectURL(blob);
//    a.download = fileName;
//    a.click();
//}
//
//function downloadZip() {
//
//    let content = "";
//
//    content += "README.md\n\n";
//    content += (latestData.readme || "") + "\n\n";
//
//    content += "Dockerfile\n\n";
//    content += (latestData.dockerfile || "") + "\n\n";
//
//    content += "docker-compose.yml\n\n";
//    content += (latestData.compose || "") + "\n\n";
//
//    content += ".env.example\n\n";
//    content += (latestData.env || "") + "\n\n";
//
//    content += "deploy.yml\n\n";
//    content += (latestData.githubActions || "") + "\n\n";
//
//    content += "repo-explanation.md\n\n";
//    content += (latestData.repoExplanation || "") + "\n\n";
//
//    const blob = new Blob(
//        [content],
//        { type: "text/plain" }
//    );
//
//    const a =
//        document.createElement("a");
//
//    a.href = URL.createObjectURL(blob);
//    a.download = "devops-assets.txt";
//    a.click();
//}


let latestData = {};

document.addEventListener("DOMContentLoaded", () => {
    initTabs();
    initRepoForm();
    initUploadForm();
    initDiagramForm();
    loadTheme();
});

/* ----------------------------
   Tabs
---------------------------- */

function initTabs() {

    document.querySelectorAll(".tab").forEach(tab => {

        tab.addEventListener("click", () => {

            document.querySelectorAll(".tab")
                .forEach(t => t.classList.remove("active"));

            document.querySelectorAll(".tab-content")
                .forEach(c => c.classList.remove("active"));

            tab.classList.add("active");

            document
                .getElementById("tab-" + tab.dataset.tab)
                .classList.add("active");
        });
    });
}

/* ----------------------------
   Repo Analyze
---------------------------- */

function initRepoForm() {

    document.getElementById("repoForm")
        .addEventListener("submit", async e => {

            e.preventDefault();

            const btn = document.getElementById("repoBtn");

            setLoading(btn, true);

            try {

                const repoUrl =
                    document.getElementById("repoUrl").value.trim();

                const res = await fetch("/api/analyze/repo", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ repoUrl: repoUrl })
                });

                const data = await res.json();

                if (!res.ok) {
                    alert(data.error || "Analysis failed");
                    return;
                }

                render(data);

            } catch (e) {
                alert("Analysis failed");
            }

            setLoading(btn, false);
        });
}

/* ----------------------------
   Upload ZIP
---------------------------- */

function initUploadForm() {

    document.getElementById("uploadForm")
        .addEventListener("submit", async e => {

            e.preventDefault();

            const btn = document.getElementById("uploadBtn");

            setLoading(btn, true);

            try {

                const file =
                    document.getElementById("zipFile").files[0];

                const fd = new FormData();
                fd.append("file", file);

                const res = await fetch("/api/analyze/upload", {
                    method: "POST",
                    body: fd
                });

                const data = await res.json();

                if (!res.ok) {
                    alert(data.error || "Upload failed");
                    return;
                }

                render(data);

            } catch (e) {
                alert("Upload failed");
            }

            setLoading(btn, false);
        });
}

/* ----------------------------
   Architecture Diagram
---------------------------- */

function initDiagramForm() {

    const form = document.getElementById("diagramForm");
    if (!form) return;

    form.addEventListener("submit", async e => {

        e.preventDefault();

        const btn = document.getElementById("diagramBtn");
        const resultSection = document.getElementById("diagramResult");
        const diagramContainer = document.getElementById("diagramContainer");
        const diagramSource = document.getElementById("diagramSource");
        const diagramMeta = document.getElementById("diagramMeta");
        const diagramError = document.getElementById("diagramError");

        setLoading(btn, true);
        resultSection.classList.add("hidden");
        diagramError.classList.add("hidden");

        try {

            const repoUrl =
                document.getElementById("diagramRepoUrl").value.trim();

            const res = await fetch("/api/diagram/repo", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ repoUrl: repoUrl })
            });

            const data = await res.json();

            if (!res.ok) {
                diagramError.textContent = data.error || "Diagram generation failed";
                diagramError.classList.remove("hidden");
                return;
            }

            // Show metadata
            diagramMeta.innerHTML =
                `<span class="stack-badge">${data.projectType || "Unknown"}</span>` +
                (data.services && data.services.length > 0
                    ? data.services.map(s =>
                        `<span class="service-badge">${s}</span>`
                    ).join("")
                    : "");

            // Show raw source
            diagramSource.textContent = data.mermaid;

            // Render with Mermaid.js
            await renderMermaid(diagramContainer, data.mermaid);

            resultSection.classList.remove("hidden");

            // Smooth scroll
            resultSection.scrollIntoView({ behavior: "smooth", block: "start" });

        } catch (err) {
            diagramError.textContent = "Failed to generate diagram: " + err.message;
            diagramError.classList.remove("hidden");
        }

        setLoading(btn, false);
    });
}

/* ----------------------------
   Mermaid Renderer
---------------------------- */

async function renderMermaid(container, mermaidCode) {

    // Load Mermaid.js from CDN if not already loaded
    if (typeof mermaid === "undefined") {
        await loadScript("https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js");
    }

    mermaid.initialize({
        startOnLoad: false,
        theme: document.body.classList.contains("light") ? "default" : "dark",
        flowchart: {
            curve: "basis",
            padding: 20
        },
        themeVariables: {
            primaryColor: "#6366f1",
            primaryTextColor: "#ffffff",
            primaryBorderColor: "#8b5cf6",
            lineColor: "#8b5cf6",
            secondaryColor: "#0d1a36",
            tertiaryColor: "#0d1a36",
            background: "#07142d",
            mainBkg: "#0d1a36",
            nodeBorder: "#6366f1",
            clusterBkg: "#111c35",
            titleColor: "#ffffff",
            edgeLabelBackground: "#0d1a36",
            fontFamily: "JetBrains Mono, monospace"
        }
    });

    try {
        const id = "mermaid-diagram-" + Date.now();
        const { svg } = await mermaid.render(id, mermaidCode);
        container.innerHTML = svg;

        // Make SVG responsive
        const svgEl = container.querySelector("svg");
        if (svgEl) {
            svgEl.style.maxWidth = "100%";
            svgEl.style.height = "auto";
        }

    } catch (err) {
        container.innerHTML =
            `<div class="diagram-error-msg">
                ⚠️ Could not render diagram.<br>
                <small>${err.message}</small>
            </div>`;
        console.error("Mermaid render error:", err);
    }
}

function loadScript(src) {
    return new Promise((resolve, reject) => {
        const s = document.createElement("script");
        s.src = src;
        s.onload = resolve;
        s.onerror = reject;
        document.head.appendChild(s);
    });
}

/* ----------------------------
   Debug Error
---------------------------- */

async function debugError() {

    const error = document.getElementById("errorText").value;

    const res = await fetch("/api/debug", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ error: error })
    });

    const txt = await res.text();

    document.getElementById("debugOutput").textContent = txt;
}

/* ----------------------------
   Loading
---------------------------- */

function setLoading(button, loading) {

    const text = button.querySelector(".btn-text");
    const spinner = button.querySelector(".spinner");

    if (loading) {
        button.disabled = true;
        text.classList.add("hidden");
        spinner.classList.remove("hidden");
    } else {
        button.disabled = false;
        text.classList.remove("hidden");
        spinner.classList.add("hidden");
    }
}

/* ----------------------------
   Render Results
---------------------------- */

function render(data) {

    latestData = data;

    document.getElementById("results").classList.remove("hidden");

    document.getElementById("stackBadge").textContent =
        data.stack || "Detected Stack";

    document.getElementById("security").textContent =
        (data.securityStatus || "") +
        " (" + (data.securityScore || 0) + "/100)";

    setText("readme", data.readme);
    setText("dockerfile", data.dockerfile);
    setText("compose", data.compose);
    setText("env", data.env);
    setText("githubActions", data.githubActions);
    setText("deploySteps", data.deploySteps);
    setText("repoExplanation", data.repoExplanation);

    window.scrollTo({
        top: document.getElementById("results").offsetTop - 20,
        behavior: "smooth"
    });
}

function setText(id, value) {
    const el = document.getElementById(id);
    if (el) el.textContent = value || "";
}

/* ----------------------------
   Theme
---------------------------- */

function toggleTheme() {

    document.body.classList.toggle("light");

    const mode = document.body.classList.contains("light") ? "light" : "dark";

    localStorage.setItem("theme", mode);
}

function loadTheme() {

    const mode = localStorage.getItem("theme");

    if (mode === "light") {
        document.body.classList.add("light");
    }
}

/* ----------------------------
   Downloads
---------------------------- */

function downloadSingle(fileName, id) {

    const text = document.getElementById(id).textContent;

    const blob = new Blob([text], { type: "text/plain" });

    const a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = fileName;
    a.click();
}

function downloadDiagram() {

    const svg = document.querySelector("#diagramContainer svg");

    if (!svg) {
        alert("No diagram to download");
        return;
    }

    // Download as SVG
    const blob = new Blob(
        [svg.outerHTML],
        { type: "image/svg+xml" }
    );

    const a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = "architecture-diagram.svg";
    a.click();
}

function downloadZip() {

    let content = "";

    content += "README.md\n\n" + (latestData.readme || "") + "\n\n";
    content += "Dockerfile\n\n" + (latestData.dockerfile || "") + "\n\n";
    content += "docker-compose.yml\n\n" + (latestData.compose || "") + "\n\n";
    content += ".env.example\n\n" + (latestData.env || "") + "\n\n";
    content += "deploy.yml\n\n" + (latestData.githubActions || "") + "\n\n";
    content += "repo-explanation.md\n\n" + (latestData.repoExplanation || "") + "\n\n";

    const blob = new Blob([content], { type: "text/plain" });

    const a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = "devops-assets.txt";
    a.click();
}