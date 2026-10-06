package com.vitor.melembre.localweb

object LocalAccessPage {
    const val HTML: String = """
<!DOCTYPE html>
<html lang="pt-BR">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Lembre-me</title>
<style>
  * { box-sizing: border-box; }
  body {
    margin: 0;
    font-family: "Segoe UI", system-ui, sans-serif;
    background: #F7F6FB;
    color: #242329;
  }
  main { max-width: 680px; margin: 0 auto; padding: 28px 16px 48px; }
  h1 { font-size: 28px; margin: 0 0 16px; font-weight: 600; }
  h2 { font-size: 13px; margin: 20px 0 6px; color: #6B6975; font-weight: 600; }
  button, input { font: inherit; }
  .primary {
    background: #6C3CE9;
    color: #fff;
    border: 0;
    border-radius: 8px;
    padding: 8px 14px;
    cursor: pointer;
  }
  .primary:hover { background: #4D23B8; }
  .ghost, .danger {
    background: transparent;
    border: 0;
    padding: 8px 10px;
    cursor: pointer;
  }
  .ghost { color: #4D23B8; }
  .danger { color: #8a3048; }
  .card { background: #fff; border-radius: 12px; padding: 16px; }
  label { display: block; font-size: 13px; color: #6B6975; margin: 10px 0 4px; }
  input[type="text"], input[type="password"], input[type="date"], input[type="time"] {
    width: 100%;
    padding: 8px 10px;
    border: 1px solid #dddbe6;
    border-radius: 8px;
    background: #fff;
  }
  .pin-box { max-width: 320px; }
  .pin-input { letter-spacing: 4px; font-size: 20px; }
  #new-btn { margin-bottom: 8px; }
  .row {
    display: flex;
    justify-content: space-between;
    gap: 16px;
    align-items: baseline;
    padding: 9px 2px;
    border-bottom: 1px solid #eceaf3;
    cursor: pointer;
  }
  .row:hover { background: #f3f0fb; }
  .msg { min-width: 0; overflow-wrap: anywhere; }
  .when { font-size: 13px; color: #6C3CE9; white-space: nowrap; }
  .done .msg, .done .when { color: #8a8794; text-decoration: line-through; }
  .undated .when { color: #6B6975; text-decoration: none; }
  #filters { display: flex; gap: 6px; overflow-x: auto; margin: 0 0 8px; }
  .chip { border: 0; background: transparent; color: #6B6975; padding: 4px 8px; cursor: pointer; white-space: nowrap; font-size: 13px; }
  .chip.on { color: #6C3CE9; font-weight: 600; }
  select { width: 100%; padding: 8px 10px; border: 1px solid #dddbe6; border-radius: 8px; background: #fff; }
  .check { display: flex; align-items: center; gap: 8px; margin-top: 12px; color: #242329; font-size: 14px; }
  .actions { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 14px; }
  .error { color: #8a3048; font-size: 14px; min-height: 1.2em; }
  .hint { color: #6B6975; font-size: 14px; }
  #editor { margin-top: 16px; }
  #app { display: none; }
</style>
</head>
<body>
<main>
  <h1>Lembre-me</h1>
  <section id="pin" class="card pin-box">
    <p class="hint">Digite o PIN mostrado no celular.</p>
    <label for="pin-input">PIN</label>
    <input id="pin-input" class="pin-input" type="password" inputmode="numeric" maxlength="4" autocomplete="off">
    <div class="actions">
      <button class="primary" id="pin-ok" type="button">Entrar</button>
    </div>
    <p class="error" id="pin-error"></p>
  </section>
  <section id="app">
    <button class="primary" id="new-btn" type="button">+ Novo lembrete</button>
    <div id="filters"></div>
    <p class="hint" id="empty" style="display:none"></p>
    <div id="active"></div>
    <h2 id="done-title" style="display:none">Concluídos</h2>
    <div id="done"></div>
    <section id="editor" class="card" style="display:none">
      <label for="message">Mensagem</label>
      <input id="message" type="text" autocomplete="off">
      <label for="list">Lista</label>
      <select id="list"></select>
      <label class="check" for="no-deadline"><input id="no-deadline" type="checkbox"> Sem prazo</label>
      <div id="deadline-fields">
      <label for="date">Data</label>
      <input id="date" type="date">
      <label for="time">Hora</label>
      <input id="time" type="time">
      </div>
      <p class="error" id="form-error"></p>
      <div class="actions">
        <button class="primary" id="save-btn" type="button">Salvar</button>
        <button class="ghost" id="snooze-btn" type="button">Adiar 1h</button>
        <button class="danger" id="delete-btn" type="button">Excluir</button>
        <button class="ghost" id="cancel-btn" type="button">Cancelar</button>
      </div>
    </section>
  </section>
</main>
<script>
(function () {
  var token = "";
  var editingId = null;
  var lists = [];
  var filterId = "";
  var choseFilter = false;
  var pinPanel = document.getElementById("pin");
  var appPanel = document.getElementById("app");
  var editor = document.getElementById("editor");
  var snoozeBtn = document.getElementById("snooze-btn");
  var deleteBtn = document.getElementById("delete-btn");

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

  function renderList(items, container, completed) {
    container.textContent = "";
    items.forEach(function (item) {
      var row = document.createElement("div");
      row.className = completed ? "row done" : "row";
      if (!completed && !item.hasDeadline) row.className += " undated";
      var msg = document.createElement("div");
      msg.className = "msg";
      msg.textContent = item.message;
      if (!completed) msg.style.color = colorFor(item.listId);
      var when = document.createElement("div");
      when.className = "when";
      when.textContent = item.schedule;
      row.appendChild(msg);
      row.appendChild(when);
      row.onclick = function () { openEditor(item); };
      container.appendChild(row);
    });
  }

  function renderFilters() {
    var bar = document.getElementById("filters");
    bar.textContent = "";
    function addChip(label, id) {
      var chip = document.createElement("button");
      chip.type = "button";
      chip.className = filterId === id ? "chip on" : "chip";
      chip.textContent = label;
      if (id) chip.style.color = colorFor(id);
      if (filterId === id && id) {
        chip.style.background = colorFor(id);
        chip.style.color = "#fff";
        chip.style.borderRadius = "12px";
      }
      chip.onclick = function () {
        filterId = id;
        loadList();
      };
      bar.appendChild(chip);
    }
    lists.forEach(function (list) {
      addChip(list.name, String(list.id));
    });
    addChip("Todas", "");
  }

  function colorFor(listId) {
    var found = null;
    lists.forEach(function (list) {
      if (String(list.id) === String(listId)) found = list;
    });
    return found && found.color ? found.color : "#242329";
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

  function toggleDeadline() {
    var hidden = document.getElementById("no-deadline").checked;
    document.getElementById("deadline-fields").style.display = hidden ? "none" : "block";
  }

  function loadList() {
    var path = filterId ? "/api/reminders?listId=" + encodeURIComponent(filterId) : "/api/reminders";
    request("GET", path).then(function (res) {
      if (res.status === 401) {
        token = "";
        appPanel.style.display = "none";
        pinPanel.style.display = "block";
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
      document.getElementById("done-title").style.display = done.length ? "block" : "none";
      var empty = document.getElementById("empty");
      if (active.length === 0 && done.length === 0) {
        empty.style.display = "block";
        empty.textContent = filterId ? "Nenhuma tarefa nesta lista." : "Nenhum lembrete por enquanto.";
      } else {
        empty.style.display = "none";
      }
    });
  }

  function openEditor(item) {
    editor.style.display = "block";
    editingId = item ? item.id : null;
    document.getElementById("message").value = item ? item.message : "";
    fillListSelect(item ? item.listId : defaultListId());
    var noDeadline = document.getElementById("no-deadline");
    noDeadline.checked = item ? !item.hasDeadline : true;
    toggleDeadline();
    document.getElementById("date").value = item && item.date ? item.date : "";
    document.getElementById("time").value = item && item.time ? item.time : "";
    document.getElementById("form-error").textContent = "";
    snoozeBtn.style.display = item && item.hasDeadline ? "inline-block" : "none";
    deleteBtn.style.display = item ? "inline-block" : "none";
    if (!item && !noDeadline.checked) {
      fillNow();
    }
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
    toggleDeadline();
    if (!document.getElementById("no-deadline").checked && !document.getElementById("date").value) {
      fillNow();
    }
  };

  document.getElementById("new-btn").onclick = function () { openEditor(null); };
  document.getElementById("cancel-btn").onclick = function () {
    editor.style.display = "none";
    editingId = null;
  };

  function payload() {
    var noDeadline = document.getElementById("no-deadline").checked;
    return {
      message: document.getElementById("message").value,
      listId: Number(document.getElementById("list").value),
      date: noDeadline ? "" : document.getElementById("date").value,
      time: noDeadline ? "" : document.getElementById("time").value
    };
  }

  function showFormError(res) {
    document.getElementById("form-error").textContent =
      (res.data && res.data.error) ? res.data.error : "Não foi possível salvar.";
  }

  document.getElementById("save-btn").onclick = function () {
    var path = editingId ? "/api/reminders/" + editingId : "/api/reminders";
    var method = editingId ? "PUT" : "POST";
    request(method, path, payload()).then(function (res) {
      if (res.status >= 200 && res.status < 300) {
        editor.style.display = "none";
        editingId = null;
        loadList();
      } else {
        showFormError(res);
      }
    });
  };

  document.getElementById("snooze-btn").onclick = function () {
    if (!editingId) return;
    request("POST", "/api/reminders/" + editingId + "/snooze").then(function (res) {
      if (res.status >= 200 && res.status < 300) {
        editor.style.display = "none";
        editingId = null;
        loadList();
      } else {
        showFormError(res);
      }
    });
  };

  document.getElementById("delete-btn").onclick = function () {
    if (!editingId) return;
    request("DELETE", "/api/reminders/" + editingId).then(function (res) {
      if (res.status >= 200 && res.status < 300) {
        editor.style.display = "none";
        editingId = null;
        loadList();
      } else {
        showFormError(res);
      }
    });
  };
})();
</script>
</body>
</html>
"""
}
