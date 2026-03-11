# etl-control-bff

Spring Boot Backend-for-Frontend (BFF) for ETL control flows.

This service exposes REST APIs used by a UI to:
- read ETL transformers and filters from JSON config files,
- read backbone entities from JSON,
- save/get configuration objects,
- save configuration from YAML,
- fetch deployment lists by team.

## Tech Stack

- Java 17
- Spring Boot 3.2
- Spring Web + Actuator
- Springdoc OpenAPI (Swagger UI)
- Jackson JSON + Jackson YAML
- Lombok
- Gradle (wrapper included)

## Prerequisites

- JDK 17+
- No external database is required for current behavior

## Run

### Windows (PowerShell)

```powershell
Set-Location C:\Users\eyalm\IdeaProjects\etl-control-bff
.\gradlew.bat bootRun
```

### Linux/macOS

```bash
cd etl-control-bff
./gradlew bootRun
```

App defaults:
- Base URL: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## CORS

CORS is enabled for UI origin:
- `http://localhost:5173`

Configured for `/api/**` with methods `GET, POST, PUT, DELETE, OPTIONS`.

## Configuration Files (Classpath Resources)

| File | Loaded by | Description |
|---|---|---|
| `src/main/resources/transformers.json` | `ConfigService#getTransforers()` | List of transformer definitions |
| `src/main/resources/filters.json` | `ConfigService#getFilters()` | List of filter definitions |
| `src/main/resources/entity.json` | `BackboneService#getEntity(String id)` | List of backbone entities |

## Data Models

### Transformer

| Field | Type | JSON key | Notes |
|---|---|---|---|
| `id` | `UUID` | `_id` | |
| `name` | `String` | `name` | |
| `description` | `String` | `description` | |
| `format` | `String` | `format` | |
| `canonize` | `boolean` | `canonize` | |
| `multipleInput` | `boolean` | `isMultipleInput` | |
| `additionalProperties` | `Map<String, Object>` | `additionalProperties` | Values can be `String` or `List<String>` (e.g. `_required` holds a string array) |

### Filter

| Field | Type | JSON key | Notes |
|---|---|---|---|
| `id` | `String` | `id` | |
| `name` | `String` | `name` | |
| `rule` | `String` | `rule` | |
| `include` | `boolean` | `isInclude` | `true` = include on match, `false` = exclude on match |

### Entity

| Field | Type | Notes |
|---|---|---|
| `id` | `String` | |
| `name` | `String` | |
| `type` | `String` | |
| `description` | `String` | |

### Configuration

| Field | Type | Notes |
|---|---|---|
| `id` | `String` | Auto-generated on save |
| `name` | `String` | |
| `settings` | `String` | |

## API Endpoints

### Config APIs  `/api/config`

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/config/transforers` | Returns all transformers from `transformers.json` |
| `GET` | `/api/config/filters` | Returns all filters from `filters.json` |

> Note: `/transforers` path spelling is preserved for backward compatibility.

### Backbone APIs  `/api/backbone`

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/backbone/entity/{id}` | Returns entity by id from `entity.json`. Falls back to `Unknown` values if not found. |

### Backend APIs  `/api/backend`

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/backend/configuration` | Saves a `Configuration` object |
| `GET` | `/api/backend/configuration/{id}` | Returns a configuration by id |
| `GET` | `/api/backend/deployments?teamName=Team%20A` | Returns deployments for a given team |
| `POST` | `/api/backend/configuration/yaml` | Parses a YAML string into `Configuration` and persists to disk |

#### Save Configuration from YAML

- Consumes: `text/plain`, `application/x-yaml`, `application/yaml`
- Parses YAML body into a `Configuration` object using `ObjectMapper(YAMLFactory)`
- Persists the result as a `.yml` file under `app.config-storage-dir`
- Returns `400 Bad Request` on invalid YAML, `500` on file write failure

## App Properties

From `src/main/resources/application.yml`:

| Property | Default | Description |
|---|---|---|
| `server.port` | `8080` | HTTP port |
| `springdoc.swagger-ui.path` | `/swagger-ui.html` | Swagger UI path |
| `springdoc.api-docs.path` | `/v3/api-docs` | OpenAPI JSON path |
| `app.config-storage-dir` | `saved-configurations` | Directory for persisted YAML configuration files |

Override via environment variables or JVM system properties.

## Build and Test

```powershell
Set-Location C:\Users\eyalm\IdeaProjects\etl-control-bff
.\gradlew.bat clean test
```

If build fails with a JVM version error, verify Java 17+ is active.
