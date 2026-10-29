# NFPL Access Control — Spring Boot + Thymeleaf UI

A local assignment prototype for managing shipment-job visibility, workflow actions, access explanations, and audit events. The browser UI is built with **Thymeleaf server-rendered templates** and responsive CSS; the REST API and Swagger UI remain available.

## Requirements
- JDK 21
- Maven 3.9+
- Internet access for Maven to download dependencies on the first build

## Run locally
1. Extract the ZIP.
2. Open the `nfpl-access-control` folder in IntelliJ IDEA (the folder containing `pom.xml`).
3. Select JDK 21 as the project SDK.
4. Run `mvn spring-boot:run` in the project folder, or run `AccessControlApplication.java`.
5. Visit **http://localhost:8080**. You will be redirected to the login page.

The app uses a local H2 file database at `./data/nfpldb` by default. Demo records are seeded only when the database has no users. Delete the local `data` folder if you want to reset the demo database.

## Database and sample data

**Records are stored in a database, not kept only in Java variables.** By default the app uses a persistent, file-based H2 database at `./data/nfpldb`; JPA repositories read and write users, jobs, permission grants, and audit events there. The small demo seed in `SeedData.java` is hardcoded sample data that is inserted only when the database has no users. Once inserted, the records live in the database. For hosted deployments, `application-prod.properties` configures PostgreSQL through environment variables.

### Generate the required scale dataset (500 users + 200,000 jobs)

Use a separate database so you do not mix the large synthetic dataset with the small demo. From the project folder run:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=scale
```

The `scale` profile uses `./data/nfpl-scale` and sets `app.dataset=scale`. On first startup it generates 500 users, scoped permission grants, and 200,000 jobs in batches of 1,000. Generation is deterministic for the job scope/stage distribution. It uses five branches, ten clients, both directions, all three shipment modes, and multiple workflow stages. It prints progress every 10,000 jobs. All generated accounts use the development-only password `ScaleDemo123!`; do not expose this dataset or password publicly.

If the scale database already contains users, the generator safely skips rather than duplicating records. To regenerate from scratch, stop the app and delete only the `data/nfpl-scale` database files, then rerun the command. The normal `mvn spring-boot:run` command continues to use the small sample dataset and `./data/nfpldb`.

**Scale note:** this generator creates the dataset; it does not by itself prove that every list endpoint is performant or permission-correct at 200,000 jobs. Database-level permission filtering, pagination, and load testing remain necessary before claiming the scale criterion is met.

## Demo accounts
These credentials are for local development only:

| Account | Username | Password | Purpose |
|---|---|---|---|
| Administrator | `admin` | `Admin123!` | Audit trail, user availability, admin pages |
| Maker | `rahul` | `Rahul123!` | Prepare jobs across the demo scope |
| Checker | `priya` | `Priya123!` | Check jobs within the ABC / Mumbai / Import scope |
| Client viewer | `clientabc` | `Client123!` | View permitted ABC jobs |

Do not use these passwords in a public deployment.

## Thymeleaf pages
- `/login` — sign in
- `/dashboard` — access-aware summary and recent visible jobs
- `/jobs` — job register with workflow action buttons
- `/access-check` — test an action and see the allow/deny reason
- `/admin/audit` — admin-only audit trail, optionally filtered by job ID
- `/admin/grants-ui` — admin-only view of users and grants; mark demo users absent/present
- `/swagger-ui/index.html` — REST API documentation

## API examples
- `GET /api/me`
- `GET /api/jobs?page=0&size=20`
- `GET /api/jobs/{id}`
- `POST /api/access/check` body: `{"jobId":1,"action":"CHECK"}`
- `POST /api/jobs/{id}/transition` body: `{"action":"PREPARE","reason":"Starting work"}`
- `POST /api/jobs` (admin only)
- `GET /api/audit?jobId=1` (admin only)
- `GET /api/admin/grants` (admin only)
- `POST /api/admin/grants` (admin only)
- `POST /api/admin/users/{username}/absence?absent=true` (admin only)

Supported actions: `VIEW`, `LIST`, `PREPARE`, `CHECK`, `FILE`, `CANCEL`, `REOPEN`, `UPDATE`.

## UI implementation notes
- `src/main/java/com/nfpl/access/ui/UiController.java` handles browser routes and form submissions.
- `src/main/resources/templates/` contains Thymeleaf pages and shared navigation.
- `src/main/resources/static/css/app.css` contains the responsive styling.
- Spring Security supports browser form login and HTTP Basic for API clients.

## Important security note
This is an educational starter, not production-ready security software. CSRF is currently disabled to keep the existing REST and form examples simple. Before production, enable CSRF protection and add tokens to every form. Also review demo credentials, client-user data isolation, admin privilege-escalation policy, audit immutability, required transition reasons, parser service-account restrictions, and database-level filtering for 200,000 jobs. The current list filtering is intended only for the small sample dataset. Run the automated tests in an environment with Maven available before submitting.

## Hosted deployment
Set `SPRING_PROFILES_ACTIVE=prod`, `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD` on your hosting provider. `DATABASE_URL` must be a JDBC PostgreSQL URL such as `jdbc:postgresql://HOST:5432/DBNAME`. Set `PORT` if required by your host. Never publicly deploy with the seeded demo passwords.
