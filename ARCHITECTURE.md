# NFPL Access Control — architecture

```mermaid
flowchart TD
  U[Employee / Client / Admin] --> UI[Thymeleaf UI]
  U --> API[REST API / Swagger]
  UI --> SEC[Spring Security: Form Login + Authorization]
  API --> SEC
  SEC --> MVC[UI Controller / REST Controller]
  MVC --> AS[AccessService]
  MVC --> JS[JobService]
  JS --> AS
  AS --> PR[Permission Grant Repository]
  AS --> UR[Staff User Repository]
  JS --> JR[Job Repository]
  AS --> AR[Audit Repository]
  JS --> AR
  PR --> DB[(H2 local / PostgreSQL hosted)]
  UR --> DB
  JR --> DB
  AR --> DB
```

## Main browser routes
- `/login`: Spring Security form login
- `/dashboard`: visible-job summary and current account status
- `/jobs`: visible jobs and workflow action forms
- `/access-check`: action authorization with reason code and explanation
- `/admin/audit`: admin-only audit log
- `/admin/grants-ui`: admin-only users and grants overview, plus absence toggle

## Request flow
1. Spring Security authenticates the user.
2. A controller receives a page or API request.
3. `AccessService` evaluates user status, grants, scope, job stage, and maker/checker separation.
4. The service returns an allow/deny decision with a reason.
5. Access checks and completed workflow transitions are recorded in `AuditEvent`.

The diagram represents the intended logical design. The project remains an assignment prototype and needs the production hardening items listed in `README.md`.
