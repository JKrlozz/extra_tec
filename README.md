# ITSM Extraescolares

Aplicación Android (Kotlin, MVVM, ViewBinding) para gestionar las actividades extraescolares del Instituto Tecnológico Superior de Misantla. Los datos viven en **Azure SQL** y la aplicación habla con ellos a través de una **API Node.js** publicada en Azure App Service.

```
App Android  ──HTTPS──▶  API (Azure App Service: extratec-api-movil)  ──▶  Azure SQL (extratec-bd)
```

## Probar la aplicación (no necesitas credenciales de Azure)

La API ya está publicada y la aplicación apunta a ella desde `gradle.properties` (`API_URL`).

1. Abre el proyecto en Android Studio y sincroniza Gradle.
2. Ejecuta la app en un emulador o en un teléfono con internet.
3. Entra con una cuenta de prueba. La contraseña de todas es `itsm1234`.

| Rol | Tipo de usuario en la pantalla | Correo de ejemplo |
|---|---|---|
| Administrador | Administrador | `admin@itsm.edu.mx` |
| Docente | Docente | `jjimenez@itsm.edu.mx`, `rhidalgo@itsm.edu.mx`, `cdrodriguez@itsm.edu.mx`, `cmendez@itsm.edu.mx`, `esesma@itsm.edu.mx`, `zrodriguez@itsm.edu.mx` |
| Alumno | Alumno | su número de control en minúsculas, por ejemplo `231t0127@itsm.edu.mx` (Joseph de los Santos), `231t0412@itsm.edu.mx` (Milton Ramírez), `231t0203@itsm.edu.mx` (Jared Pardo, con asistencia baja) |

Los 17 alumnos, 6 docentes y 8 actividades de ejemplo salen de `api/scripts/crear_bd.js`.

### Cosas que hay que saber al probar

- **Primer uso lento.** El App Service gratuito se duerme tras 20 minutos sin uso y la base serverless se pausa sola. La primera petición puede tardar hasta un minuto; la app espera hasta 90 segundos.
- **Zona horaria del emulador.** La API calcula "hoy" con hora de México. Si el emulador está en UTC, el pase de lista puede fallar con "La fecha no puede ser posterior a hoy". Se arregla con `adb shell cmd alarm set-timezone America/Mexico_City`.
- **Semestre.** Solo se puede inscribir, pasar lista y subir evidencias con un semestre vigente. El de ejemplo va del 17/08/2026 al 18/12/2026 y se **archiva solo** al pasar la fecha de fin. Después, el administrador debe iniciar uno nuevo en *Semestre* (copia las actividades sin alumnos).
- **Los datos son compartidos.** Todos los que prueben usan la misma base. Lo que alguien registre lo ve el resto hasta que se restablezca con `npm run bd`.
- **Constancias.** Se emiten con 80 % o más de asistencia. Los alumnos de ejemplo tienen casos distintos para probar ambos lados.

## Trabajar con la API y la base

Las credenciales **no están en el repositorio**. Pídelas a quien administra el recurso de Azure (`recursos-extratec`): usuario y contraseña del servidor SQL, y un `JWT_SECRET`.

```
cd api
npm install
copy .env.example .env     # llena DB_USER, DB_PASSWORD y JWT_SECRET
npm test                   # pruebas de validaciones, no necesita la base
npm start                  # http://localhost:3000/api/salud
npm run bd                 # BORRA y recrea tablas y datos de ejemplo
npm run migrar             # agrega lo del archivo de semestres a una base ya creada, sin borrar datos
```

Para correr `npm run bd`, `npm run migrar` o `npm start` desde tu computadora hay que agregar tu IP en *servidor-extratec → Redes → Reglas de firewall* del portal de Azure.

Para apuntar la app a tu API local o a otra publicada, cambia `API_URL` en `gradle.properties` y vuelve a compilar (con HTTP sin cifrar Android bloquea el tráfico, usa HTTPS).

### Publicar cambios de la API

Se despliega con un zip a través de Kudu, sin la línea de comandos de Azure:

```
cd api
tar -a -c -f api.zip src package.json package-lock.json
```

Ese `tar` es el de Windows (PowerShell o cmd). El `tar` de Git Bash crea un `.tar` aunque el nombre diga `.zip` y Kudu no lo acepta; desde Git Bash usa `/c/Windows/System32/tar.exe`.

Sube `api.zip` en `https://<nombre-del-app-service>.scm.<region>.azurewebsites.net/ZipDeployUI`. No incluyas `.env` ni `node_modules`. Las variables de entorno se conservan en el App Service. Más detalle en [`api/README.md`](api/README.md).

## Pruebas automáticas

```
./gradlew testDebugUnitTest     # pruebas unitarias de la app
cd api && npm test              # pruebas de la API
```

## Estructura

| Carpeta | Contenido |
|---|---|
| `app/` | Aplicación Android. `Modelo/Red.kt` hace las peticiones a la API |
| `api/` | API Node.js (Express, mssql, JWT). Scripts de base de datos en `api/scripts/` |
| `database/azure/schema.sql` | Esquema T-SQL de la base en Azure |
| `contexto.md` | Notas técnicas y decisiones del proyecto |

`servidor_legado/` y los archivos `schema.sql`, `schema.sqlite.sql` y `seed.sql` de `database/` son de la versión anterior (SQLite local y servidor MySQL) y ya no se usan.
