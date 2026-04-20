document.addEventListener("DOMContentLoaded",()=>{

const themeBtn=document.getElementById("themeToggle");
const saved=localStorage.getItem("theme")||"light";
document.documentElement.setAttribute("data-theme",saved);
themeBtn.textContent=saved==="dark"?"☀️":"🌙";

themeBtn.onclick=()=>{
const current=document.documentElement.getAttribute("data-theme");
const next=current==="dark"?"light":"dark";
document.documentElement.setAttribute("data-theme",next);
localStorage.setItem("theme",next);
themeBtn.textContent=next==="dark"?"☀️":"🌙";
};

document.querySelectorAll(".tab").forEach(tab=>{
tab.onclick=()=>{
document.querySelectorAll(".tab").forEach(t=>t.classList.remove("active"));
document.querySelectorAll(".tab-content").forEach(c=>c.classList.remove("active"));
tab.classList.add("active");
document.getElementById("tab-"+tab.dataset.tab).classList.add("active");
};
});

const dropZone=document.getElementById("dropZone");
const fileInput=document.getElementById("fileInput");
const uploadBtn=document.getElementById("uploadBtn");

dropZone.onclick=()=>fileInput.click();

fileInput.onchange=()=>{
if(fileInput.files.length){
document.getElementById("fileName").textContent=fileInput.files[0].name;
uploadBtn.disabled=false;
}
};

document.getElementById("repoForm").onsubmit=repoSubmit;
document.getElementById("uploadForm").onsubmit=uploadSubmit;

document.querySelectorAll(".copy-btn").forEach(btn=>{
btn.onclick=()=>copyText(btn.dataset.target);
});

});

async function repoSubmit(e){
e.preventDefault();
setLoading("repoBtn",true);

const url=document.getElementById("repoUrl").value;

try{
const res=await fetch("/api/analyze/repo",{
method:"POST",
headers:{"Content-Type":"application/json"},
body:JSON.stringify({repoUrl:url})
});
const data=await res.json();
if(!res.ok) throw new Error(data.error||"Failed");
render(data);
toast("Analysis complete 🚀");
}catch(err){
showError(err.message);
}

setLoading("repoBtn",false);
}

async function uploadSubmit(e){
e.preventDefault();
setLoading("uploadBtn",true);

const fd=new FormData();
fd.append("file",document.getElementById("fileInput").files[0]);

try{
const res=await fetch("/api/analyze/upload",{method:"POST",body:fd});
const data=await res.json();
if(!res.ok) throw new Error(data.error||"Failed");
render(data);
toast("Upload analyzed 🚀");
}catch(err){
showError(err.message);
}

setLoading("uploadBtn",false);
}

function render(data){
document.getElementById("results").classList.remove("hidden");
document.getElementById("stackBadge").textContent=data.stack||"Unknown";
document.getElementById("dockerfile").textContent=data.dockerfile||"";
document.getElementById("compose").textContent=data.compose||"";
document.getElementById("env").textContent=data.env||"";
document.getElementById("githubActions").textContent=data.githubActions||"";
document.getElementById("deploySteps").textContent=data.deploySteps||"";

const ul=document.getElementById("recommendations");
ul.innerHTML="";
(data.recommendations||[]).forEach(r=>{
const li=document.createElement("li");
li.textContent=r;
ul.appendChild(li);
});

document.getElementById("results").scrollIntoView({behavior:"smooth"});
}

function setLoading(id,state){
const btn=document.getElementById(id);
btn.disabled=state;
btn.querySelector(".btn-text").classList.toggle("hidden",state);
btn.querySelector(".spinner").classList.toggle("hidden",!state);
}

function copyText(id){
navigator.clipboard.writeText(document.getElementById(id).textContent);
toast("Copied");
}

function downloadGeneratedFile(name,id){
const content=document.getElementById(id).textContent;
const blob=new Blob([content],{type:"text/plain"});
const a=document.createElement("a");
a.href=URL.createObjectURL(blob);
a.download=name;
a.click();
}

async function downloadAllZip(){
const zip=new JSZip();

zip.file("Dockerfile",document.getElementById("dockerfile").textContent);
zip.file("docker-compose.yml",document.getElementById("compose").textContent);
zip.file(".env.example",document.getElementById("env").textContent);
zip.file("workflow.yml",document.getElementById("githubActions").textContent);
zip.file("deployment-steps.txt",document.getElementById("deploySteps").textContent);

const blob=await zip.generateAsync({type:"blob"});
const a=document.createElement("a");
a.href=URL.createObjectURL(blob);
a.download="devops-assets.zip";
a.click();

toast("ZIP downloaded 📦");
}

function showError(msg){
const el=document.getElementById("errorMsg");
el.textContent="❌ "+msg;
el.classList.remove("hidden");
}

function toast(msg){
const t=document.getElementById("toast");
t.textContent=msg;
t.classList.remove("hidden");
setTimeout(()=>t.classList.add("hidden"),2500);
}