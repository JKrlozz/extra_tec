const app = document.querySelector("#app");

const state = {
  route: "splash",
  role: "alumno",
  enrolled: ["Banda de guerra"],
  activities: ["Fútbol", "Ajedrez", "Voleibol"],
  teachers: ["Carlos Pérez", "Ana López", "Luis Gómez"],
  students: ["María Hernández", "José Martínez", "Diana Sánchez", "Pedro García"],
  reportReady: false,
};

const homes = {
  alumno: "student-home",
  docente: "teacher-home",
  admin: "admin-home",
};

const nav = {
  alumno: [
    ["home", "Inicio", "student-home"],
    ["sports_soccer", "Actividades", "student-activities"],
    ["calendar_month", "Horario", "student-schedule"],
    ["workspace_premium", "Constancia", "certificate"],
  ],
  docente: [
    ["home", "Inicio", "teacher-home"],
    ["checklist", "Lista", "attendance"],
    ["add_a_photo", "Evidencia", "evidence"],
  ],
  admin: [
    ["home", "Inicio", "admin-home"],
    ["event_note", "Actividades", "manage-activities"],
    ["groups", "Personas", "manage-teachers"],
    ["description", "Reportes", "reports"],
  ],
};

const icons = {
  home: "⌂",
  sports_soccer: "●",
  calendar_month: "▣",
  workspace_premium: "★",
  checklist: "✓",
  add_a_photo: "▧",
  event_note: "□",
  groups: "●",
  description: "≡",
  logout: "↪",
  arrow_back: "←",
  chevron_right: "›",
  download: "↓",
  person: "●",
  edit: "✎",
  delete: "×",
  search: "⌕",
  add: "+",
  school: "■",
  bar_chart: "▥",
  picture_as_pdf: "PDF",
};

const icon = (name) => `<span class="icon" aria-hidden="true">${icons[name] || "•"}</span>`;
const button = (label, action, style = "") =>
  `<button type="${action === "submit" ? "submit" : "button"}" class="button ${style}" data-action="${action}">${label}</button>`;
const field = (label, type = "text", placeholder = "") =>
  `<label class="field"><span>${label}</span><input type="${type}" placeholder="${placeholder || label}" /></label>`;
const select = (label, values, id = "") =>
  `<label class="field"><span>${label}</span><select ${id ? `id="${id}"` : ""}>${values.map((value) => `<option value="${value}">${value}</option>`).join("")}</select></label>`;

function shell(title, content, role = "", back = "") {
  const bottom = role
    ? `<nav class="nav">${nav[role].map(([symbol, label, route]) =>
        `<button class="${state.route === route ? "active" : ""}" data-route="${route}">${icon(symbol)}<span>${label}</span></button>`
      ).join("")}</nav>`
    : "";

  return `<section class="phone">
    <div class="status"><span>9:41</span><span>● ◔ ▰</span></div>
    <header class="bar">
      <button class="icon-button" data-route="${back || (role ? homes[role] : "login")}" aria-label="Regresar">${icon(back ? "arrow_back" : "home")}</button>
      <strong>${title}</strong>
      <button class="icon-button" data-route="login" aria-label="Cerrar sesión">${icon("logout")}</button>
    </header>
    <div class="content">${content}</div>
    ${bottom}
  </section>`;
}

function shortcut(symbol, label, route) {
  return `<button class="shortcut" data-route="${route}">${icon(symbol)}<span>${label}</span>${icon("chevron_right")}</button>`;
}

function activityCard(name, schedule, detail, actions = "") {
  return `<article class="card"><h3>${icon("sports_soccer")} ${name}</h3><p><b>Horario:</b> ${schedule}</p><p>${detail}</p>${actions ? `<div class="actions">${actions}</div>` : ""}</article>`;
}

function personCard(name, detail, type = "") {
  const controls = type
    ? `<button class="icon-button" data-route="${type}-form" aria-label="Editar">${icon("edit")}</button>
       <button class="icon-button" data-delete="${type}" data-name="${name}" aria-label="Eliminar">${icon("delete")}</button>`
    : "";
  return `<article class="card person">${icon("person")}<div><h3>${name}</h3><p>${detail}</p></div>${controls}</article>`;
}

function formPage(title, fields, back, saveAction) {
  return shell(title, `<form class="form" data-submit="${saveAction}">${fields}${button("Guardar", "submit")}${button("Cancelar", `route:${back}`, "secondary")}</form>`, "", back);
}

function render() {
  let html = "";

  switch (state.route) {
    case "splash":
      html = `<section class="phone splash"><div class="logo">ITSM</div><h1>Actividades Extraescolares</h1><p>Instituto Tecnológico Superior de Misantla</p>${button("Entrar", "route:login")}</section>`;
      break;
    case "login":
      html = `<section class="phone"><div class="login-head"><div class="login-logo">ITSM</div><h1>Iniciar sesión</h1></div><form class="content form" data-submit="login">${field("Correo", "email", "correo@itsm.edu.mx")}${field("Contraseña", "password", "••••••••")}${select("Tipo de usuario", ["alumno", "docente", "admin"], "role")}${button("Iniciar sesión", "submit")}<button class="button text" data-route="register">¿No tienes cuenta? Regístrate</button></form></section>`;
      break;
    case "register":
      html = formPage("Registro de alumno", `${field("Nombre completo")}${field("Correo", "email")}${field("Contraseña", "password")}${select("Carrera", ["Ing. en Sistemas", "Ing. Industrial", "Ing. en Gestión"])}${select("Semestre", ["1°", "3°", "5°", "7°"])}${field("Matrícula")}`, "login", "register");
      break;
    case "student-home":
      html = shell("Inicio", `<div class="welcome"><p>Hola,</p><h2>María Hernández</h2></div><h3 class="title">Accesos rápidos</h3>${shortcut("sports_soccer", "Inscribirme a una actividad", "student-activities")}${shortcut("calendar_month", "Ver mi horario", "student-schedule")}${shortcut("workspace_premium", "Consultar constancia", "certificate")}`, "alumno");
      break;
    case "student-activities":
      html = shell("Actividades", [
        ["Fútbol", "Lun. y Mié. 16:00", "Prof. Carlos Pérez · Cupo 18/25"],
        ["Ajedrez", "Martes 15:00", "Prof. Ana López · Cupo 12/20"],
        ["Voleibol", "Jueves 17:00", "Prof. Luis Gómez · Cupo 16/25"],
      ].map(([name, schedule, detail]) => activityCard(name, schedule, detail, button(state.enrolled.includes(name) ? "Ya inscrito" : "Inscribirme", `enroll:${name}`))).join(""), "alumno");
      break;
    case "student-schedule":
      html = shell("Mi horario", `<p class="muted">Actividades inscritas:</p>${state.enrolled.map((name, index) => `<article class="schedule"><b>${index % 2 ? "18:00" : "16:00"}</b><div><h3>${name}</h3><p>Lunes y miércoles · ITSM</p></div></article>`).join("")}${button("Agregar otra actividad", "route:student-activities", "secondary")}`, "alumno");
      break;
    case "certificate":
      html = shell("Constancia", `${select("Actividad", ["Fútbol — Ene/Jun 2025"])}<div class="certificate">${icon("workspace_premium")}<h2>Constancia disponible</h2><p>Asistencia: <b>85%</b></p><div class="progress"><span></span></div>${button(`${icon("download")} Descargar PDF`, "download")}</div>`, "alumno");
      break;
    case "teacher-home":
      html = shell("Mis clases", `<div class="welcome"><p>Buen día,</p><h2>Prof. Carlos Pérez</h2></div>${activityCard("Fútbol", "Lun. y Mié. 16:00", "25 alumnos", `${button("Pase de lista", "route:attendance")}${button("Ver alumnos", "route:teacher-students", "secondary")}`)}${activityCard("Baloncesto", "Viernes 17:00", "18 alumnos", button("Pase de lista", "route:attendance"))}`, "docente");
      break;
    case "attendance":
      html = shell("Pase de lista", `<h3 class="title">Fútbol · Miércoles</h3>${state.students.map((name, index) => `<label class="check-row"><span><b>${name}</b><small>Matrícula 20240${index + 21}</small></span><input type="checkbox" ${index !== 3 ? "checked" : ""}></label>`).join("")}<div class="actions">${button("Guardar asistencia", "save-attendance")}</div>`, "docente", "teacher-home");
      break;
    case "teacher-students":
      html = shell("Alumnos inscritos", `<div class="search">${icon("search")}<input placeholder="Buscar alumno"></div>${state.students.map((name, i) => personCard(name, `Matrícula 20240${i + 21}`)).join("")}`, "", "teacher-home");
      break;
    case "evidence":
      html = shell("Subir evidencia", `<form class="form" data-submit="evidence">${select("Actividad", ["Fútbol", "Baloncesto"])}${field("Descripción")}<label class="field"><span>Fotografía</span><input type="file" accept="image/*" capture="environment" required></label>${button("Guardar evidencia", "submit")}</form>`, "docente");
      break;
    case "admin-home":
      html = shell("Administrador", `<div class="welcome"><p>Bienvenido,</p><h2>Administrador</h2></div>${shortcut("event_note", "Actividades", "manage-activities")}${shortcut("school", "Docentes", "manage-teachers")}${shortcut("groups", "Alumnos", "manage-students")}${shortcut("description", "Reportes", "reports")}`, "admin");
      break;
    case "manage-activities":
      html = shell("Actividades", `${button(`${icon("add")} Nueva actividad`, "route:activity-form")}${state.activities.map((name) => activityCard(name, "Lun. y Mié. 16:00", "Cupo 18/25 · Docente asignado", `${button("Editar", "route:activity-form")}${button("Eliminar", `delete-activity:${name}`, "danger")}`)).join("")}`, "admin");
      break;
    case "activity-form":
      html = formPage("Nueva / editar actividad", `${field("Nombre")}${field("Horario")}${field("Cupo", "number")}${select("Docente", state.teachers)}`, "manage-activities", "activity");
      break;
    case "manage-teachers":
      html = shell("Docentes", `<div class="tabs"><button class="active" data-route="manage-teachers">Docentes</button><button data-route="manage-students">Alumnos</button></div>${button(`${icon("add")} Nuevo docente`, "route:teacher-form")}${state.teachers.map((name) => personCard(name, "Especialidad extracurricular", "teacher")).join("")}`, "admin", "admin-home");
      break;
    case "teacher-form":
      html = formPage("Registrar / editar docente", `${field("Nombre")}${field("Correo", "email")}${field("Especialidad")}${field("Teléfono", "tel")}`, "manage-teachers", "teacher");
      break;
    case "manage-students":
      html = shell("Alumnos", `<div class="tabs"><button data-route="manage-teachers">Docentes</button><button class="active" data-route="manage-students">Alumnos</button></div><div class="search">${icon("search")}<input placeholder="Buscar nombre o matrícula"></div>${button(`${icon("add")} Nuevo alumno`, "route:student-form")}${state.students.map((name, i) => personCard(name, `Matrícula 20240${i + 21}`, "student")).join("")}`, "admin", "admin-home");
      break;
    case "student-form":
      html = formPage("Agregar / editar alumno", `${field("Nombre")}${field("Correo", "email")}${field("Matrícula")}${select("Carrera", ["Ing. en Sistemas", "Ing. Industrial"])}${select("Semestre", ["1°", "3°", "5°"])}`, "manage-students", "student");
      break;
    case "reports":
      html = shell("Reportes", `<form class="form" data-submit="report">${select("Tipo de reporte", ["Por actividad", "Por alumno", "Por docente"])}${field("Fecha inicial", "date")}${field("Fecha final", "date")}${button(`${icon("bar_chart")} Generar reporte`, "submit")}${button(`${icon("picture_as_pdf")} Exportar PDF`, "export", "secondary")}${state.reportReady ? `<div class="success"><b>Reporte listo</b><p>12 actividades · 248 alumnos</p></div>` : ""}</form>`, "admin");
      break;
  }

  app.innerHTML = html;
}

function routeTo(route) {
  state.route = route;
  render();
}

app.addEventListener("click", (event) => {
  const target = event.target.closest("button");
  if (!target) return;

  if (target.dataset.route) return routeTo(target.dataset.route);
  const action = target.dataset.action || "";

  if (action.startsWith("route:")) return routeTo(action.split(":")[1]);
  if (action.startsWith("enroll:")) {
    const name = action.slice(7);
    if (!state.enrolled.includes(name)) state.enrolled.push(name);
    return routeTo("student-schedule");
  }
  if (action.startsWith("delete-activity:")) {
    const name = action.slice(16);
    if (confirm(`¿Eliminar "${name}"?`)) state.activities = state.activities.filter((item) => item !== name);
    return render();
  }
  if (target.dataset.delete) {
    const list = target.dataset.delete === "teacher" ? "teachers" : "students";
    if (confirm(`¿Eliminar a "${target.dataset.name}"?`)) state[list] = state[list].filter((item) => item !== target.dataset.name);
    return render();
  }
  if (action === "download") return alert("Descarga de constancia PDF iniciada.");
  if (action === "export") return state.reportReady ? alert("Exportación PDF iniciada.") : alert("Primero genera el reporte.");
  if (action === "save-attendance") {
    alert("Asistencia guardada.");
    return routeTo("teacher-home");
  }
});

app.addEventListener("submit", (event) => {
  event.preventDefault();
  const action = event.target.dataset.submit;

  if (action === "login") {
    state.role = document.querySelector("#role").value;
    return routeTo(homes[state.role]);
  }
  if (action === "register") {
    alert("Registro realizado.");
    return routeTo("login");
  }
  if (action === "evidence") {
    alert("Evidencia guardada.");
    return routeTo("teacher-home");
  }
  if (action === "activity") {
    if (!state.activities.includes("Nueva actividad")) state.activities.push("Nueva actividad");
    return routeTo("manage-activities");
  }
  if (action === "teacher") {
    if (!state.teachers.includes("Nuevo docente")) state.teachers.push("Nuevo docente");
    return routeTo("manage-teachers");
  }
  if (action === "student") {
    if (!state.students.includes("Nuevo alumno")) state.students.push("Nuevo alumno");
    return routeTo("manage-students");
  }
  if (action === "report") {
    state.reportReady = true;
    alert("Reporte generado.");
    return render();
  }
});

render();
