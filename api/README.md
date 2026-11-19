# API de ITSM Extraescolares

La app Android ya no guarda nada en el teléfono: habla por HTTPS con esta API (Node.js + Express) y la API guarda todo en **Azure SQL** (`servidor-extratec` / `extratec-bd`).

```
App Android  ──HTTPS──▶  API (Azure App Service)  ──▶  Azure SQL (extratec-bd)
```

- La base se crea con `database/azure/schema.sql` (T-SQL) y los datos de prueba salen de `scripts/crear_bd.js`.
- Las fotografías de las evidencias se guardan dentro de la base (`VARBINARY`), así no hace falta una cuenta de Storage.
- Las contraseñas se guardan con PBKDF2 y la sesión usa un token (JWT) de 30 días.

## 1. Permitir el acceso a la base (portal de Azure)

En `servidor-extratec` → **Redes** (Networking):

1. **Reglas de firewall → Agregar la dirección IP de cliente** (tu IP actual), para poder crear las tablas desde tu computadora.
2. Activa **Permitir que los servicios y recursos de Azure accedan a este servidor**, para que la API publicada pueda conectarse.

Necesitas el **usuario y contraseña de administrador SQL** del servidor (los que pusiste al crearlo). Si el servidor solo permite autenticación de Microsoft Entra, hay que activar también la autenticación SQL.

## 2. Configurar y crear las tablas

```powershell
cd api
npm install
copy .env.example .env      # y llena DB_USER, DB_PASSWORD y JWT_SECRET
npm run bd                  # crea tablas y datos de prueba (borra lo que haya)
npm start                   # http://localhost:3000/api/salud debe responder {"ok":true}
```

`JWT_SECRET` puede ser cualquier texto largo y aleatorio:

```powershell
node -e "console.log(require('crypto').randomBytes(48).toString('hex'))"
```

Datos de ejemplo (contraseña `itsm1234` para todos): administrador `admin@itsm.edu.mx`; 6 docentes (`jjimenez`, `rhidalgo`, `cdrodriguez`, `cmendez`, `esesma`, `zrodriguez` `@itsm.edu.mx`); 17 alumnos, cuyo correo es su número de control en minúsculas (por ejemplo `231t0412@itsm.edu.mx`, Milton Ramírez). Hay 8 actividades (fútbol, baloncesto y voleibol matutino/vespertino, banda de guerra y ajedrez), cada alumno inscrito en una y cada docente a cargo de al menos una.

## 3. Publicar la API (Azure App Service, plan gratuito F1)

Con la [CLI de Azure](https://learn.microsoft.com/cli/azure/install-azure-cli) instalada. Elige un nombre único (por ejemplo `extratec-api-movil`) y la misma región que el servidor SQL:

```powershell
az login
az appservice plan create --name plan-extratec --resource-group recursos-extratec --sku F1 --is-linux --location westus3
az webapp create --name extratec-api-movil --resource-group recursos-extratec --plan plan-extratec --runtime "NODE:20-lts"

az webapp config appsettings set --resource-group recursos-extratec --name extratec-api-movil --settings `
  SCM_DO_BUILD_DURING_DEPLOYMENT=true DB_SERVER=servidor-extratec.database.windows.net DB_NAME=extratec-bd `
  DB_USER=<usuario> DB_PASSWORD=<contraseña> JWT_SECRET=<texto-largo>

cd api
tar -a -c -f api.zip src package.json package-lock.json
az webapp deploy --resource-group recursos-extratec --name extratec-api-movil --src-path api.zip --type zip
```

Prueba: `https://extratec-api-movil-h9fyaka4d3eagta6.westus3-01.azurewebsites.net/api/salud`.

## 4. Apuntar la app a la API

En `gradle.properties` (raíz del proyecto) cambia:

```
API_URL=https://extratec-api-movil-h9fyaka4d3eagta6.westus3-01.azurewebsites.net
```

y vuelve a compilar la app.

## Cosas a tener en cuenta (plan gratuito)

- **Primer uso lento:** App Service F1 se duerme a los 20 minutos sin uso y la base serverless se pausa sola. La primera petición puede tardar de 30 s a 1 min; la app espera hasta 90 s y la API reintenta la conexión.
- F1 tiene un límite de 60 minutos de CPU al día: suficiente para un proyecto escolar.
- No subas `.env` a git (ya está en `api/.gitignore`).

## Semestre y archivo

El administrador define en la app (**Semestre**) las fechas de inicio y fin del semestre en uso.

- **Con el semestre en curso** se puede inscribir, pasar lista y subir evidencias. Antes de la fecha de inicio la API lo rechaza.
- **Al llegar la fecha de fin** (o con el botón *Cerrar y archivar ahora*) el semestre se archiva: las inscripciones pasan a `archivada` y las actividades se desactivan. Nada se borra; solo deja de aparecer en listas, horarios y reportes. Los alumnos conservan sus constancias de semestres archivados.
- **Para iniciar el siguiente** el administrador elige sus fechas. Se copian las actividades del semestre anterior (docente, cupo y horarios) sin alumnos ni registros.
- El archivo ocurre solo en la primera petición que llega después de la fecha de fin.

Si la base se creó antes de esta función, agrega lo necesario sin perder datos con `npm run migrar` (se puede repetir sin problema). `npm run bd` ya crea todo con la estructura nueva.

## Pruebas

`npm test` prueba las validaciones y el cifrado de contraseñas (no necesita la base).
