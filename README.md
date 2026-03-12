# etl-control-bff

Spring Boot Backend-for-Frontend (BFF) for ETL Pipeline Studio control flows.

This service exposes REST APIs consumed by a UI to:
- read ETL transformers and filters from JSON config files,
- read backbone entities from JSON,
- save and retrieve pipeline configurations as raw YAML,
- fetch deployment lists by team,
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

### Transformer
Uses Lombok (`@Getter @Setter @NoArgsConstructor @AllArgsConstructor @ToString`).

| Field | Type | JSON key | Notes |
|---|---|---|---|
| `id` | `UUID` | `_id` | |
| `name` | `String` | `name` | |
| `description` | `String` | `description` | |
| `format` | `String` | `format` | e.g. `"string"`, `"json"` |
| `canonize` | `boolean` | `canonize` | |
| `multipleInput` | `boolean` | `isMultipleInput` | |
| `additionalProperties` | `Map<String, Object>` | `additionalProperties` | Values may be `String` or `List<String>`. The reserved key `_required` holds a `List<String>` of required property keys. |

#### Built-in transformers (`transformers.json`)

| `_id` | `name` | Description | Notable `additionalProperties` |
|---|---|---|---|
| `…000001` | `ToTimestamp` | Convert date string to timestamp | `format`, `zone` (both required), `output_format` |
| `…000002` | `ConvertMulti` | Convert multiple fields via logic expression | `logic` (required), `defaultValue`, `case_sensitive` |
| `…000003` | `AddString` | Prepend / insert / append a fixed string | `add_value` (string to add), `position` (`PREFIX` \| `MIDDLE` \| `SUFFIX`) |

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

| Field | Type |
|---|---|
| `id` | `String` |
| `name` | `String` |
| `source` | `String` |
| `status` | `String` (`draft` / `running` / `stopped`) |
| `currentVersion` | `String` |
| `deployedVersion` | `String` |
| `lastModified` | `long` (epoch ms) |
| `createdAt` | `long` (epoch ms) |

---

## API Endpoints

### Config API — `/api/config`

| Method | Path | Response | Description |
|---|---|---|---|
| `GET` | `/api/config/transformers` | `List<Transformer>` | Returns all transformers loaded from `transformers.json` |
| `GET` | `/api/config/filters` | `List<Filter>` | Returns all filters loaded from `filters.json` |

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
| `GET` | `/api/backend/deployments?teamName=Team%20A` | — | `application/json` | Returns deployments for the given team |
| `GET` | `/api/backend/configuration/yaml?productType=&source=&team=&environment=` | — | `text/plain` | Returns saved YAML configuration as a string |
| `POST` | `/api/backend/configuration/yaml?productType=&source=&team=&environment=` | `text/plain` | `text/plain` | Saves raw YAML string to disk; returns `"ok"` or `"error: …"` |
| `POST` | `/api/backend/schema` | `text/plain` / `application/json` | `application/json` | Returns contents of `schema.json` (pipeline JSON Schema). Payload is accepted but currently unused. |

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
│   │   ├── BackboneController.java   # /api/backbone
│   │   ├── BackendController.java    # /api/backend
│   │   └── ConfigController.java    # /api/config
│   ├── model/
│   │   ├── Deployment.java
│   │   ├── Entity.java
│   │   ├── Filter.java
│   │   └── Transformer.java
│   └── service/
│       ├── BackboneService.java      # loads entity.json
│       ├── BackendService.java       # saves/loads YAML configs, returns schema
│       └── ConfigService.java        # loads transformers.json & filters.json
└── resources/
    ├── application.yml
    ├── entity.json
    ├── filters.json
    ├── schema.json
    └── transformers.json
```
