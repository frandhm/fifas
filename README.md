# ⚽ Los FIFAS

Plataforma e-commerce de camisetas de fútbol con personalización, construida con una arquitectura de **microservicios** (Spring Boot) y un frontend **Angular 21**. Autenticación gestionada por **Azure AD (MSAL)** y despliegue en **AWS**.

---

## 📐 Arquitectura

```
┌─────────────────────────────────────────────────────────────────────┐
│                          FRONTEND                                   │
│                   Angular 21 + TailwindCSS 4                        │
│                  Autenticación: MSAL (Azure AD)                     │
└────────────────────────────┬────────────────────────────────────────┘
                             │ HTTPS
                             ▼
              ┌──────────────────────────────┐
              │     API Gateway (AWS)         │
              │  Amazon API Gateway / ALB     │
              └──────────────┬───────────────┘
                             │
                             ▼
              ┌──────────────────────────────┐
              │         BFF (:8089)           │
              │  Spring Boot 4.1 – Proxy     │
              │  OAuth2 Resource Server      │
              └──────────────┬───────────────┘
                             │  HTTP (VPC interna)
           ┌─────────┬──────┴──────┬──────────┬──────────┐
           ▼         ▼            ▼          ▼          ▼
     ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐
     │ Usuarios │ │Productos │ │ Carrito  │ │Solicitud │ │Notificaciones│
     │  :8082   │ │  :8083   │ │  :8084   │ │  :8087   │ │    :8086     │
     └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ └──────┬───────┘
          │            │            │            │              │
          └────────────┴────────────┴────────────┴──────────────┘
                                    │
                              ┌─────┴─────┐
                              │  MySQL    │
                              │ (AWS RDS) │
                              └───────────┘
```

> Además existe un **soporteService** (`:8088`) que gestiona mensajes de soporte/chat.

---

## 🗂️ Estructura del Proyecto

```
fifas/
├── frontend/                   # Angular 21 SPA
│   ├── src/
│   │   ├── app/
│   │   │   ├── core/           # Guards, interceptors, layout, auth
│   │   │   ├── features/       # Módulos funcionales
│   │   │   │   ├── auth/       # Login / registro (MSAL)
│   │   │   │   ├── home/       # Landing page
│   │   │   │   ├── catalog/    # Catálogo + detalle de producto
│   │   │   │   ├── custom-jersey/ # Personalización de camisetas
│   │   │   │   ├── cart/       # Carrito de compras
│   │   │   │   ├── profile/    # Perfil de usuario
│   │   │   │   ├── transactions/ # Historial de transacciones
│   │   │   │   ├── notifications/ # Centro de notificaciones
│   │   │   │   └── support/    # Soporte / chat
│   │   │   └── share/          # Componentes y utilidades compartidas
│   │   └── environment/        # Configuración de entorno
│   ├── angular.json
│   ├── package.json
│   └── tsconfig.json
│
└── backend/
    ├── bff/                    # Backend For Frontend – Proxy central
    ├── usuarioService/         # Gestión de usuarios y perfiles
    ├── productoService/        # Catálogo de productos
    ├── carritoService/         # Carrito de compras
    ├── solicitudService/       # Solicitudes / órdenes
    ├── notificacionService/    # Notificaciones
    └── soporteService/         # Mensajes de soporte
```

---

## 🛠️ Tech Stack

| Capa | Tecnología |
|---|---|
| **Frontend** | Angular 21, TypeScript 5.9, TailwindCSS 4, RxJS 7 |
| **Autenticación** | Azure AD (MSAL Angular + MSAL Browser) |
| **BFF** | Spring Boot 4.1, WebFlux (`WebClient`), OAuth2 Resource Server |
| **Microservicios** | Spring Boot 3.2.5, Spring Data JPA, Bean Validation |
| **Base de datos** | MySQL (AWS RDS) · H2 para tests |
| **Infraestructura** | AWS (API Gateway, EC2/ECS, RDS) |
| **Java** | JDK 17 |
| **Build tools** | Maven (backend) · npm (frontend) |
| **Testing** | Vitest (frontend) · JUnit 5 + Spring Security Test (backend) |

---

## 🚀 Inicio Rápido

### Prerrequisitos

- **Node.js** ≥ 20 y **npm** ≥ 11
- **JDK 17**
- **Maven** 3.9+ (o usar el wrapper `./mvnw` incluido)
- **MySQL** 8+ (o acceso a la instancia RDS)

### Frontend

```bash
cd frontend
npm install
npm start
```

La app se levanta en `http://localhost:4200`.

### Backend (cada microservicio)

Cada servicio se ejecuta de forma independiente. Ejemplo con `productoService`:

```bash
cd backend/productoService
./mvnw spring-boot:run
```

#### Variables de entorno requeridas por servicio

| Variable | Descripción | Ejemplo |
|---|---|---|
| `*_DB_URL` | URL JDBC de la base de datos | `jdbc:mysql://host:3306/erp_cloud?useSSL=true` |
| `*_DB_USERNAME` | Usuario de la DB | `app_productos` |
| `*_DB_PASSWORD` | Contraseña de la DB | *(secreto)* |
| `AZURE_ISSUER_URI` | URI del emisor de tokens Azure AD | `https://login.microsoftonline.com/{tenant}/v2.0` |
| `AZURE_API_AUDIENCE` | Client ID de la API en Azure | `4974cb3b-...` |

> 📝 Consulta el archivo [`.env.example`](backend/soporteService/.env.example) del servicio de soporte como referencia.

### BFF

```bash
cd backend/bff
./mvnw spring-boot:run
```

El BFF escucha en el puerto **8089** y enruta las peticiones `GET/POST/PUT/DELETE /api/{servicio}/**` al microservicio correspondiente.

---

## 🔐 Autenticación

El flujo de autenticación utiliza **Microsoft Identity Platform (Azure AD)**:

1. El usuario inicia sesión en el frontend a través de **MSAL Redirect Flow**.
2. El frontend obtiene un token JWT con el scope `api://{clientId}/access_as_user`.
3. El interceptor HTTP (`apiAuthInterceptor`) adjunta el token `Bearer` a cada petición al BFF.
4. El BFF y cada microservicio validan el JWT como **OAuth2 Resource Server**.

---

## 🧪 Tests

### Frontend

```bash
cd frontend
npm test
```

### Backend

```bash
cd backend/<servicio>
./mvnw test
```

Los tests del backend usan **H2 en memoria** para no depender de MySQL.

---

## 📡 Puertos por Servicio

| Servicio | Puerto |
|---|---|
| Frontend (dev server) | `4200` |
| BFF | `8089` |
| usuarioService | `8082` |
| productoService | `8083` |
| carritoService | `8084` |
| notificacionService | `8086` |
| solicitudService | `8087` |
| soporteService | `8088` |

---

## 👥 Equipo

Proyecto académico — **DUOC UC**.

---

## 📄 Licencia

Uso académico. Todos los derechos reservados.
