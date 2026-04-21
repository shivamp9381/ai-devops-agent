let latestData = {};

document.addEventListener("DOMContentLoaded", () => {
    initTabs();
    initRepoForm();
    initUploadForm();
    loadTheme();
});

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

function initRepoForm() {

    document.getElementById("repoForm")
        .addEventListener("submit", async e => {

            e.preventDefault();

            const btn =
                document.getElementById("repoBtn");

            setLoading(btn, true);

            try {

                const repoUrl =
                    document.getElementById("repoUrl").value;

                const res = await fetch(
                    "/api/analyze/repo",
                    {
                        method: "POST",
                        headers: {
                            "Content-Type": "application/json"
                        },
                        body: JSON.stringify({
                            repoUrl: repoUrl
                        })
                    }
                );

                const data = await res.json();

                render(data);

            } catch (e) {
                alert("Analysis failed");
            }

            setLoading(btn, false);
        });
}

function initUploadForm() {

    document.getElementById("uploadForm")
        .addEventListener("submit", async e => {

            e.preventDefault();

            const btn =
                document.getElementById("uploadBtn");

            setLoading(btn, true);

            try {

                const file =
                    document.getElementById("zipFile")
                        .files[0];

                const fd = new FormData();

                fd.append("file", file);

                const res = await fetch(
                    "/api/analyze/upload",
                    {
                        method: "POST",
                        body: fd
                    }
                );

                const data = await res.json();

                render(data);

            } catch (e) {
                alert("Upload failed");
            }

            setLoading(btn, false);
        });
}

async function debugError() {

    const error =
        document.getElementById("errorText").value;

    const res = await fetch("/api/debug", {
        method: "POST",
        headers: {
            "Content-Type":"application/json"
        },
        body: JSON.stringify({
            error:error
        })
    });

    const txt = await res.text();

    document.getElementById("debugOutput")
        .textContent = txt;
}

function setLoading(button, loading) {

    const text =
        button.querySelector(".btn-text");

    const spinner =
        button.querySelector(".spinner");

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

function render(data) {

    latestData = data;

    document.getElementById("results")
        .classList.remove("hidden");

    document.getElementById("stackBadge")
        .textContent = data.stack || "Detected Stack";

    document.getElementById("security")
        .textContent =
        (data.securityStatus || "") +
        " (" +
        (data.securityScore || 0) +
        "/100)";

    setText("readme", data.readme);
    setText("dockerfile", data.dockerfile);
    setText("compose", data.compose);
    setText("env", data.env);
    setText("githubActions", data.githubActions);
    setText("deploySteps", data.deploySteps);

    window.scrollTo({
        top:
            document.getElementById("results")
                .offsetTop - 20,
        behavior: "smooth"
    });
}

function setText(id, value) {

    document.getElementById(id)
        .textContent = value || "";
}

/* ----------------------------
   Theme
---------------------------- */

function toggleTheme() {

    document.body.classList.toggle("light");

    const mode =
        document.body.classList.contains("light")
        ? "light"
        : "dark";

    localStorage.setItem("theme", mode);
}

function loadTheme() {

    const mode =
        localStorage.getItem("theme");

    if (mode === "light") {
        document.body.classList.add("light");
    }
}

/* ----------------------------
   Downloads
---------------------------- */

function downloadSingle(fileName, id) {

    const text =
        document.getElementById(id)
            .textContent;

    const blob =
        new Blob([text], {
            type: "text/plain"
        });

    const a =
        document.createElement("a");

    a.href = URL.createObjectURL(blob);
    a.download = fileName;
    a.click();
}

function downloadZip() {

    let content = "";

    content += "README.md\n\n";
    content += latestData.readme + "\n\n";

    content += "Dockerfile\n\n";
    content += latestData.dockerfile + "\n\n";

    content += "docker-compose.yml\n\n";
    content += latestData.compose + "\n\n";

    content += ".env.example\n\n";
    content += latestData.env + "\n\n";

    content += "deploy.yml\n\n";
    content += latestData.githubActions + "\n\n";

    const blob =
        new Blob([content], {
            type: "text/plain"
        });

    const a =
        document.createElement("a");

    a.href = URL.createObjectURL(blob);
    a.download = "devops-assets.txt";
    a.click();
}