# Kafka Simulator API — Production Usage

## Environment Setup

Ensure you have Kafka running and update `application.yml` with the correct broker addresses:

```yaml
kafka:
  environments:
    HOME: localhost:9092
    OFFICE: kafka.office.internal:9092
    HOME1: localhost:9092
    OFFICE1: localhost:9092
```

---

## Test Connection to Kafka (Verify Topic Exists)

**Endpoint:** `POST /api/simulator/kafka/test-connection`

This endpoint connects to the real Kafka broker for the given environment and verifies the topic exists.

### Request

```bash
curl -X POST http://localhost:8080/api/simulator/kafka/test-connection \
  -H "Content-Type: application/json" \
  -H "X-User-Id: my-user" \
  -d '{
    "environment": "HOME1",
    "topic": "test__topic"
  }'
```

### Success Response (200 OK)

```json
{
  "success": true,
  "message": "Connection successful and topic exists"
}
```

### Error Responses

**Topic not found (404 Not Found):**
```json
{
  "message": "Topic not found: test__topic"
}
```

**Unknown environment (400 Bad Request):**
```json
{
  "message": "Unknown Kafka environment: INVALID_ENV"
}
```

**Kafka broker unreachable (500 Internal Server Error):**
```json
{
  "message": "Failed to connect to Kafka broker 'localhost:9092' or list topics: Connection refused. Check that (1) the broker address is correct, (2) the broker is running and reachable, and (3) network connectivity is available."
}
```

---

## Start Simulation Task

**Endpoint:** `POST /api/simulator/kafka/start`

Once you've verified the topic exists with `/test-connection`, use this to publish synthetic messages.

### Request

```bash
curl -X POST http://localhost:8080/api/simulator/kafka/start \
  -H "Content-Type: application/json" \
  -H "X-User-Id: my-user" \
  -d '{
    "environment": "HOME1",
    "topic": "test__topic",
    "messageFormat": "json",
    "sampleMessage": "{\n  \"id\": \"{{uuid}}\",\n  \"timestamp\": \"{{now}}\",\n  \"value\": \"{{value}}\"\n}",
    "messagesPerSecond": 1,
    "totalMessages": 10,
    "intervalSeconds": 1
  }'
```

### Success Response (200 OK)

```json
{
  "taskId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "running",
  "startedAt": "2026-04-22T10:35:00.000Z"
}
```

---

## List All Tasks

```bash
curl http://localhost:8080/api/simulator/kafka/tasks \
  -H "X-User-Id: my-user"
```

Response:
```json
[
  {
    "taskId": "550e8400-e29b-41d4-a716-446655440000",
    "environment": "HOME1",
    "topic": "test__topic",
    "messageFormat": "json",
    "messagesPerSecond": 1,
    "totalMessages": 10,
    "intervalSeconds": 1,
    "status": "running",
    "sentCount": 5,
    "startedAt": "2026-04-22T10:35:00.000Z",
    "stoppedAt": null
  }
]
```

---

## Stop a Task

```bash
curl -X POST http://localhost:8080/api/simulator/kafka/stop/550e8400-e29b-41d4-a716-446655440000 \
  -H "X-User-Id: my-user"
```

---

## Delete a Task

```bash
curl -X DELETE http://localhost:8080/api/simulator/kafka/tasks/550e8400-e29b-41d4-a716-446655440000 \
  -H "X-User-Id: my-user"
```

---

## How It Works

1. **test-connection**: Uses Kafka `AdminClient` to connect to the broker and list topics in real-time.
2. **start**: Begins publishing messages in bursts (async background task).
3. **Placeholder substitution**: Each message gets:
   - `{{uuid}}` → unique UUID v4
   - `{{value}}` → random int 1–1000
   - `{{now}}` → current ISO-8601 UTC timestamp

---

## Kafka Broker Quick Start (Docker)

To run a local Kafka broker for testing:

```bash
docker run -d \
  --name kafka \
  -p 9092:9092 \
  -e KAFKA_BROKER_ID=1 \
  -e KAFKA_ZOOKEEPER_CONNECT=localhost:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  confluentinc/cp-kafka:latest
```

Create a test topic:

```bash
docker exec kafka kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic test__topic \
  --partitions 1 \
  --replication-factor 1
```

