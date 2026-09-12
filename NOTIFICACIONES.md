# Notificaciones

La campana del encabezado abre `/notificaciones`. El historial consume el microservicio a través del BFF y permite filtrar por leídas y marcar avisos como leídos. No hay datos de ejemplo en el historial.

## Eventos

- Retorno exitoso del login de Microsoft (no cada recarga de página).
- Agregar, quitar, cambiar cantidades y vaciar el carrito, incluidas camisetas personalizadas.
- Guardar el perfil: el mensaje aclara que el guardado actual es local.
- Simular compra: aviso de demostración, sin cobros, pedidos ni vaciado del carrito.

Los invitados reciben un aviso emergente de sus acciones, pero no se guarda una notificación sin cuenta. Si falla la persistencia del aviso, se informa sin deshacer la acción.

## Despliegue requerido

1. Compilar y desplegar `backend/notificacionService` (Java 17+, puerto 8086). Se agregaron columnas `propietario`, `tipo` y `fecha`. Con el `ddl-auto=update` existente Hibernate actualiza el esquema; revisar y respaldar la tabla antes de desplegar según el procedimiento del equipo.
2. Configurar `AZURE_ISSUER_URI` y `AZURE_API_AUDIENCE` de acuerdo con el registro de la API. Los valores predeterminados corresponden al tenant y API del proyecto, con tokens Azure v2. El emisor debe coincidir con el del token que valida el BFF.
3. Mantener en API Gateway las rutas `GET /api/notificaciones`, `POST /api/notificaciones`, `PUT /api/notificaciones/{proxy+}` hacia el BFF puerto 8089, conservando método, ruta y Authorization. La ruta PUT debe llegar al BFF como `/api/notificaciones/{id}`.
4. CORS: permitir el origen del frontend, métodos GET/POST/PUT/OPTIONS y encabezados Authorization y Content-Type. Publicar los cambios en la etapa utilizada.
5. Desplegar el frontend actualizado.

El propietario se deriva del JWT validado en el microservicio, nunca de un usuarioId enviado por el navegador. Las notificaciones antiguas que no tienen propietario no se muestran: no existe un vínculo seguro entre su usuarioId numérico y Microsoft Entra en el esquema actual. No asignarlas automáticamente.

## Contrato

GET devuelve únicamente el historial de la cuenta autenticada, ordenado por ID descendente.
POST recibe `{ "mensaje": "…", "tipo": "sesion|carrito|perfil|compra" }` y asigna fecha, propietario y leido=false en el servidor.
PUT `/{id}` recibe `{ "leido": true }` y solo permite modificar avisos del propietario.

La categoría compra representa únicamente la demostración visual. No debe usarse como comprobante de pago ni como evidencia de una transacción real.

## Verificación manual en AWS

Iniciar sesión, agregar un producto, abrir la campana, marcar el aviso como leído y recargar para comprobar persistencia. Cambiar de cuenta y verificar que el historial anterior no aparezca. Probar la simulación de compra y verificar su texto explícito de demostración. Si devuelve 401, revisar WWW-Authenticate y la configuración de emisor/audiencia; no desactivar la validación.
