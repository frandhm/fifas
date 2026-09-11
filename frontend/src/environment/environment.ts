export const environment = {
  production: false,
  azure: {
    clientId: '4974cb3b-f538-4df9-8aec-d2c5dca687d0',
    tenantId: '3fb87110-4624-4d64-8806-a2304cfbbceb',
    authority: 'https://login.microsoftonline.com/3fb87110-4624-4d64-8806-a2304cfbbceb',
    redirectUri: 'http://localhost:4200',
    // Solo si tu API pide un scope específico (ej. api://TU_API_ID/access_as_user)
    protectedResourceScopes: ['api://PON_AQUI_TU_API_ID/access_as_user'],
  },
  // URL base de tu backend (BFF o API Gateway) - ajusta cuando la tengas lista
  apiBaseUrl: 'http://localhost:8080',
};