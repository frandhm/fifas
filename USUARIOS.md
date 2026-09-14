# Usuarios y perfil

El perfil consulta `GET /api/usuarios/me` y guarda con `PUT /api/usuarios/me`, pasando por el API Gateway y el BFF. El microservicio persiste en la tabla `usuarios` de MySQL/RDS. El frontend obtiene el access token con el interceptor MSAL existente.

La primera consulta devuelve `404 {"code":"PROFILE_NOT_FOUND"}` si la cuenta todavía no tiene perfil. El formulario usa los datos disponibles de Microsoft y permite completar los campos faltantes. El primer guardado crea el registro y los siguientes actualizan el mismo. Una ruta inexistente del despliegue no se interpreta como un perfil nuevo.

## Datos e identidad

Ejemplo del cuerpo de PUT:

```json
{
  "nombre": "Ana",
  "apellido": "Pérez",
  "correo": "ana@example.com",
  "telefono": "+56912345678",
  "direccion": "Santiago 123"
}
```

Nombre, apellido y correo son obligatorios. Teléfono y dirección aceptan cadenas vacías. La respuesta incluye estos campos y el `id` de la base.

La identidad proviene del issuer y subject del JWT validado; no se recibe un propietario ni un ID de usuario del frontend. Cambiar el correo de contacto no cambia la cuenta de Microsoft. El servicio exige el scope `access_as_user` y valida el audience configurado. Las rutas del CRUD antiguo (`/api/usuarios` y `/api/usuarios/{id}`) quedan bloqueadas para impedir acceso a perfiles ajenos.

La tabla incorpora `apellido`, `telefono` y `propietario`, con un índice único para propietario. Los registros antiguos sin propietario no se vinculan automáticamente por correo; requerirían una migración con identidad verificada. El perfil ya no lee ni escribe datos personales en `fifas.auth.user` de localStorage. La dirección visible y el nombre de la barra lateral provienen del servicio; se retiraron los datos ficticios del perfil.

## Configuración del microservicio en AWS

Configura estas variables en el entorno que ejecuta el JAR (systemd, contenedor o consola de despliegue):

- `USUARIOS_DB_USERNAME`: usuario de MySQL con acceso a `erp_cloud`.
- `USUARIOS_DB_PASSWORD`: contraseña de ese usuario.
- `USUARIOS_DB_URL`: opcional; el valor predeterminado conserva el endpoint RDS existente, base `erp_cloud`, puerto 3306 y exige TLS con `sslMode=REQUIRED`.
- `USUARIOS_DB_POOL_MAX` y `USUARIOS_DB_POOL_MIN_IDLE`: valores predeterminados 5 y 1. Limitan las conexiones de usuarios para compartir la RDS con los demás microservicios.
- `USUARIOS_DDL_AUTO`: predeterminado `update`, para agregar las columnas e índice al iniciar. Si el esquema se administra por migraciones, aplícalas antes y usa `validate`.
- `JWT_ISSUER_URI`: predeterminado el issuer v2.0 del tenant de Microsoft ya configurado.
- `JWT_AUDIENCE`: predeterminado `4974cb3b-f538-4df9-8aec-d2c5dca687d0`, que debe coincidir con `aud` del access token de esta API.

Las credenciales de RDS dejaron de estar incrustadas en `application.properties`: las dos variables de credenciales son necesarias para iniciar el servicio.

El BFF mantiene la ruta de usuarios hacia `http://172.31.24.223:8082`. Desde esa máquina, RDS debe ser accesible en 3306. El BFF debe poder llegar al servicio en 8082. API Gateway debe reenviar GET y PUT de `/api/usuarios/me`, incluidos el cuerpo JSON y Authorization, y permitirlos en su configuración CORS para el origen del frontend.

Despliega tanto el nuevo JAR de usuarios como el BFF: el BFF ahora conserva el estado HTTP y el cuerpo originales, incluidos 404, 400, 401, 409 y 204. Sin ese cambio el frontend no puede distinguir un perfil nuevo de un fallo del servidor. Actualiza también el frontend compilado en `frontend/dist/frontend/browser`.

## Compilación y pruebas locales

Desde la raíz del repositorio:

```sh
mvn -f backend/usuarioService/pom.xml test
mvn -f backend/bff/pom.xml -Dtest=ProxyResponseTests,PreflightTests test
npm --prefix frontend test -- --watch=false
npm --prefix frontend run build
mvn -f backend/usuarioService/pom.xml -DskipTests package
mvn -f backend/bff/pom.xml -DskipTests package
```

Las pruebas de usuarios usan H2 en memoria; no modifican RDS. Cubren guardado, recuperación, actualización sin duplicados, separación de cuentas, autenticación y validación. Las pruebas del frontend cubren carga, errores, guardado confirmado y cambio de cuenta. Las del BFF verifican la conservación de respuestas y preflight.

## Verificación después del despliegue

1. Inicia sesión con Microsoft y abre Perfil. En Network debe aparecer GET al API Gateway terminado en `/api/usuarios/me`.
2. Completa los campos y guarda: PUT debe responder 200 con un ID. La confirmación solo aparece después de esa respuesta.
3. Recarga el navegador y verifica que GET devuelva los datos guardados. Repite desde otra sesión de navegador con la misma cuenta.
4. Inicia sesión con otra cuenta: no debe mostrar el perfil anterior.
5. En RDS verifica que exista un único registro para cada propietario y que un segundo guardado conserve el ID.

Despliegue realizado el 12 de septiembre de 2026:

- Usuarios actualizado en `100.53.129.80` (`172.31.24.223`), puerto 8082.
- BFF actualizado en `3.212.71.32`, puerto 8089.
- API Gateway: integración `vrpjw4t`, rutas GET y PUT `/api/usuarios/me`, despliegue `1f90p3` publicado en el stage `fifas`.
- Petición sin token: HTTP 401 con CORS correcto para `http://localhost:4200`.
- RDS: conexión TLS, columnas del perfil, escritura y lectura verificadas mediante una transacción revertida; no quedaron datos de prueba. Se redujo el pool de usuarios a un máximo de 5 conexiones y un mínimo de 1 tras detectar `Too many connections`.
- Petición desde la sesión Azure de Safari: el perfil inexistente se interpreta correctamente, se carga el correo y se habilita el formulario.

En el servidor de usuarios se conservó la configuración de acceso a RDS en `/home/ec2-user/app/usuarios-db.properties` con permisos 600, cargada mediante `--spring.config.additional-location`. Este archivo satisface la configuración de usuario y contraseña sin incrustarlas en el nuevo JAR. No se copia al repositorio.

Se dejaron scripts `/home/ec2-user/app/start-usuarios.sh` y `/home/ec2-user/app/start-bff.sh` en sus respectivos servidores. Los servicios continúan ejecutándose como procesos Java, igual que antes; estos scripts no configuran arranque automático al reiniciar EC2.

Respaldos en `/home/ec2-user/app` de cada servidor: `usuarios-service-0.0.1-SNAPSHOT.jar.backup-20260912-183818` y `bff-0.0.1-SNAPSHOT.jar.backup-20260912-183841`. La copia de la configuración previa de API Gateway está en `/tmp/fifas-api-before-profile.json` del equipo local. No se han cambiado los datos personales del usuario para probar el formulario.
