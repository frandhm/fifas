# Módulo de soporte

## Flujo

El frontend llama a `POST /api/mensajes` y `GET /api/mensajes` mediante API Gateway y el BFF. El BFF reenvía la petición autenticada al microservicio de soporte en el puerto `8088`; solo ese servicio se conecta a MySQL en RDS.

Nunca se deben publicar credenciales de AWS ni de RDS en Angular, Git o una imagen Docker.

## Variables del microservicio

Configurar estas variables en la instancia o servicio que ejecuta `soporteService`:

```text
SOPORTE_DB_URL=jdbc:mysql://HOST_RDS:3306/erp_cloud?useSSL=true&serverTimezone=UTC
SOPORTE_DB_USERNAME=app_soporte
SOPORTE_DB_PASSWORD=secreto_rotado
AZURE_ISSUER_URI=https://login.microsoftonline.com/TENANT_ID/v2.0
AZURE_API_AUDIENCE=API_CLIENT_ID
```

Se recomienda guardar la contraseña en AWS Secrets Manager y entregarla al proceso como variable de entorno mediante un rol IAM. El Security Group de RDS debe aceptar el puerto `3306` solo desde el Security Group del servidor de soporte, nunca desde Internet.

Con `spring.jpa.hibernate.ddl-auto=update`, Hibernate ampliará `mensajes_soporte` con los campos de asunto, categoría, estado, fecha y propietario al iniciar el servicio. En producción conviene reemplazar esta actualización automática por una migración versionada.

## Verificación local

```sh
cd backend/soporteService
./mvnw -DskipTests package

cd ../../frontend
npx ngc -p tsconfig.app.json --noEmit
```
