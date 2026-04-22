let latestData = {};
let currentMode = "repo"; // repo | upload | diagram | debug

/* ══════════════════════════════════════════════
   INIT
══════════════════════════════════════════════ */
document.addEventListener("DOMContentLoaded", () => {
    loadTheme();
    syncThemeIcons();
});

/* ══════════════════════════════════════════════
   INPUT MODE SWITCHING
   (the pill tabs inside the input panel)
══════════════════════════════════════════════ */
function setInputMode(mode) {
    currentMode = mode;

    // Update pill tabs
    document.querySelectorAll(".input-tab").forEach(t => {
        t.classList.toggle("active", t.dataset.mode === mode);
    });

    // Update mode panels
    document.querySelectorAll(".input-mode").forEach(m => {
        m.classList.toggle("active", m.id === "mode-" + mode);
    });

    // Sync sidebar nav highlight
    document.querySelectorAll(".nav-item").forEach(n => {
        n.classList.toggle("active", n.dataset.tab === mode);
    });

    // Hide all result sections when switching mode
    hideAllResults();
}

/* ══════════════════════════════════════════════
   SIDEBAR NAV (legacy tab system compatibility)
══════════════════════════════════════════════ */
function switchTab(tab) {
    setInputMode(tab);
    // On mobile, close sidebar after nav click
    closeSidebar();
    // Scroll hero into view
    document.getElementById("heroSection").scrollIntoView({ behavior: "smooth" });
}

/* ══════════════════════════════════════════════
   SIDEBAR MOBILE
══════════════════════════════════════════════ */
function openSidebar() {
    document.getElementById("sidebar").classList.add("open");
    document.getElementById("sidebarOverlay").classList.add("open");
}

function closeSidebar() {
    document.getElementById("sidebar").classList.remove("open");
    document.getElementById("sidebarOverlay").classList.remove("open");
}

/* ══════════════════════════════════════════════
   NEW ANALYSIS
══════════════════════════════════════════════ */
function startNewAnalysis() {
    hideAllResults();
    document.getElementById("repoUrl").value = "";
    document.getElementById("diagramRepoUrl").value = "";
    document.getElementById("errorText").value = "";
    document.getElementById("debugOutput").textContent = "";
    setInputMode("repo");
    document.getElementById("heroSection").scrollIntoView({ behavior: "smooth" });
    closeSidebar();
}

function hideAllResults() {
    document.getElementById("results").classList.add("hidden");
    document.getElementById("debugResultSection").classList.add("hidden");
    document.getElementById("diagramResult").classList.add("hidden");
}

/* ══════════════════════════════════════════════
   FILL SUGGESTION
══════════════════════════════════════════════ */
function fillSuggestion(mode, value) {
    setInputMode(mode);
    if (mode === "repo") {
        document.getElementById("repoUrl").value = value;
    } else if (mode === "diagram") {
        document.getElementById("diagramRepoUrl").value = value;
    }
}

/* ══════════════════════════════════════════════
   REPO ANALYZE — original API preserved
══════════════════════════════════════════════ */
async function handleRepoSubmit(e) {
    e.preventDefault();

    const btn = document.getElementById("repoBtn");
    setLoading(btn, true);
    hideAllResults();

    try {
        const repoUrl = document.getElementById("repoUrl").value.trim();

        const res = await fetch("/api/analyze/repo", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ repoUrl })
        });

        const data = await res.json();

        if (!res.ok) {
            showToast("❌ " + (data.error || "Analysis failed"));
            return;
        }

        render(data);

    } catch (err) {
        showToast("❌ Analysis failed. Check your URL.");
    }

    setLoading(btn, false);
}

/* ══════════════════════════════════════════════
   ZIP UPLOAD — original API preserved
══════════════════════════════════════════════ */
async function handleUploadSubmit(e) {
    e.preventDefault();

    const btn = document.getElementById("uploadBtn");
    setLoading(btn, true);
    hideAllResults();

    try {
        const file = document.getElementById("zipFile").files[0];
        const fd = new FormData();
        fd.append("file", file);

        const res = await fetch("/api/analyze/upload", {
            method: "POST",
            body: fd
        });

        const data = await res.json();

        if (!res.ok) {
            showToast("❌ " + (data.error || "Upload failed"));
            return;
        }

        render(data);

    } catch (err) {
        showToast("❌ Upload failed.");
    }

    setLoading(btn, false);
}

/* ══════════════════════════════════════════════
   DEBUG ERROR — original API preserved
══════════════════════════════════════════════ */
async function handleDebugSubmit() {
    const btn = document.querySelector(".debug-submit-btn");
    const error = document.getElementById("errorText").value.trim();

    if (!error) { showToast("Please paste an error first"); return; }

    setLoading(btn, true);
    hideAllResults();

    try {
        const res = await fetch("/api/debug", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ error })
        });

        const txt = await res.text();

        document.getElementById("debugOutput").textContent = txt;
        document.getElementById("debugResultSection").classList.remove("hidden");

        document.getElementById("debugResultSection")
            .scrollIntoView({ behavior: "smooth", block: "start" });

    } catch (err) {
        showToast("❌ Debug request failed.");
    }

    setLoading(btn, false);
}

// Keep legacy onclick="debugError()" working
async function debugError() {
    await handleDebugSubmit();
}

/* ══════════════════════════════════════════════
   ARCHITECTURE DIAGRAM — original API preserved
══════════════════════════════════════════════ */
async function handleDiagramSubmit(e) {
    e.preventDefault();

    const btn = document.getElementById("diagramBtn");
    const diagramResult = document.getElementById("diagramResult");
    const diagramError  = document.getElementById("diagramError");
    const diagramMeta   = document.getElementById("diagramMeta");
    const diagramContainer = document.getElementById("diagramContainer");
    const diagramSource = document.getElementById("diagramSource");

    setLoading(btn, true);
    hideAllResults();
    diagramError.classList.add("hidden");

    try {
        const repoUrl = document.getElementById("diagramRepoUrl").value.trim();

        const res = await fetch("/api/diagram/repo", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ repoUrl })
        });

        const data = await res.json();

        if (!res.ok) {
            diagramError.textContent = data.error || "Diagram generation failed";
            diagramError.classList.remove("hidden");
            diagramResult.classList.remove("hidden");
            return;
        }

        // Meta
        diagramMeta.textContent =
            (data.projectType || "Unknown") +
            (data.services && data.services.length > 0
                ? " · " + data.services.join(", ")
                : "");

        // Source
        diagramSource.textContent = data.mermaid;

        // Render
        await renderMermaid(diagramContainer, data.mermaid);

        diagramResult.classList.remove("hidden");
        diagramResult.scrollIntoView({ behavior: "smooth", block: "start" });

    } catch (err) {
        showToast("❌ Diagram generation failed: " + err.message);
    }

    setLoading(btn, false);
}

/* ══════════════════════════════════════════════
   MERMAID RENDERER
══════════════════════════════════════════════ */
async function renderMermaid(container, mermaidCode) {

    if (typeof mermaid === "undefined") {
        await loadScript("https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js");
    }

    mermaid.initialize({
        startOnLoad: false,
        theme: document.body.classList.contains("light") ? "default" : "dark",
        flowchart: { curve: "basis", padding: 20 },
        themeVariables: {
            primaryColor:        "#6366f1",
            primaryTextColor:    "#ffffff",
            primaryBorderColor:  "#8b5cf6",
            lineColor:           "#8b5cf6",
            secondaryColor:      "#111827",
            tertiaryColor:       "#111827",
            background:          "#0B1120",
            mainBkg:             "#111827",
            nodeBorder:          "#6366f1",
            clusterBkg:          "#1a2236",
            titleColor:          "#f1f5f9",
            edgeLabelBackground: "#111827",
            fontFamily:          "Inter, sans-serif"
        }
    });

    try {
        const id = "mermaid-" + Date.now();
        const { svg } = await mermaid.render(id, mermaidCode);
        container.innerHTML = svg;

        const svgEl = container.querySelector("svg");
        if (svgEl) {
            svgEl.style.maxWidth = "100%";
            svgEl.style.height   = "auto";
        }

    } catch (err) {
        container.innerHTML =
            `<div style="color:#f87171;text-align:center;padding:32px;font-size:14px;">
                ⚠️ Could not render diagram<br/>
                <small style="color:#94a3b8;">${err.message}</small>
            </div>`;
        console.error("Mermaid error:", err);
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

/* ══════════════════════════════════════════════
   RENDER RESULTS — original logic preserved
══════════════════════════════════════════════ */
function render(data) {

    latestData = data;

    // Stack badge (two places)
    const stackText = data.stack || "Detected Stack";
    document.getElementById("stackBadge").textContent  = stackText;
    document.getElementById("stackSummary").textContent = stackText;

    // Security
    document.getElementById("security").textContent =
        (data.securityStatus || "") + " (" + (data.securityScore || 0) + "/100)";

    // Code blocks
    setText("readme",          data.readme);
    setText("dockerfile",      data.dockerfile);
    setText("compose",         data.compose);
    setText("env",             data.env);
    setText("githubActions",   data.githubActions);
    setText("deploySteps",     data.deploySteps);
    setText("repoExplanation", data.repoExplanation);

    // Show results section
    const section = document.getElementById("results");
    section.classList.remove("hidden");

    // Open first accordion by default
    const first = document.querySelector(".accordion");
    if (first && !first.classList.contains("open")) {
        first.classList.add("open");
    }

    // Smooth scroll
    section.scrollIntoView({ behavior: "smooth", block: "start" });
}

function setText(id, value) {
    const el = document.getElementById(id);
    if (el) el.textContent = value || "";
}

/* ══════════════════════════════════════════════
   LOADING STATE
══════════════════════════════════════════════ */
function setLoading(button, loading) {
    if (!button) return;
    const text    = button.querySelector(".btn-text");
    const spinner = button.querySelector(".spinner");

    button.disabled = loading;
    if (text)    text.classList.toggle("hidden", loading);
    if (spinner) spinner.classList.toggle("hidden", !loading);
}

/* ══════════════════════════════════════════════
   THEME
══════════════════════════════════════════════ */
function toggleTheme() {
    document.body.classList.toggle("light");
    const mode = document.body.classList.contains("light") ? "light" : "dark";
    localStorage.setItem("theme", mode);
    syncThemeIcons();
}

function loadTheme() {
    if (localStorage.getItem("theme") === "light") {
        document.body.classList.add("light");
    }
}

function syncThemeIcons() {
    const isLight = document.body.classList.contains("light");
    const icon = isLight ? "☀️" : "🌙";
    const a = document.getElementById("themeIcon");
    const b = document.getElementById("themeIconMobile");
    if (a) a.textContent = icon;
    if (b) b.textContent = icon;
}

/* ══════════════════════════════════════════════
   COPY CODE
══════════════════════════════════════════════ */
function copyCode(id) {
    const el = document.getElementById(id);
    if (!el) return;
    navigator.clipboard.writeText(el.textContent).then(() => {
        showToast("✅ Copied to clipboard");
    });
}

/* ══════════════════════════════════════════════
   DOWNLOADS — original logic preserved exactly
══════════════════════════════════════════════ */
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
    if (!svg) { showToast("No diagram to download"); return; }
    const blob = new Blob([svg.outerHTML], { type: "image/svg+xml" });
    const a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = "architecture-diagram.svg";
    a.click();
}

function downloadZip() {
    let content = "";
    content += "README.md\n\n"            + (latestData.readme          || "") + "\n\n";
    content += "Dockerfile\n\n"           + (latestData.dockerfile       || "") + "\n\n";
    content += "docker-compose.yml\n\n"   + (latestData.compose          || "") + "\n\n";
    content += ".env.example\n\n"         + (latestData.env              || "") + "\n\n";
    content += "deploy.yml\n\n"           + (latestData.githubActions    || "") + "\n\n";
    content += "repo-explanation.md\n\n"  + (latestData.repoExplanation  || "") + "\n\n";
    const blob = new Blob([content], { type: "text/plain" });
    const a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = "devops-assets.txt";
    a.click();
}

/* ══════════════════════════════════════════════
   FILE LABEL UPDATE
══════════════════════════════════════════════ */
function updateFileLabel(input) {
    const label = document.querySelector(".file-label-text");
    if (label && input.files[0]) {
        label.textContent = "📁 " + input.files[0].name;
    }
}

/* ══════════════════════════════════════════════
   TOAST NOTIFICATION
══════════════════════════════════════════════ */
function showToast(msg) {
    const existing = document.querySelector(".toast");
    if (existing) existing.remove();

    const toast = document.createElement("div");
    toast.className = "toast";
    toast.textContent = msg;
    document.body.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = "0";
        toast.style.transition = "opacity 0.4s";
        setTimeout(() => toast.remove(), 400);
    }, 2800);
}