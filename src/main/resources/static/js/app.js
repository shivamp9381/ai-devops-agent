document.addEventListener('DOMContentLoaded', () => {

    // Theme toggle
    const themeBtn = document.getElementById('themeToggle');
    const saved = localStorage.getItem('theme') || 'light';
    document.documentElement.setAttribute('data-theme', saved);
    themeBtn.textContent = saved === 'dark' ? '☀️' : '🌙';

    themeBtn.addEventListener('click', () => {
        const current = document.documentElement.getAttribute('data-theme');
        const next = current === 'dark' ? 'light' : 'dark';
        document.documentElement.setAttribute('data-theme', next);
        localStorage.setItem('theme', next);
        themeBtn.textContent = next === 'dark' ? '☀️' : '🌙';
    });

    // Tabs
    document.querySelectorAll('.tab').forEach(tab => {
        tab.addEventListener('click', () => {
            document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
            document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
            tab.classList.add('active');
            document.getElementById('tab-' + tab.dataset.tab).classList.add('active');
        });
    });

    // Drop zone
    const dropZone = document.getElementById('dropZone');
    const fileInput = document.getElementById('fileInput');
    const fileName = document.getElementById('fileName');
    const uploadBtn = document.getElementById('uploadBtn');

    dropZone.addEventListener('click', () => fileInput.click());
    dropZone.addEventListener('dragover', e => { e.preventDefault(); dropZone.classList.add('dragover'); });
    dropZone.addEventListener('dragleave', () => dropZone.classList.remove('dragover'));
    dropZone.addEventListener('drop', e => {
        e.preventDefault();
        dropZone.classList.remove('dragover');
        if (e.dataTransfer.files.length) {
            fileInput.files = e.dataTransfer.files;
            handleFileSelect();
        }
    });
    fileInput.addEventListener('change', handleFileSelect);

    function handleFileSelect() {
        if (fileInput.files.length) {
            fileName.textContent = fileInput.files[0].name;
            uploadBtn.disabled = false;
        }
    }

    // Repo form
    document.getElementById('repoForm').addEventListener('submit', async e => {
        e.preventDefault();
        const url = document.getElementById('repoUrl').value.trim();
        if (!url) return;

        setLoading('repoBtn', true);
        hideError();
        hideResults();

        try {
            const res = await fetch('/api/analyze/repo', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ repoUrl: url })
            });
            const data = await res.json();
            if (!res.ok) throw new Error(data.error || 'Analysis failed');
            showResults(data);
            showToast('Analysis complete!');
        } catch (err) {
            showError(err.message);
        } finally {
            setLoading('repoBtn', false);
        }
    });

    // Upload form
    document.getElementById('uploadForm').addEventListener('submit', async e => {
        e.preventDefault();
        if (!fileInput.files.length) return;

        setLoading('uploadBtn', true);
        hideError();
        hideResults();

        const formData = new FormData();
        formData.append('file', fileInput.files[0]);

        try {
            const res = await fetch('/api/analyze/upload', {
                method: 'POST',
                body: formData
            });
            const data = await res.json();
            if (!res.ok) throw new Error(data.error || 'Analysis failed');
            showResults(data);
            showToast('Analysis complete!');
        } catch (err) {
            showError(err.message);
        } finally {
            setLoading('uploadBtn', false);
        }
    });

    function setLoading(btnId, loading) {
        const btn = document.getElementById(btnId);
        btn.disabled = loading;
        btn.querySelector('.btn-text').classList.toggle('hidden', loading);
        btn.querySelector('.spinner').classList.toggle('hidden', !loading);
    }

    function showResults(data) {
        document.getElementById('results').classList.remove('hidden');
        document.getElementById('stackBadge').textContent = data.stack || 'Unknown';
        document.getElementById('dockerfile').textContent = data.dockerfile || 'Not generated';
        document.getElementById('compose').textContent = data.compose || 'Not generated';
        document.getElementById('env').textContent = data.env || 'Not generated';
        document.getElementById('githubActions').textContent = data.githubActions || 'Not generated';
        document.getElementById('deploySteps').textContent = data.deploySteps || 'Not generated';

        const recList = document.getElementById('recommendations');
        recList.innerHTML = '';
        (data.recommendations || []).forEach(r => {
            const li = document.createElement('li');
            li.textContent = r;
            recList.appendChild(li);
        });

        document.getElementById('results').scrollIntoView({ behavior: 'smooth', block: 'start' });
    }

    function hideResults() {
        document.getElementById('results').classList.add('hidden');
    }

    function showError(msg) {
        const el = document.getElementById('errorMsg');
        el.textContent = '❌ ' + msg;
        el.classList.remove('hidden');
    }

    function hideError() {
        document.getElementById('errorMsg').classList.add('hidden');
    }

    function showToast(msg) {
        const toast = document.getElementById('toast');
        toast.textContent = '✅ ' + msg;
        toast.classList.remove('hidden');
        setTimeout(() => toast.classList.add('hidden'), 3000);
    }

    // Copy buttons
    document.querySelectorAll('.copy-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const target = document.getElementById(btn.dataset.target);
            const text = target.textContent;
            navigator.clipboard.writeText(text).then(() => {
                const orig = btn.textContent;
                btn.textContent = '✅ Copied!';
                setTimeout(() => btn.textContent = orig, 2000);
            });
        });
    });
});
