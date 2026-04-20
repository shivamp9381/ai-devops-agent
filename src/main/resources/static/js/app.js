document.addEventListener("DOMContentLoaded", () => {

    initTheme();
    initTabs();
    initRepoForm();
    initUploadForm();
    initCopyButtons();

});


// ---------------- THEME ----------------
function initTheme() {
    const btn = document.getElementById("themeToggle");

    const saved = localStorage.getItem("theme") || "light";
    document.documentElement.setAttribute("data-theme", saved);
    btn.textContent = saved === "dark" ? "☀️" : "🌙";

    btn.onclick = () => {
        const current = document.documentElement.getAttribute("data-theme");
        const next = current === "dark" ? "light" : "dark";

        document.documentElement.setAttribute("data-theme", next);
        localStorage.setItem("theme", next);
        btn.textContent = next === "dark" ? "☀️" : "🌙";
    };
}


// ---------------- TABS ----------------
function initTabs() {
    document.querySelectorAll(".tab").forEach(tab => {
        tab.onclick = () => {

            document.querySelectorAll(".tab").forEach(t =>
                t.classList.remove("active"));

            document.querySelectorAll(".tab-content").forEach(c =>
                c.classList.remove("active"));

            tab.classList.add("active");

            document
                .getElementById("tab-" + tab.dataset.tab)
                .classList.add("active");
        };
    });
}


// ---------------- REPO ----------------
function initRepoForm() {

    document.getElementById("repoForm").onsubmit = async (e) => {
        e.preventDefault();

        const url = document.getElementById("repoUrl").value.trim();

        if (!url) return showError("Please enter repository URL");

        setLoading("repoBtn", true);

        try {

            const res = await fetch("/api/analyze/repo", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ repoUrl: url })
            });

            const data = await res.json();

            if (!res.ok) throw new Error(data.error || "Failed");

            render(data);
            toast("Repository analyzed 🚀");

        } catch (err) {
            showError(err.message);
        }

        setLoading("repoBtn", false);
    };
}


// ---------------- ZIP UPLOAD ----------------
function initUploadForm() {

    const dropZone = document.getElementById("dropZone");
    const fileInput = document.getElementById("fileInput");
    const uploadBtn = document.getElementById("uploadBtn");

    dropZone.onclick = () => fileInput.click();

    dropZone.addEventListener("dragover", e => {
        e.preventDefault();
        dropZone.classList.add("dragging");
    });

    dropZone.addEventListener("dragleave", () => {
        dropZone.classList.remove("dragging");
    });

    dropZone.addEventListener("drop", e => {
        e.preventDefault();

        dropZone.classList.remove("dragging");

        const file = e.dataTransfer.files[0];
        if (file) {
            fileInput.files = e.dataTransfer.files;
            updateSelectedFile(file);
        }
    });

    fileInput.onchange = () => {
        if (fileInput.files.length) {
            updateSelectedFile(fileInput.files[0]);
        }
    };

    function updateSelectedFile(file) {

        if (!file.name.toLowerCase().endsWith(".zip")) {
            showError("Only ZIP files are supported");
            fileInput.value = "";
            return;
        }

        document.getElementById("fileName").textContent =
            "Selected: " + file.name;

        uploadBtn.disabled = false;
    }

    document.getElementById("uploadForm").onsubmit = async (e) => {

        e.preventDefault();

        if (!fileInput.files.length) {
            return showError("Please choose ZIP file");
        }

        setLoading("uploadBtn", true);

        try {

            const fd = new FormData();
            fd.append("file", fileInput.files[0]);

            const res = await fetch("/api/analyze/upload", {
                method: "POST",
                body: fd
            });

            const data = await res.json();

            if (!res.ok) throw new Error(data.error || "Upload failed");

            render(data);
            toast("ZIP analyzed successfully 📦");

        } catch (err) {
            showError(err.message);
        }

        setLoading("uploadBtn", false);
    };
}


// ---------------- RENDER ----------------
function render(data) {

    document.getElementById("results").classList.remove("hidden");

    document.getElementById("stackBadge").textContent = data.stack || "";

    document.getElementById("dockerfile").textContent = data.dockerfile || "";
    document.getElementById("compose").textContent = data.compose || "";
    document.getElementById("env").textContent = data.env || "";
    document.getElementById("githubActions").textContent = data.githubActions || "";
    document.getElementById("deploySteps").textContent = data.deploySteps || "";

    const ul = document.getElementById("recommendations");
    ul.innerHTML = "";

    (data.recommendations || []).forEach(item => {
        const li = document.createElement("li");
        li.textContent = item;
        ul.appendChild(li);
    });

    document.getElementById("results")
        .scrollIntoView({ behavior: "smooth" });
}


// ---------------- COPY ----------------
function initCopyButtons() {
    document.querySelectorAll(".copy-btn").forEach(btn => {
        btn.onclick = () => copyText(btn.dataset.target);
    });
}

function copyText(id) {
    navigator.clipboard.writeText(
        document.getElementById(id).textContent
    );
    toast("Copied");
}


// ---------------- DOWNLOAD ----------------
function downloadGeneratedFile(name, id) {

    const text = document.getElementById(id).textContent;

    const blob = new Blob([text], { type: "text/plain" });

    const a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = name;
    a.click();
}

async function downloadAllZip() {

    const zip = new JSZip();

    zip.file("Dockerfile", getText("dockerfile"));
    zip.file("docker-compose.yml", getText("compose"));
    zip.file(".env.example", getText("env"));
    zip.file("workflow.yml", getText("githubActions"));
    zip.file("deployment-steps.txt", getText("deploySteps"));

    const blob = await zip.generateAsync({ type: "blob" });

    const a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = "devops-assets.zip";
    a.click();

    toast("ZIP downloaded 📦");
}

function getText(id) {
    return document.getElementById(id).textContent;
}


// ---------------- UI HELPERS ----------------
function setLoading(id, state) {

    const btn = document.getElementById(id);

    btn.disabled = state;

    btn.querySelector(".btn-text")
        .classList.toggle("hidden", state);

    btn.querySelector(".spinner")
        .classList.toggle("hidden", !state);
}

function showError(msg) {
    const el = document.getElementById("errorMsg");

    el.textContent = "❌ " + msg;
    el.classList.remove("hidden");

    setTimeout(() => {
        el.classList.add("hidden");
    }, 4000);
}

function toast(msg) {

    const t = document.getElementById("toast");

    t.textContent = msg;
    t.classList.remove("hidden");

    setTimeout(() => {
        t.classList.add("hidden");
    }, 2500);
}