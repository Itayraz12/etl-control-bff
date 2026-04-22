# etl-control-bff

Spring Boot Backend-for-Frontend (BFF) for ETL Pipeline Studio control flows.

This service exposes REST APIs consumed by a UI to:
- read ETL transformers and filters from JSON config files,
- read backbone entities from JSON,
- save and retrieve pipeline configurations as raw YAML,
- fetch deployment lists by team,
- start deployments and stream real-time progress via SSE,
- return the pipeline JSON Schema.

## Tech Stack

- Java 17
- Spring Boot 3.2
- Spring Web + Actuator
- Springdoc OpenAPI (Swagger UI)
- Jackson JSON + Jackson YAML (`jackson-dataformat-yaml`)
- Lombok
- Gradle (wrapper included)

## Prerequisites

- JDK 17+
- No external database required

## Run

### Windows (PowerShell)

```powershell
Set-Location C:\Users\eyalm\IdeaProjects\etl-control-bff
.\gradlew.bat bootRun
```

### Linux / macOS

```bash
cd etl-control-bff
./gradlew bootRun
```

Defaults after start-up:
| URL | Description |
|---|---|
| `http://localhost:8080` | Base URL |
| `http://localhost:8080/swagger-ui.html` | Swagger UI |
| `http://localhost:8080/v3/api-docs` | OpenAPI JSON |

## CORS

CORS is enabled for:
- Origin: `http://localhost:5173`
- Paths: `/api/**`
- Methods: `GET, POST, PUT, DELETE, OPTIONS`

Configured in `CorsConfig.java`.

---

## Resource Files (Classpath)

| File | Loaded by | Description |
|---|---|---|
| `src/main/resources/transformers.json` | `ConfigService` | List of transformer definitions |
| `src/main/resources/filters.json` | `ConfigService` | List of filter definitions |
| `src/main/resources/entity.json` | `BackboneService` | List of backbone entities |
| `src/main/resources/schema.json` | `BackendService` | JSON Schema (draft-07) for pipeline configuration |

Saved configuration files land in the directory defined by `app.config-storage-dir` (default: `saved-configurations/`).  
File naming convention: `<productType>_<source>_<team>_<environment>.yml`

---

## Data Models

### InputType (enum)

Used by the `Transformer` model to describe how many input fields the transformer accepts.

| Value | Meaning |
|---|---|
| `NONE` | Transformer takes no input field (rare) |
| `SINGLE` | Transformer takes exactly one input field (most common) |
| `MULTI` | Transformer takes multiple input fields |

> Legacy JSON using `"isMultipleInput": true/false` is still accepted on read (`true` → `MULTI`, `false` → `SINGLE`). New JSON must use `"inputType"`.

---

### Transformer
Uses Lombok (`@Getter @Setter @NoArgsConstructor @AllArgsConstructor @ToString`).

| Field | Type | JSON key | Notes |
|---|---|---|---|
| `id` | `UUID` | `_id` | |
| `name` | `String` | `name` | |
| `description` | `String` | `description` | |
| `format` | `String` | `format` | e.g. `"string"`, `"json"` |
| `canonize` | `boolean` | `canonize` | |
| `inputType` | `InputType` | `inputType` | Enum: `NONE`, `SINGLE`, `MULTI`. Legacy `isMultipleInput` boolean is still deserialised. |
| `additionalProperties` | `Map<String, Object>` | `additionalProperties` | Values may be `String` or `List<String>`. The reserved key `_required` holds a `List<String>` of required property keys. |

#### Built-in transformers (`transformers.json`)

| `_id` | `name` | `inputType` | Description | Notable `additionalProperties` |
|---|---|---|---|---|
| `…000001` | `ToTimestamp` | `SINGLE` | Convert date string to timestamp | `format`, `zone` (both required), `output_format` |
| `…000002` | `ConvertMulti` | `MULTI` | Convert multiple fields via logic expression | `logic` (required), `defaultValue`, `case_sensitive` |
| `…000003` | `AddString` | `SINGLE` | Prepend / insert / append a fixed string | `add_value`, `position` (`PREFIX` \| `MIDDLE` \| `SUFFIX`) |
| `…000004` | `LowerCase` | `SINGLE` | Lower-case a field value | — |
| `…000005` | `UpperCase` | `SINGLE` | Upper-case a field value | — |

> `position` for `AddString` is stored as the enum string `PREFIX`, `MIDDLE`, or `SUFFIX`.

---

### Filter
Uses Lombok (`@Getter @Setter @NoArgsConstructor @AllArgsConstructor @ToString`).

| Field | Type | JSON key | Notes |
|---|---|---|---|
| `id` | `String` | `id` | |
| `name` | `String` | `name` | |
| `rule` | `String` | `rule` | Filter expression, e.g. `"severity >= warning"` |
| `include` | `boolean` | `isInclude` | `true` = include record on match; `false` = exclude record on match |

#### Built-in filters (`filters.json`)

| `id` | `name` | `rule` | `isInclude` |
|---|---|---|---|
| `f-1` | Filter 1 | `severity >= warning` | `true` |
| `f-2` | Filter 2 | `source == legacy-system` | `false` |

---

### Entity

| Field | Type | Notes |
|---|---|---|
| `id` | `String` | |
| `name` | `String` | Human-readable entity name |
| `type` | `String` | e.g. `"Order"`, `"Product"` |
| `description` | `String` | |

Entities are loaded at start-up from `entity.json` by `BackboneService`.

---

### Deployment

Returned by the backend deployments endpoint. Populated in-memory (no database).

| Field | Type | Notes |
|---|---|---|
| `id` | `String` | Pipeline record ID |
| `teamName` | `String` | Owning team name, e.g. `Team A` |
| `productType` | `String` | e.g. `"ETL Job"`, `"Analytics"` |
| `productSource` | `String` | e.g. `"GitHub"`, `"Bitbucket"` |
| `deploymentStatus` | `String` | `draft` / `running` / `stopped` |
| `savedVersion` | `String` | Latest saved version |
| `deployedVersion` | `String` | Currently deployed version (may be `null`) |
| `lastStatusChange` | `long` | Epoch ms of last status change |
| `createdAt` | `long` | Epoch ms of creation |
| `environment` | `String` | `development` / `staging` / `production` |

---

### DeployResponse

Returned by `POST /api/backend/deployments/deploy`.

| Field | Type | Notes |
|---|---|---|
| `success` | `boolean` | Always `true` on 200 OK |
| `id` | `String` | Same as `deploymentId` (run UUID) |
| `deploymentId` | `String` | Unique run UUID — use this to open the SSE progress stream |

---

### DeploymentStep

Returned by `GET /api/backend/deployments/steps`.

| Field | Type | Notes |
|---|---|---|
| `id` | `String` | Step identifier, matches `stepId` in SSE events |
| `label` | `String` | Human-readable label shown in the progress modal |

---

### AdminTeam

| Field | Type | Notes |
|---|---|---|
| `id` | `String` | Stable slug derived from `teamName` |
| `teamName` | `String` | Unique team name |
| `devopsName` | `String` | Required DevOps ownership name |
| `createdAt` | `String` | ISO-8601 creation timestamp |
| `updatedAt` | `String` | ISO-8601 last update timestamp |

---

### AdminUser

| Field | Type | Notes |
|---|---|---|
| `id` | `String` | Stable key, aligned with `userId` |
| `userId` | `String` | Unique user identifier |
| `createdAt` | `String` | ISO-8601 creation timestamp |

---

## API Endpoints

### Config API — `/api/config`

| Method | Path | Response | Description |
|---|---|---|---|
| `GET` | `/api/config/transformers` | `List<Transformer>` | Returns all transformers loaded from `transformers.json` |
| `GET` | `/api/config/filters?environment=prod` | `List<Filter>` | Returns all filters loaded from `filters.json` for the requested environment |
| `GET` | `/api/config/streaming-continuities` | `List<{value,label}>` | Returns the UI options for streaming continuity |
| `GET` | `/api/config/records-per-day` | `List<{value,label}>` | Returns the UI options for average records per day |

---

### Backbone API — `/api/backbone`

| Method | Path | Response | Description |
|---|---|---|---|
| `GET` | `/api/backbone/entity/{id}` | `Entity` | Returns a single entity by id. Falls back to `Unknown` values if not found. |
| `GET` | `/api/backbone/entities` | `List<Entity>` | Returns all entities from `entity.json` |

---

### Backend API — `/api/backend`

| Method | Path | Consumes | Produces | Description |
|---|---|---|---|---|
| `GET` | `/api/backend/deployments` or `/api/backend/deployments?teamName=Team%20A` | — | `application/json` | Returns deployments for all teams, or filters by team when `teamName` is provided |
| `GET` | `/api/backend/deployments/steps` | — | `application/json` | Returns the ordered list of deployment steps |
| `POST` | `/api/backend/deployments/deploy` | `text/plain` / `application/json` / `application/x-yaml` | `application/json` | Starts an async deployment run; returns `DeployResponse` with a `deploymentId` |
| `GET` | `/api/backend/deployments/{deploymentId}/progress` | — | `text/event-stream` | SSE stream of real-time step progress |
| `GET` | `/api/backend/configuration/yaml?productType=&source=&team=&environment=` | — | `text/plain` | Returns saved YAML configuration as a string |
| `POST` | `/api/backend/configuration/yaml?productType=&source=&team=&environment=` | `text/plain` | `text/plain` | Saves raw YAML string to disk; returns `"ok"` or `"error: …"` |
| `POST` | `/api/backend/schemaByExample/CSV` or `/api/backend/schemaByExample/JSON` | `text/plain` / `application/json` | `application/json` | Returns a JSON Schema based on the path `formatType` |
| `POST` | `/api/backend/filters/evaluate` | `application/json` | `application/json` | Evaluates a nested filter configuration against input field values and returns `{ "matches": boolean }` |
| `GET` | `/api/backend/schema/entity/{entityName}` | — | `application/json` | Returns the JSON Schema for the named entity |

---

### Admin API — `/api/backend/admin`

All admin endpoints require:

- `X-user-id` request header
- authenticated user context
- admin role

Behavior:

- `401` when user context is missing or unauthenticated
- `403` when the user is authenticated but not admin

| Method | Path | Consumes | Produces | Description |
|---|---|---|---|---|
| `GET` | `/api/backend/admin/teams` | — | `application/json` | Returns all teams for the Team Management page |
| `POST` | `/api/backend/admin/teams` | `application/json` | `application/json` | Creates a team and returns the created entity |
| `PUT` | `/api/backend/admin/teams/{id}` | `application/json` | `application/json` | Updates a team and returns the updated entity |
| `DELETE` | `/api/backend/admin/teams/{id}` | — | `application/json` | Deletes a team and returns `{ "success": true }` |
| `GET` | `/api/backend/admin/users` | — | `application/json` | Returns all users for the User Management page |
| `GET` | `/api/backend/admin/admin-users` | — | `application/json` | Returns admin users as `{ userId, createdDate }` |
| `POST` | `/api/backend/admin/users` | `application/json` | `application/json` | Creates a user and returns the created entity |
| `PUT` | `/api/backend/admin/users/{id}` | `application/json` | `application/json` | Updates a user and returns the updated entity |
| `DELETE` | `/api/backend/admin/users/{id}` | — | `application/json` | Deletes a user and returns `{ "success": true }` |

---

### Auth API — `/api/auth`

| Method | Path | Consumes | Produces | Description |
|---|---|---|---|---|
| `POST` | `/api/auth/login` | `application/json` | `application/json` | Decrypts username/password, returns the mapped team as JSON, and sets the `user-role` response header |

**Login request body**

```json
{
  "username": "<base64-iv>:<base64-ciphertext>",
  "password": "<base64-iv>:<base64-ciphertext>"
}
```

**Supported users**

| Username | Password | JSON response body | `user-role` header |
|---|---|---|---|
| `a` | `a` | `{ "teamName": "Team A" }` | `regular` |
| `b` | `b` | `{ "teamName": "Team B" }` | `regular` |
| `yarden` | `yarden` | `{ "teamName": "team yarden" }` | `admin` |

The login endpoint is excluded from the required `X-user-id` request header. Credential decryption uses the shared AES key configured under `app.auth.encryption-key`.

---

#### Deployment Progress (SSE)

**Flow:**
1. `POST /api/backend/deployments/deploy` — send the pipeline YAML as the request body; receive a `deploymentId`.
2. `GET /api/backend/deployments/{deploymentId}/progress` — open an `EventSource` to receive step events.
3. The stream closes automatically on `deployment-complete` or `deployment-failed` / `step-failed`.

**Step list** (`GET /api/backend/deployments/steps`):

| `id` | `label` |
|---|---|
| `validate-config` | Validating pipeline configuration |
| `prepare-resources` | Preparing Kafka topics |
| `validate-mappings` | Validating field mappings |
| `prepare-flink` | Preparing Flink job |
| `upload-artifacts` | Uploading pipeline artifacts |
| `register-pipeline` | Registering pipeline |
| `deploy-job` | Deploying Flink job |
| `health-checks` | Running health checks |

**SSE events:**

| Event name | Data fields | Emitted when |
|---|---|---|
| `step-start` | `stepIndex`, `stepId`, `label` | A step begins |
| `step-complete` | `stepIndex` | A step finishes successfully |
| `step-failed` | `stepIndex`, `error` | A step fails — stream closes after this |
| `deployment-complete` | `success: true` | All steps passed — stream closes after this |
| `deployment-failed` | `error` | High-level failure (not step-specific) — stream closes after this |

**Example event sequence (success):**
```
event: step-start
data: {"stepIndex":0,"stepId":"validate-config","label":"Validating pipeline configuration"}

event: step-complete
data: {"stepIndex":0}

...

event: deployment-complete
data: {"success":true}
```

**Example event sequence (step failure):**
```
event: step-start
data: {"stepIndex":6,"stepId":"deploy-job","label":"Deploying Flink job"}

event: step-failed
data: {"stepIndex":6,"error":"Flink job deployment failed: cluster unavailable"}
```

> **Race-condition handling:** events emitted between the `POST /deploy` response and the browser opening the SSE connection are buffered internally and flushed immediately when the `EventSource` connects.

---

#### Save / Get Configuration YAML

Files are stored under `app.config-storage-dir` using the naming pattern:

```
<productType>_<source>_<team>_<environment>.yml
```

Each part is lowercased and non-alphanumeric characters are replaced with `-`.

**Example** — file `etl-job_bitbucket_team-a_production.yml`:
```yaml
metadata:
  id: etl-ub9lnj0
  entity: Order
  product_source: Bitbucket
  product_type: ETL Job
  environment: production
  team: Team A
source:
  type: kafka
  format: JSON
  topic: source_products_raw
mappings:
  - src: id
    tgt: name
sink:
  type: kafka
  topic: etl_products_v3
```

**Responses:**
- `POST` save: `200 OK` body `"ok"` on success; `400` body `"error: …"` for bad input; `500` for I/O failure.
- `GET` retrieve: `200 OK` with YAML body; `404` if file does not exist; `500` on I/O failure.

#### Schema Endpoint

`POST /api/backend/schema`

Reads `schema.json` from the classpath and returns it verbatim. The request body (payload) is accepted for future use (e.g. context-aware schema filtering) but is not processed today.

---

## App Properties

From `src/main/resources/application.yml`:

| Property | Default | Description |
|---|---|---|
| `server.port` | `8080` | HTTP port |
| `springdoc.swagger-ui.path` | `/swagger-ui.html` | Swagger UI path |
| `springdoc.api-docs.path` | `/v3/api-docs` | OpenAPI JSON path |
| `app.config-storage-dir` | `saved-configurations` | Directory where YAML configuration files are persisted |

Override via environment variable or JVM system property, e.g.:

```powershell
.\gradlew.bat bootRun --args="--app.config-storage-dir=C:/configs"
```

---

## Build & Test

```powershell
Set-Location C:\Users\eyalm\IdeaProjects\etl-control-bff
.\gradlew.bat clean test
```

Test report: `build/reports/tests/test/index.html`

> If the build fails with a JVM version error, verify that Java 17+ is the active JDK.

---

## Project Structure

```
src/main/
├── java/com/example/
│   ├── ApiServiceApplication.java
│   ├── config/
│   │   └── CorsConfig.java
│   ├── controller/
│   │   ├── BackboneController.java      # /api/backbone
│   │   ├── BackendController.java       # /api/backend (kafka, config YAML, schema)
│   │   ├── ConfigController.java        # /api/config
│   │   └── DeploymentController.java    # /api/backend/deployments (deploy + SSE progress)
│   ├── model/
│   │   ├── Deployment.java
│   │   ├── DeploymentStep.java
│   │   ├── DeployResponse.java
│   │   ├── Entity.java
│   │   ├── Filter.java
│   │   ├── InputType.java               # enum: NONE | SINGLE | MULTI
│   │   └── Transformer.java
│   └── service/
│       ├── BackboneService.java         # loads entity.json
│       ├── BackendService.java          # saves/loads YAML configs, returns schema
│       ├── ConfigService.java           # loads transformers.json & filters.json
│       ├── DeployProgressService.java   # async deployment execution + SSE emitter registry
│       └── StepException.java          # per-step failure exception
└── resources/
    ├── application.yml
    ├── entity.json
    ├── filters.json
    ├── schema.json
    └── transformers.json
```
