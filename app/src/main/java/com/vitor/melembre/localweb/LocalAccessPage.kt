package com.vitor.melembre.localweb

object LocalAccessPage {
    const val HTML: String = """
<!DOCTYPE html>
<html lang="pt-BR">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Lembre-me</title>
<link rel="icon" type="image/svg+xml" href="data:image/svg+xml,%3Csvg%20xmlns='http://www.w3.org/2000/svg'%20viewBox='0%200%20108%20108'%3E%3Crect%20width='108'%20height='108'%20rx='24'%20fill='%236C3CE9'/%3E%3Cg%20transform='translate(54%2054)%20scale(0.95)%20translate(-54%20-54)'%20fill='%23fff'%3E%3Cpath%20d='M54,30c-9.4,0%20-17,7.6%20-17,17v8.2c0,2.4%20-1,4.7%20-2.7,6.3l-4.2,4c-1.5,1.4%20-0.5,3.9%201.5,3.9h45.8c2,0%203,-2.5%201.5,-3.9l-4.2,-4c-1.7,-1.6%20-2.7,-3.9%20-2.7,-6.3V47c0,-9.4%20-7.6,-17%20-17,-17z'/%3E%3Cpath%20d='M46,73h16c0,4.4%20-3.6,8%20-8,8s-8,-3.6%20-8,-8z'/%3E%3Cpath%20d='M70,38%20L73.5,41.5%20L65,50%20L59.5,44.5%20L63,41%20L65,43z'/%3E%3C/g%3E%3C/svg%3E">
<script>
(function () {
  try {
    if (localStorage.getItem("melembre-theme") === "dark") document.documentElement.setAttribute("data-theme", "dark");
  } catch (e) {}
})();
</script>
<style>
  :root {
    --bg: #E9E7F8; --bg2: #F4F2FC; --text: #17142B; --muted: #6B6882; --card: #fff; --soft: #F6F5FC;
    --border: #DAD7EC; --line: #ECEAF5; --hover: #F1EEFC; --accent: #5B35E5; --accent-dark: #4723C4;
    --accent-soft: #EDE8FD; --danger: #D3263F; --done: #8E8BA3; --undated: #A9780B;
    --overlay: rgba(23, 20, 43, 0.45); --tip-bg: #EFEBFD; --tip-text: #5B35E5;
  }
  :root[data-theme="dark"] {
    --bg: #17161B; --bg2: #1E1D24; --text: #ECEAF4; --muted: #A19EB3; --card: #26252D; --soft: #2D2C36;
    --border: #44424F; --line: #34323D; --hover: #31303A; --accent: #8A64F5; --accent-dark: #A183F8;
    --accent-soft: #3A3258; --danger: #F2869B; --done: #7E7B8E; --undated: #F1DF9A;
    --overlay: rgba(0, 0, 0, 0.62); --tip-bg: #322D4A; --tip-text: #C3B2FA;
  }
  * { box-sizing: border-box; }
  body {
    margin: 0; min-height: 100vh;
    font-family: "Segoe UI", system-ui, sans-serif;
    background: linear-gradient(160deg, var(--bg) 0%, var(--bg2) 100%) fixed;
    color: var(--text);
  }
  main { max-width: 760px; margin: 0 auto; padding: 28px 18px 56px; }
  svg.i { width: 18px; height: 18px; fill: none; stroke: currentColor; stroke-width: 1.9; stroke-linecap: round; stroke-linejoin: round; flex: none; }
  button, input, select { font: inherit; color: inherit; }
  button { cursor: pointer; }
  .top { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 18px; }
  .brand { display: flex; align-items: center; gap: 14px; }
  .logo { width: 52px; height: 52px; border-radius: 14px; background: var(--accent); color: #fff; display: grid; place-items: center; }
  .logo svg.i { width: 26px; height: 26px; fill: #fff; stroke: #fff; }
  h1 { font-size: 28px; line-height: 1.1; margin: 0; font-weight: 700; }
  .sub { color: var(--muted); font-size: 15px; margin-top: 2px; }
  .seg { display: inline-flex; background: var(--card); border: 1px solid var(--border); border-radius: 12px; padding: 4px; gap: 2px; }
  .seg button { border: 0; background: transparent; color: var(--muted); padding: 7px 12px; border-radius: 9px; display: flex; align-items: center; gap: 6px; font-size: 14px; }
  .seg button.on { background: var(--accent); color: #fff; }
  .primary {
    background: var(--accent); color: #fff; border: 0; border-radius: 10px; padding: 10px 18px;
    display: inline-flex; align-items: center; gap: 8px; font-weight: 600;
  }
  .primary:hover { background: var(--accent-dark); }
  .outline { background: var(--card); border: 1px solid var(--border); border-radius: 10px; padding: 10px 16px; display: inline-flex; align-items: center; gap: 8px; }
  .outline:hover { background: var(--hover); }
  .link { background: transparent; border: 0; color: var(--accent); padding: 10px 6px; font-weight: 500; }
  .dangerbtn { background: transparent; border: 0; color: var(--danger); padding: 10px 10px; display: inline-flex; align-items: center; gap: 6px; font-weight: 500; }
  .card { background: var(--card); border-radius: 16px; padding: 18px; }
  .bar { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 14px; flex-wrap: wrap; }
  .toggle { display: inline-flex; align-items: center; gap: 10px; font-size: 14px; color: var(--text); cursor: pointer; }
  .toggle input { display: none; }
  .sw { width: 44px; height: 24px; border-radius: 12px; background: var(--border); position: relative; transition: background .15s; }
  .sw::after { content: ""; position: absolute; top: 3px; left: 3px; width: 18px; height: 18px; border-radius: 50%; background: #fff; transition: left .15s; }
  .toggle input:checked + .sw { background: var(--accent); }
  .toggle input:checked + .sw::after { left: 23px; }
  #filters { display: flex; gap: 8px; overflow-x: auto; margin-bottom: 14px; padding-bottom: 2px; }
  .chip { border: 1px solid var(--border); background: var(--card); color: var(--text); padding: 7px 14px; white-space: nowrap; font-size: 14px; border-radius: 10px; display: inline-flex; align-items: center; gap: 8px; }
  .chip.on { background: var(--accent); border-color: var(--accent); color: #fff; }
  .dot { width: 11px; height: 11px; border-radius: 50%; display: inline-block; flex: none; }
  .chip.on .dot { box-shadow: 0 0 0 2px #fff; }
  .list { background: var(--card); border-radius: 16px; overflow: hidden; }
  .row { display: flex; align-items: center; gap: 12px; padding: 12px 14px; border-bottom: 1px solid var(--line); cursor: pointer; }
  .row:last-child { border-bottom: 0; }
  .row:hover { background: var(--hover); }
  .grip { cursor: grab; color: var(--muted); opacity: .45; display: grid; place-items: center; width: 18px; flex: none; margin-right: -6px; }
  .grip:hover { opacity: 1; }
  .grip svg.i { width: 14px; height: 14px; fill: currentColor; stroke: none; }
  .row.dragging { opacity: .35; }
  .circle { width: 30px; height: 30px; border-radius: 50%; border: 2px solid var(--border); background: transparent; display: grid; place-items: center; color: #fff; padding: 0; flex: none; }
  .circle:hover { border-color: var(--accent); }
  .done .circle { background: var(--accent); border-color: var(--accent); }
  .circle svg.i { width: 16px; height: 16px; stroke-width: 3; }
  .main { min-width: 0; flex: 1; }
  .msg { font-weight: 600; font-size: 17px; overflow-wrap: anywhere; }
  .meta { display: flex; align-items: center; gap: 6px; color: var(--muted); font-size: 14px; margin-top: 2px; }
  .meta svg.i { width: 15px; height: 15px; }
  .meta.dated { color: var(--accent); font-weight: 500; }
  .undated .msg { color: var(--undated) !important; }
  .done .msg { color: var(--done) !important; text-decoration: line-through; font-weight: 500; }
  .done .meta { color: var(--done); }
  .acts { display: flex; align-items: center; gap: 4px; flex: none; }
  .ic { background: transparent; border: 0; color: var(--muted); padding: 6px 8px; border-radius: 8px; display: inline-flex; align-items: center; gap: 5px; font-size: 14px; }
  .ic:hover { background: var(--accent-soft); color: var(--accent); }
  .ic.del { color: var(--danger); }
  .ic.del:hover { background: var(--accent-soft); color: var(--danger); }
  .ic.hide { visibility: hidden; }
  .sect { display: flex; align-items: center; gap: 8px; margin: 22px 4px 8px; font-weight: 600; font-size: 16px; background: transparent; border: 0; color: var(--text); padding: 0; }
  .sect svg.i { transition: transform .15s; }
  .sect.closed svg.i { transform: rotate(-90deg); }
  .hint { color: var(--muted); font-size: 14px; }
  .pin-box { max-width: 340px; }
  .pin-input { letter-spacing: 4px; font-size: 20px; }
  label { display: block; font-size: 14px; color: var(--muted); margin: 14px 0 6px; }
  input[type="text"], input[type="password"], input[type="date"], input[type="time"], select {
    width: 100%; padding: 11px 12px; border: 1px solid var(--border); border-radius: 10px;
    background: var(--card); color: var(--text); color-scheme: light;
  }
  :root[data-theme="dark"] input, :root[data-theme="dark"] select { color-scheme: dark; }
  input:disabled { background: var(--soft); color: var(--muted); }
  .error { color: var(--danger); font-size: 14px; min-height: 1.2em; margin: 8px 0 0; }
  #app { display: none; }
  #overlay { display: none; position: fixed; inset: 0; background: var(--overlay); align-items: center; justify-content: center; padding: 16px; z-index: 10; }
  #editor { width: 100%; max-width: 560px; max-height: 94vh; overflow-y: auto; padding: 24px; border-radius: 18px; box-shadow: 0 18px 50px rgba(0,0,0,.3); }
  .mhead { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; }
  .mhead h3 { margin: 0; font-size: 24px; line-height: 1.2; font-weight: 700; overflow-wrap: anywhere; }
  .close { background: transparent; border: 0; color: var(--muted); padding: 4px; border-radius: 8px; }
  .close:hover { background: var(--hover); }
  .chips { display: flex; gap: 10px; flex-wrap: wrap; margin-top: 12px; }
  .pill { display: inline-flex; align-items: center; gap: 8px; padding: 7px 14px; border-radius: 10px; background: var(--soft); border: 1px solid var(--line); font-size: 14px; }
  .selwrap { position: relative; }
  .selwrap .dot { position: absolute; left: 12px; top: 50%; transform: translateY(-50%); pointer-events: none; }
  .selwrap select { padding-left: 34px; }
  .check { display: flex; align-items: center; gap: 10px; margin: 18px 0 0; font-size: 16px; color: var(--text); cursor: pointer; }
  .check input { width: 20px; height: 20px; accent-color: var(--accent); }
  .help { margin: 2px 0 0 30px; font-size: 13px; color: var(--muted); }
  .grid2 { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
  .btns { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-top: 22px; }
  .after { margin-top: 20px; padding-top: 16px; border-top: 1px solid var(--line); }
  .after .t { font-size: 14px; color: var(--muted); margin-bottom: 8px; }
  .prev { display: flex; align-items: center; gap: 12px; background: var(--soft); border: 1px solid var(--line); border-radius: 12px; padding: 12px 14px; }
  .prev .circle { background: var(--accent); border-color: var(--accent); cursor: default; }
  .prev .pm { color: var(--done); text-decoration: line-through; overflow-wrap: anywhere; }
  .prev .meta { color: var(--done); }
  .tip { margin-top: 14px; background: var(--tip-bg); color: var(--tip-text); border-radius: 10px; padding: 11px 14px; font-size: 13px; display: flex; gap: 10px; align-items: center; }
  .frow { display: grid; grid-template-columns: 1fr 108px 108px auto auto; gap: 8px; align-items: center; margin-top: 10px; }
  .frow input[type="text"], .frow input[type="time"] { padding: 8px 10px; }
  .f-rest { display: flex; align-items: center; gap: 6px; margin: 0; font-size: 13px; white-space: nowrap; }
  .f-rest input { width: auto; }
  #focus-overlay { display: none; position: fixed; inset: 0; background: var(--overlay); align-items: center; justify-content: center; padding: 16px; z-index: 11; }
  #focus-editor { width: 100%; max-width: 760px; max-height: 94vh; overflow-y: auto; padding: 24px; border-radius: 18px; box-shadow: 0 18px 50px rgba(0,0,0,.3); }
  @media (max-width: 720px) { .frow { grid-template-columns: 1fr 1fr; } .frow .f-name { grid-column: 1 / -1; } }
  @media (max-width: 520px) { .acts .lbl { display: none; } .grid2 { grid-template-columns: 1fr; } }
</style>
</head>
<body>
<main>
  <div class="top">
    <div class="brand">
      <div class="logo" id="logo"></div>
      <div>
        <h1>Lembre-me</h1>
        <div class="sub">Acesso pelo computador</div>
      </div>
    </div>
    <div class="seg" id="theme-seg">
      <button type="button" id="th-light"></button>
      <button type="button" id="th-dark"></button>
    </div>
  </div>
  <section id="pin" class="card pin-box">
    <p class="hint">Digite o PIN mostrado no celular.</p>
    <label for="pin-input">PIN</label>
    <input id="pin-input" class="pin-input" type="password" inputmode="numeric" maxlength="4" autocomplete="off">
    <div class="btns">
      <button class="primary" id="pin-ok" type="button">Entrar</button>
    </div>
    <p class="error" id="pin-error"></p>
  </section>
  <section id="app">
    <div class="bar">
      <div style="display:flex;gap:8px;align-items:center;flex-wrap:wrap">
        <button class="primary" id="new-btn" type="button"></button>
        <button class="outline" id="focus-btn" type="button"></button>
      </div>
      <label class="toggle" style="margin:0">Mostrar concluídas
        <input type="checkbox" id="show-done" checked><span class="sw"></span>
      </label>
    </div>
    <div id="filters"></div>
    <p class="hint" id="empty" style="display:none"></p>
    <div class="list" id="active" style="display:none"></div>
    <button type="button" class="sect" id="done-title" style="display:none"></button>
    <div class="list" id="done" style="display:none"></div>
  </section>
</main>
<div id="overlay">
  <section id="editor" class="card" role="dialog" aria-modal="true">
    <div class="mhead">
      <h3 id="modal-title">Editar tarefa</h3>
      <button class="close" id="close-btn" type="button" aria-label="Fechar"></button>
    </div>
    <div class="chips">
      <span class="pill" id="pill-list"></span>
      <span class="pill" id="pill-when"></span>
    </div>
    <label for="message">Mensagem</label>
    <input id="message" type="text" autocomplete="off">
    <label for="list">Lista</label>
    <div class="selwrap"><span class="dot" id="list-dot"></span><select id="list"></select></div>
    <label for="url">URL (opcional)</label>
    <input id="url" type="text" inputmode="url" autocomplete="off" placeholder="https://...">
    <label class="check" for="no-deadline"><input id="no-deadline" type="checkbox"> Sem prazo</label>
    <div class="help">A tarefa não terá uma data definida.</div>
    <div class="grid2">
      <div><label for="date">Data</label><input id="date" type="date"></div>
      <div><label for="time">Horário</label><input id="time" type="time"></div>
    </div>
    <p class="error" id="form-error"></p>
    <div class="btns">
      <button class="primary" id="save-btn" type="button"></button>
      <button class="outline" id="complete-btn" type="button"></button>
      <button class="outline" id="snooze-btn" type="button"></button>
      <button class="dangerbtn" id="delete-btn" type="button"></button>
      <button class="link" id="cancel-btn" type="button">Cancelar</button>
    </div>
    <div class="tip" id="tip"></div>
  </section>
</div>
<div id="focus-overlay">
  <section id="focus-editor" class="card" role="dialog" aria-modal="true">
    <div class="mhead">
      <h3>Agenda do modo foco</h3>
      <button class="close" id="focus-close" type="button" aria-label="Fechar"></button>
    </div>
    <p class="hint" style="margin:8px 0 0">Dentro de cada bloco: 25 min de foco e 5 de pausa. Marque Intervalo para blocos sem ciclos, como o almoço. A tela de foco no celular usa esta agenda.</p>
    <div id="focus-rows"></div>
    <p class="error" id="focus-error"></p>
    <div class="btns">
      <button class="outline" id="focus-add" type="button"></button>
      <button class="primary" id="focus-save" type="button"></button>
      <button class="link" id="focus-cancel" type="button">Cancelar</button>
    </div>
  </section>
</div>
<script>
(function () {
  var ICONS = {
    bell: '<path d="M6 8a6 6 0 0 1 12 0c0 7 3 8 3 8H3s3-1 3-8"/><path d="M10.3 21a1.9 1.9 0 0 0 3.4 0"/>',
    plus: '<path d="M12 5v14M5 12h14"/>',
    sun: '<circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/>',
    moon: '<path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"/>',
    cal: '<rect x="3.5" y="5" width="17" height="15.5" rx="2.5"/><path d="M3.5 10h17M8 3v4M16 3v4"/>',
    clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
    edit: '<path d="M4 20l4.5-1 10-10a2.1 2.1 0 0 0-3-3l-10 10z"/><path d="M13.5 7.5l3 3"/>',
    trash: '<path d="M4 7h16M9 7V4h6v3M6.5 7l1 13h9l1-13M10 11v6M14 11v6"/>',
    check: '<path d="M5 12.5l4.5 4.5L19 7.5"/>',
    undo: '<path d="M9 14L4 9l5-5"/><path d="M4 9h10a6 6 0 0 1 0 12h-3"/>',
    x: '<path d="M6 6l12 12M18 6L6 18"/>',
    down: '<path d="M6 9l6 6 6-6"/>',
    grip: '<circle cx="9" cy="6" r="1.6"/><circle cx="15" cy="6" r="1.6"/><circle cx="9" cy="12" r="1.6"/><circle cx="15" cy="12" r="1.6"/><circle cx="9" cy="18" r="1.6"/><circle cx="15" cy="18" r="1.6"/>',
    target: '<circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="4.5"/><circle cx="12" cy="12" r="0.6"/>',
    link: '<path d="M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1"/><path d="M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1"/>',
    info: '<circle cx="12" cy="12" r="9"/><path d="M12 11v5M12 8h.01"/>'
  };
  function icon(name) {
    return '<svg class="i" viewBox="0 0 24 24" aria-hidden="true">' + ICONS[name] + '</svg>';
  }
  function put(id, name, text) {
    document.getElementById(id).innerHTML = (name ? icon(name) : "") + (text ? "<span>" + text + "</span>" : "");
  }

  var token = "";
  var editingId = null;
  var editingItem = null;
  var snapshot = "";
  var lists = [];
  var filterId = "";
  var choseFilter = false;
  var doneOpen = true;
  var pinPanel = document.getElementById("pin");
  var appPanel = document.getElementById("app");
  var overlay = document.getElementById("overlay");
  var snoozeBtn = document.getElementById("snooze-btn");
  var deleteBtn = document.getElementById("delete-btn");
  var completeBtn = document.getElementById("complete-btn");
  var showDone = document.getElementById("show-done");

  document.getElementById("logo").innerHTML = icon("bell");
  put("new-btn", "plus", "Nova tarefa");
  put("focus-btn", "target", "Agenda de foco");
  put("focus-close", "x", "");
  put("focus-add", "plus", "Adicionar bloco");
  put("focus-save", "check", "Salvar agenda");
  put("th-light", "sun", "Light");
  put("th-dark", "moon", "Dark");
  put("close-btn", "x", "");
  put("save-btn", "check", "Salvar");
  put("snooze-btn", "clock", "Adiar 1h");
  put("delete-btn", "trash", "Excluir");
  document.getElementById("tip").innerHTML = icon("info") +
    "<span>Dica: o editor abre em uma janela para você não precisar rolar a página.</span>";
  try { showDone.checked = localStorage.getItem("melembre-showdone") !== "0"; } catch (e) {}

  function isDark() { return document.documentElement.getAttribute("data-theme") === "dark"; }
  function syncTheme() {
    document.getElementById("th-light").className = isDark() ? "" : "on";
    document.getElementById("th-dark").className = isDark() ? "on" : "";
  }
  function setTheme(dark) {
    if (dark) document.documentElement.setAttribute("data-theme", "dark");
    else document.documentElement.removeAttribute("data-theme");
    try { localStorage.setItem("melembre-theme", dark ? "dark" : "light"); } catch (e) {}
    syncTheme();
    if (appPanel.style.display === "block") loadList();
  }
  document.getElementById("th-light").onclick = function () { setTheme(false); };
  document.getElementById("th-dark").onclick = function () { setTheme(true); };
  syncTheme();

  showDone.onchange = function () {
    try { localStorage.setItem("melembre-showdone", showDone.checked ? "1" : "0"); } catch (e) {}
    applyDoneVisibility();
  };

  function showApp() {
    pinPanel.style.display = "none";
    appPanel.style.display = "block";
    loadList();
  }

  function request(method, path, payload) {
    var headers = { "Accept": "application/json" };
    if (token) headers["X-Local-Token"] = token;
    var opts = { method: method, headers: headers };
    if (payload) {
      headers["Content-Type"] = "application/json; charset=utf-8";
      opts.body = JSON.stringify(payload);
    }
    return fetch(path, opts).then(function (res) {
      return res.text().then(function (text) {
        var data = {};
        if (text) {
          try { data = JSON.parse(text); } catch (e) { data = {}; }
        }
        return { status: res.status, data: data };
      });
    });
  }

  document.getElementById("pin-ok").onclick = function () {
    var value = document.getElementById("pin-input").value;
    request("POST", "/api/session", { pin: value }).then(function (res) {
      if (res.status === 200 && res.data.token) {
        token = res.data.token;
        document.getElementById("pin-error").textContent = "";
        showApp();
      } else {
        document.getElementById("pin-error").textContent = "PIN incorreto.";
      }
    });
  };

  document.getElementById("pin-input").addEventListener("keydown", function (event) {
    if (event.key === "Enter") document.getElementById("pin-ok").click();
  });

  function ok(res) { return res.status >= 200 && res.status < 300; }

  function toggleDone(item) {
    var action = item.completed ? "/reopen" : "/complete";
    request("POST", "/api/reminders/" + item.id + action).then(function (res) {
      if (ok(res)) loadList();
      else alert((res.data && res.data.error) ? res.data.error : "Não foi possível atualizar.");
    });
  }

  function listById(id) {
    var found = null;
    lists.forEach(function (list) {
      if (String(list.id) === String(id)) found = list;
    });
    return found;
  }

  function colorFor(listId) {
    var found = listById(listId);
    if (!found || !found.color) return "";
    return isDark() ? "color-mix(in srgb, " + found.color + " 55%, #ffffff)" : found.color;
  }

  function mkBtn(cls, html, title, handler) {
    var b = document.createElement("button");
    b.type = "button";
    b.className = cls;
    b.innerHTML = html;
    b.title = title;
    b.onclick = function (event) { event.stopPropagation(); handler(); };
    return b;
  }

  var dragEl = null;

  function enableDrag(row) {
    row.addEventListener("dragstart", function (event) {
      if (!row.draggable) { event.preventDefault(); return; }
      dragEl = row;
      row.classList.add("dragging");
      event.dataTransfer.effectAllowed = "move";
      event.dataTransfer.setData("text/plain", row.dataset.id);
    });
    row.addEventListener("dragover", function (event) {
      if (!dragEl || dragEl === row) return;
      event.preventDefault();
      var box = row.getBoundingClientRect();
      var after = event.clientY > box.top + box.height / 2;
      row.parentNode.insertBefore(dragEl, after ? row.nextSibling : row);
    });
    row.addEventListener("dragend", function () {
      row.draggable = false;
      row.classList.remove("dragging");
      if (!dragEl) return;
      dragEl = null;
      var ids = Array.prototype.map.call(document.querySelectorAll("#active .row.undated"), function (r) { return r.dataset.id; });
      request("POST", "/api/reminders/reorder", { listId: Number(filterId), ids: ids.join(",") }).then(loadList);
    });
  }

  function renderList(items, container, completed) {
    container.textContent = "";
    container.style.display = items.length ? "block" : "none";
    items.forEach(function (item) {
      var row = document.createElement("div");
      row.className = completed ? "row done" : "row";
      if (!completed && !item.hasDeadline) row.className += " undated";

      var circle = mkBtn("circle", completed ? icon("check") : "", completed ? "Reabrir" : "Concluir", function () { toggleDone(item); });

      var main = document.createElement("div");
      main.className = "main";
      var msg = document.createElement("div");
      msg.className = "msg";
      msg.textContent = item.message;
      if (!completed && item.hasDeadline) msg.style.color = colorFor(item.listId);
      var meta = document.createElement("div");
      meta.className = completed || !item.hasDeadline ? "meta" : "meta dated";
      meta.innerHTML = icon("cal");
      var when = document.createElement("span");
      when.textContent = item.schedule;
      meta.appendChild(when);
      main.appendChild(msg);
      main.appendChild(meta);

      var acts = document.createElement("div");
      acts.className = "acts";
      var snooze = mkBtn("ic", icon("clock") + '<span class="lbl">+1h</span>', "Adiar 1h", function () {
        request("POST", "/api/reminders/" + item.id + "/snooze").then(function (res) {
          if (ok(res)) loadList();
          else alert((res.data && res.data.error) ? res.data.error : "Não foi possível adiar.");
        });
      });
      if (!item.hasDeadline || completed) snooze.className += " hide";
      if (item.url) {
        acts.appendChild(mkBtn("ic", icon("link"), item.url, function () {
          window.open(item.url, "_blank", "noopener,noreferrer");
        }));
      }
      acts.appendChild(snooze);
      acts.appendChild(mkBtn("ic", icon("edit"), "Editar", function () { openEditor(item); }));
      acts.appendChild(mkBtn("ic del", icon("trash"), "Excluir", function () {
        if (!confirm("Excluir esta tarefa?")) return;
        request("DELETE", "/api/reminders/" + item.id).then(function (res) {
          if (ok(res)) loadList();
        });
      }));

      row.dataset.id = String(item.id);
      if (!completed && !item.hasDeadline && filterId) {
        var grip = document.createElement("span");
        grip.className = "grip";
        grip.title = "Arrastar para reordenar";
        grip.innerHTML = icon("grip");
        grip.onmousedown = function () { row.draggable = true; };
        grip.onmouseup = function () { row.draggable = false; };
        grip.onclick = function (event) { event.stopPropagation(); };
        row.appendChild(grip);
        enableDrag(row);
      }
      row.appendChild(circle);
      row.appendChild(main);
      row.appendChild(acts);
      row.onclick = function () { openEditor(item); };
      container.appendChild(row);
    });
  }

  function renderFilters() {
    var bar = document.getElementById("filters");
    bar.textContent = "";
    function addChip(label, id, color) {
      var chip = document.createElement("button");
      chip.type = "button";
      chip.className = filterId === id ? "chip on" : "chip";
      if (color) {
        var dot = document.createElement("span");
        dot.className = "dot";
        dot.style.background = color;
        chip.appendChild(dot);
      }
      var text = document.createElement("span");
      text.textContent = label;
      chip.appendChild(text);
      chip.onclick = function () {
        filterId = id;
        loadList();
      };
      bar.appendChild(chip);
    }
    addChip("Todas", "", "");
    lists.forEach(function (list) {
      addChip(list.name, String(list.id), list.color || "#6C3CE9");
    });
  }

  function fillListSelect(selectedId) {
    var select = document.getElementById("list");
    select.textContent = "";
    lists.forEach(function (list) {
      var option = document.createElement("option");
      option.value = String(list.id);
      option.textContent = list.name;
      select.appendChild(option);
    });
    if (selectedId) select.value = String(selectedId);
  }

  function defaultListId() {
    if (filterId) return filterId;
    var reminders = lists.filter(function (list) { return list.name === "Lembretes"; })[0];
    return reminders ? String(reminders.id) : (lists[0] ? String(lists[0].id) : "");
  }

  function applyDoneVisibility() {
    var title = document.getElementById("done-title");
    var done = document.getElementById("done");
    var any = done.children.length > 0;
    title.style.display = any && showDone.checked ? "flex" : "none";
    done.style.display = any && showDone.checked && doneOpen ? "block" : "none";
    title.className = doneOpen ? "sect" : "sect closed";
  }

  document.getElementById("done-title").onclick = function () {
    doneOpen = !doneOpen;
    applyDoneVisibility();
  };

  function loadList() {
    var path = filterId ? "/api/reminders?listId=" + encodeURIComponent(filterId) : "/api/reminders";
    request("GET", path).then(function (res) {
      if (res.status === 401) {
        token = "";
        appPanel.style.display = "none";
        pinPanel.style.display = "block";
        overlay.style.display = "none";
        return;
      }
      lists = res.data.lists || [];
      if (!choseFilter) {
        choseFilter = true;
        var remindersList = null;
        lists.forEach(function (list) {
          if (list.name === "Lembretes") remindersList = list;
        });
        if (remindersList) {
          filterId = String(remindersList.id);
          loadList();
          return;
        }
      }
      renderFilters();
      var all = res.data.reminders || [];
      var active = [];
      var done = [];
      all.forEach(function (item) {
        if (item.completed) done.push(item); else active.push(item);
      });
      renderList(active, document.getElementById("active"), false);
      renderList(done, document.getElementById("done"), true);
      document.getElementById("done-title").innerHTML =
        icon("down") + "<span>Concluídas (" + done.length + ")</span>";
      applyDoneVisibility();
      var empty = document.getElementById("empty");
      if (active.length === 0 && done.length === 0) {
        empty.style.display = "block";
        empty.textContent = filterId ? "Nenhuma tarefa nesta lista." : "Nenhuma tarefa por enquanto.";
      } else {
        empty.style.display = "none";
      }
    });
  }

  function currentWhen() {
    if (document.getElementById("no-deadline").checked) return "Sem prazo";
    var d = document.getElementById("date").value;
    var t = document.getElementById("time").value;
    if (!d) return "Sem prazo";
    var p = d.split("-");
    return p[2] + "/" + p[1] + "/" + p[0] + (t ? " " + t : "");
  }

  function refreshSummary() {
    var list = listById(document.getElementById("list").value);
    var color = list && list.color ? list.color : "#6C3CE9";
    document.getElementById("list-dot").style.background = color;
    document.getElementById("pill-list").innerHTML =
      '<span class="dot" style="background:' + color + '"></span><span></span>';
    document.getElementById("pill-list").lastChild.textContent = list ? list.name : "";
    document.getElementById("pill-when").innerHTML = icon("cal") + "<span></span>";
    document.getElementById("pill-when").lastChild.textContent = currentWhen();
  }

  function syncDeadlineFields() {
    var none = document.getElementById("no-deadline").checked;
    document.getElementById("date").disabled = none;
    document.getElementById("time").disabled = none;
  }

  function openEditor(item) {
    overlay.style.display = "flex";
    editingId = item ? item.id : null;
    editingItem = item;
    var title = item ? item.message : "Nova tarefa";
    document.getElementById("modal-title").textContent = title;
    document.getElementById("message").value = item ? item.message : "";
    document.getElementById("url").value = item && item.url ? item.url : "";
    fillListSelect(item ? item.listId : defaultListId());
    var noDeadline = document.getElementById("no-deadline");
    noDeadline.checked = item ? !item.hasDeadline : true;
    document.getElementById("date").value = item && item.date ? item.date : "";
    document.getElementById("time").value = item && item.time ? item.time : "";
    syncDeadlineFields();
    document.getElementById("form-error").textContent = "";
    snoozeBtn.style.display = item && item.hasDeadline && !item.completed ? "inline-flex" : "none";
    completeBtn.style.display = item ? "inline-flex" : "none";
    if (item && item.completed) put("complete-btn", "undo", "Reabrir");
    else put("complete-btn", "check", "Concluir");
    deleteBtn.style.display = item ? "inline-flex" : "none";
    refreshSummary();
    snapshot = JSON.stringify(payload());
    document.getElementById("message").focus();
  }

  function closeEditor(force) {
    if (!force && JSON.stringify(payload()) !== snapshot &&
        !confirm("Descartar as alterações não salvas?")) {
      return;
    }
    overlay.style.display = "none";
    editingId = null;
    editingItem = null;
  }

  function fillNow() {
    var now = new Date();
    var month = String(now.getMonth() + 1).padStart(2, "0");
    var day = String(now.getDate()).padStart(2, "0");
    document.getElementById("date").value = now.getFullYear() + "-" + month + "-" + day;
    var hour = String(now.getHours()).padStart(2, "0");
    var minute = String(now.getMinutes()).padStart(2, "0");
    document.getElementById("time").value = hour + ":" + minute;
  }

  document.getElementById("no-deadline").onchange = function () {
    syncDeadlineFields();
    if (!document.getElementById("no-deadline").checked && !document.getElementById("date").value) {
      fillNow();
    }
    refreshSummary();
  };
  ["message", "list", "date", "time"].forEach(function (id) {
    document.getElementById(id).addEventListener("input", refreshSummary);
    document.getElementById(id).addEventListener("change", refreshSummary);
  });

  var focusOverlay = document.getElementById("focus-overlay");
  var focusRows = document.getElementById("focus-rows");

  function addFocusRow(block) {
    var row = document.createElement("div");
    row.className = "frow";
    row.innerHTML = '<input type="text" class="f-name" placeholder="Nome do bloco" maxlength="40" autocomplete="off">' +
      '<input type="time" class="f-start"><input type="time" class="f-end">' +
      '<label class="f-rest"><input type="checkbox" class="f-rest-box"> Intervalo</label>';
    row.querySelector(".f-name").value = block.name;
    row.querySelector(".f-start").value = block.start;
    row.querySelector(".f-end").value = block.end;
    row.querySelector(".f-rest-box").checked = block.rest;
    row.appendChild(mkBtn("ic del", icon("trash"), "Remover bloco", function () { focusRows.removeChild(row); }));
    focusRows.appendChild(row);
  }

  function openFocus() {
    document.getElementById("focus-error").textContent = "";
    focusRows.textContent = "";
    request("GET", "/api/focus").then(function (res) {
      var text = (res.data && res.data.agenda) || "";
      text.split("\n").forEach(function (line) {
        if (!line.trim()) return;
        var p = line.split("|");
        addFocusRow({ start: p[0], end: p[1], rest: p[2] === "1", name: p.slice(3).join("|") });
      });
      focusOverlay.style.display = "flex";
    });
  }

  function closeFocus() { focusOverlay.style.display = "none"; }

  document.getElementById("focus-btn").onclick = openFocus;
  document.getElementById("focus-close").onclick = closeFocus;
  document.getElementById("focus-cancel").onclick = closeFocus;
  document.getElementById("focus-add").onclick = function () {
    var rows = focusRows.querySelectorAll(".f-end");
    var last = rows.length ? rows[rows.length - 1].value : "08:00";
    addFocusRow({ name: "", start: last || "08:00", end: "", rest: false });
  };
  document.getElementById("focus-save").onclick = function () {
    var lines = [];
    var failed = "";
    Array.prototype.forEach.call(focusRows.querySelectorAll(".frow"), function (row) {
      var name = row.querySelector(".f-name").value.replace(/[|\n\r]/g, " ").trim();
      var start = row.querySelector(".f-start").value;
      var end = row.querySelector(".f-end").value;
      if (!start || !end) failed = "Preencha o início e o fim de todos os blocos.";
      lines.push(start + "|" + end + "|" + (row.querySelector(".f-rest-box").checked ? "1" : "0") + "|" + name);
    });
    if (failed) { document.getElementById("focus-error").textContent = failed; return; }
    request("POST", "/api/focus", { agenda: lines.join("\n") }).then(function (res) {
      if (ok(res)) closeFocus();
      else document.getElementById("focus-error").textContent = (res.data && res.data.error) ? res.data.error : "Não foi possível salvar a agenda.";
    });
  };
  focusOverlay.addEventListener("mousedown", function (event) {
    if (event.target === focusOverlay) closeFocus();
  });

  document.getElementById("new-btn").onclick = function () { openEditor(null); };
  document.getElementById("cancel-btn").onclick = function () { closeEditor(false); };
  document.getElementById("close-btn").onclick = function () { closeEditor(false); };
  overlay.addEventListener("mousedown", function (event) {
    if (event.target === overlay) closeEditor(false);
  });
  document.addEventListener("keydown", function (event) {
    if (event.key === "Escape" && focusOverlay.style.display === "flex") closeFocus();
    else if (event.key === "Escape" && overlay.style.display === "flex") closeEditor(false);
  });

  function payload() {
    var noDeadline = document.getElementById("no-deadline").checked;
    return {
      message: document.getElementById("message").value,
      listId: Number(document.getElementById("list").value),
      url: document.getElementById("url").value.trim(),
      date: noDeadline ? "" : document.getElementById("date").value,
      time: noDeadline ? "" : document.getElementById("time").value
    };
  }

  function showFormError(res) {
    document.getElementById("form-error").textContent =
      (res.data && res.data.error) ? res.data.error : "Não foi possível salvar.";
  }

  function finish(res) {
    if (ok(res)) {
      closeEditor(true);
      loadList();
    } else {
      showFormError(res);
    }
  }

  document.getElementById("save-btn").onclick = function () {
    var path = editingId ? "/api/reminders/" + editingId : "/api/reminders";
    var method = editingId ? "PUT" : "POST";
    request(method, path, payload()).then(finish);
  };

  completeBtn.onclick = function () {
    if (!editingId) return;
    var dirty = JSON.stringify(payload()) !== snapshot;
    var action = editingItem && editingItem.completed ? "/reopen" : "/complete";
    var run = function () { request("POST", "/api/reminders/" + editingId + action).then(finish); };
    if (!dirty) { run(); return; }
    request("PUT", "/api/reminders/" + editingId, payload()).then(function (res) {
      if (!ok(res)) { showFormError(res); return; }
      if (action === "/reopen") finish(res); else run();
    });
  };

  snoozeBtn.onclick = function () {
    if (!editingId) return;
    request("POST", "/api/reminders/" + editingId + "/snooze").then(finish);
  };

  deleteBtn.onclick = function () {
    if (!editingId) return;
    request("DELETE", "/api/reminders/" + editingId).then(finish);
  };
})();
</script>
</body>
</html>
"""
}
