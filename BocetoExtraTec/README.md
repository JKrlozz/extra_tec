# Versión HTML, CSS y JavaScript

Esta carpeta contiene una versión independiente de la aplicación. El diseño está
escrito directamente en HTML y CSS; no es una exportación de Figma. No usa
React, Vite, Capacitor, bibliotecas de interfaz, tipografías remotas ni recursos
externos.

## Archivos

- `index.html`: estructura y carga de recursos.
- `styles.css`: diseño móvil adaptable.
- `app.js`: navegación, formularios y datos simulados.

Los tres archivos son locales. La aplicación no realiza peticiones de red, no
se conecta a una API y funciona sin internet.

## Abrir localmente

Se puede abrir `index.html` directamente en un navegador. Para evitar
restricciones del navegador también se puede servir la carpeta con cualquier
servidor HTTP estático, por ejemplo:

```bash
python3 -m http.server 8080 --directory referencia-html-css
```

Después abre `http://localhost:8080`.

## Integración en otros sistemas

La aplicación guarda su estado únicamente en memoria. No contiene conexiones
con Figma, Android Studio, bases de datos ni servicios externos.

La carpeta puede copiarse a proyectos PHP, Java, .NET, Django, Laravel u otros
sistemas que puedan servir archivos HTML, CSS y JavaScript.
