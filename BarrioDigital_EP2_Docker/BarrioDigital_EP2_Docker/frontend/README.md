# Pedidos360 — Frontend (Angular + MSAL)

SPA Angular 22 que autentica contra Microsoft Entra ID usando MSAL (Authorization Code Flow + PKCE) y consume el backend a través de AWS API Gateway.

## Arquitectura

```mermaid
graph LR
    subgraph Cliente
        A["Angular SPA<br/>MSAL + PKCE"]
    end
    subgraph Azure["Microsoft Entra ID (IDaaS)"]
        B["Tenant<br/>App SPA + App API<br/>Scope: OT.Create"]
    end
    subgraph AWS
        C["API Gateway<br/>JWT Authorizer + CORS"]
        D["EC2 · Spring Boot<br/>OAuth2 Resource Server"]
        E[("H2 archivo / RDS")]
    end
    A -->|"1. loginRedirect (code + PKCE)"| B
    B -->|"2. id_token + access_token"| A
    A -->|"3. HTTPS · Authorization: Bearer JWT"| C
    C -->|"4. token válido"| D
    C -.->|"401 token inválido"| A
    D --> E
```

## Flujo Authorization Code con PKCE

```mermaid
sequenceDiagram
    autonumber
    participant U as Usuario
    participant F as Angular SPA (MSAL)
    participant E as Entra ID
    U->>F: Click "Iniciar sesión"
    F->>F: Genera code_verifier + code_challenge (S256)
    F->>E: GET /authorize?client_id&code_challenge&state&nonce
    E->>U: Formulario login / consentimiento
    U->>E: Credenciales
    E->>F: Redirect a redirectUri con authorization code
    F->>E: POST /token (code + code_verifier)
    E->>E: Verifica SHA256(code_verifier) == code_challenge
    E->>F: id_token + access_token (JWT)
    F->>F: Cachea tokens en sessionStorage
```

## Flujo de petición autenticada (validación JWT)

```mermaid
sequenceDiagram
    autonumber
    participant F as Angular SPA
    participant G as API Gateway (AWS)
    participant B as Spring Boot (EC2)
    F->>G: GET /api/requests<br/>Authorization: Bearer JWT
    G->>G: JWT Authorizer: valida iss + aud
    alt token inválido o ausente
        G-->>F: 401 Unauthorized
    else token válido
        G->>B: Reenvía request + JWT
        B->>B: Resource Server verifica firma, issuer-uri, audience
        alt falta SCOPE_OT.Create
            B-->>F: 403 Forbidden
        else autorizado
            B-->>G: 200 + JSON
            G-->>F: 200 + JSON
        end
    end
```

## Archivos clave

| Archivo | Propósito |
|---|---|
| `src/environments/environment.ts` | `clientId`, `tenantId`, `backendScope`, `apiUrl` (API Gateway) |
| `src/app/factories/msal-instance.factory.ts` | `PublicClientApplication` → flujo PKCE, caché en `sessionStorage` |
| `src/app/app.config.ts` | `protectedResourceMap` + `MsalInterceptor` (Bearer automático) |
| `src/app/services/auth.service.ts` | `loginRedirect`, logout, roles desde `idTokenClaims` |
| `src/app/app.routes.ts` | `MsalGuard` en rutas protegidas, `roleGuard` en `/catalog` |
| `src/app/services/order.service.ts` | Llamadas a la API vía API Gateway, manejo de errores 401/403 |

---

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 22.1.5.

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Vitest](https://vitest.dev/) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
