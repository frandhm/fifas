# Autorización con Microsoft Entra ID

El BFF valida el JWT contra el issuer y audience configurados. También convierte el
claim `roles` de Microsoft Entra en autoridades Spring con prefijo `ROLE_`; por
ejemplo, `Admin` pasa a ser `ROLE_Admin`.

## Configuración requerida en Entra ID

1. En **App registrations**, abre el registro de la API cuyo Application (client) ID
   es el valor de `AZURE_API_AUDIENCE`.
2. En **App roles**, crea al menos los roles `Admin` y `Cliente`, permitidos para
   usuarios y grupos.
3. En **Enterprise applications**, asigna cada usuario o grupo a uno de esos roles.
4. Cierra la sesión de la aplicación e inicia sesión nuevamente. El access token debe
   contener un claim equivalente a `"roles": ["Admin"]` o `"roles": ["Cliente"]`.
5. Configura en el despliegue `AZURE_ISSUER_URI` y `AZURE_API_AUDIENCE`; no dependas
   de los valores predeterminados fuera del ambiente de desarrollo.

## Comprobación

El perfil muestra los roles recibidos en el token. Si dice **Sin rol asignado**, el
token es válido, pero no se asignó un App Role en Entra ID o se debe renovar la
sesión. Las rutas bajo `/api/admin/**` exigen `ROLE_Admin` en el BFF; una cuenta con
otro rol recibe HTTP 403.

El catálogo exige `SCOPE_access_as_user` para las consultas y reserva crear,
actualizar o eliminar productos para `ROLE_Admin`. Así, el frontend puede mostrar el
catálogo a usuarios que iniciaron sesión, pero no permite cambiarlo desde una cuenta
cliente.
