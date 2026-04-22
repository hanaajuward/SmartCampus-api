# SmartCampus Sensor & Room Management API

A RESTful API built with **JAX-RS (Jersey)** for the University of Westminster 5COSC022W coursework. It manages campus rooms and IoT sensors using in-memory data structures (no database).

---

## API Design Overview

The API follows REST principles with a versioned base path of `/api/v1`. It is structured in three layers:

- **Resource Layer** — JAX-RS classes that handle HTTP routing and responses
- **Service Layer** — `RoomService` contains all business logic and validation
- **Storage Layer** — `DataStore` holds all data in `ConcurrentHashMap` instances

### Resource Hierarchy

```
/api/v1/
├── rooms/
│   ├── GET     → list all rooms
│   ├── POST    → create a room
│   ├── {roomId}/
│   │   ├── GET    → get one room
│   │   └── DELETE → delete room (fails with 409 if sensors exist)
├── sensors/
│   ├── GET     → list all sensors (supports ?type= filter)
│   ├── POST    → register a sensor (validates roomId exists → 422 if not)
│   ├── {sensorId}/
│   │   ├── GET    → get one sensor
│   │   └── readings/          ← sub-resource (Part 4)
│   │       ├── GET   → list readings
│   │       ├── POST  → add reading (fails with 403 if sensor is MAINTENANCE/OFFLINE)
│   │       └── {readingId}/
│   │           └── GET → get one reading
```

### Error Handling Strategy

All errors are handled through JAX-RS `ExceptionMapper` classes. Custom exceptions are thrown in `RoomService` and propagate freely through the resource layer — no try/catch blocks intercept them — so mappers can convert them to structured JSON responses:

| Exception | HTTP Code | Scenario |
|---|---|---|
| `RoomNotEmptyException` | 409 Conflict | Deleting a room that still has sensors |
| `LinkedResourceNotFoundException` | 422 Unprocessable Entity | Creating a sensor with a non-existent roomId |
| `SensorUnavailableException` | 403 Forbidden | Posting a reading to a MAINTENANCE/OFFLINE sensor |
| Any uncaught `Throwable` | 500 Internal Server Error | Unexpected runtime errors |

---

## Build & Run Instructions

### Prerequisites

- Java 17+
- Maven 3.6+
- Apache Tomcat 9.x

### Steps

**1. Clone the repository**

```bash
git clone https://github.com/YOUR_USERNAME/SmartCampusAPI.git
cd SmartCampusAPI
```

**2. Build the WAR file**

```bash
mvn clean package
```

This generates `target/SmartCampusAPI-1.0-SNAPSHOT.war`.

**3. Deploy to Tomcat**

Copy the WAR file to Tomcat's webapps directory:

```bash
cp target/SmartCampusAPI-1.0-SNAPSHOT.war /path/to/tomcat/webapps/
```

Or deploy via the Tomcat Manager at `http://localhost:8080/manager`.

**4. Start Tomcat**

```bash
/path/to/tomcat/bin/startup.sh     # Mac/Linux
/path/to/tomcat/bin/startup.bat    # Windows
```

**5. Verify it's running**

```bash
curl http://localhost:8080/SmartCampusAPI/api/v1/
```

You should see the discovery JSON response.

---

## Sample curl Commands

### 1. Discovery — GET /api/v1/

```bash
curl -X GET http://localhost:8080/SmartCampusAPI/api/v1/ \
  -H "Accept: application/json"
```

**Expected response (200):**
```json
{
  "version": "1.0.0",
  "contact": "smartcampus@university.edu",
  "collections": {
    "rooms": "/api/v1/rooms",
    "sensors": "/api/v1/sensors"
  }
}
```

---

### 2. Create a Room — POST /api/v1/rooms

```bash
curl -X POST http://localhost:8080/SmartCampusAPI/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id": "HALL-001", "name": "Main Lecture Hall", "capacity": 200}'
```

**Expected response (201 Created):**
```json
{
  "id": "HALL-001",
  "name": "Main Lecture Hall",
  "capacity": 200,
  "sensorIds": []
}
```

---

### 3. Delete a Room With Active Sensors — DELETE /api/v1/rooms/LIB-301

```bash
curl -X DELETE http://localhost:8080/SmartCampusAPI/api/v1/rooms/LIB-301
```

**Expected response (409 Conflict) — RoomNotEmptyExceptionMapper:**
```json
{
  "error": "Cannot delete room",
  "message": "Room LIB-301 cannot be deleted because it has 2 active sensor(s)",
  "roomId": "LIB-301",
  "activeSensors": 2
}
```

---

### 4. Filter Sensors by Type — GET /api/v1/sensors?type=CO2

```bash
curl -X GET "http://localhost:8080/SmartCampusAPI/api/v1/sensors?type=CO2" \
  -H "Accept: application/json"
```

**Expected response (200):** Array of only CO2 sensors.

---

### 5. Create Sensor With Non-Existent Room — POST /api/v1/sensors

```bash
curl -X POST http://localhost:8080/SmartCampusAPI/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"type": "CO2", "roomId": "DOES-NOT-EXIST"}'
```

**Expected response (422 Unprocessable Entity) — LinkedResourceNotFoundExceptionMapper:**
```json
{
  "error": "Unprocessable Entity",
  "message": "Room not found with id: DOES-NOT-EXIST",
  "resourceType": "Room",
  "resourceId": "DOES-NOT-EXIST"
}
```

---

### 6. Add Reading to MAINTENANCE Sensor — POST /api/v1/sensors/SENSOR-CO2-003/readings

```bash
curl -X POST http://localhost:8080/SmartCampusAPI/api/v1/sensors/SENSOR-CO2-003/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 100}'
```

**Expected response (403 Forbidden) — SensorUnavailableExceptionMapper:**
```json
{
  "error": "Forbidden",
  "message": "Sensor SENSOR-CO2-003 is currently in MAINTENANCE status and cannot accept new readings",
  "sensorId": "SENSOR-CO2-003",
  "currentStatus": "MAINTENANCE"
}
```

---

### 7. Add Reading to Active Sensor (with side effect) — POST /api/v1/sensors/SENSOR-CO2-001/readings

```bash
curl -X POST http://localhost:8080/SmartCampusAPI/api/v1/sensors/SENSOR-CO2-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 512.3}'
```

**Expected response (201 Created):**
```json
{
  "id": "some-generated-uuid",
  "timestamp": 1745000000000,
  "value": 512.3
}
```

After this, `GET /api/v1/sensors/SENSOR-CO2-001` will show `currentValue: 512.3` — the side effect update.

---

## Conceptual Report (Question Answers)

### Part 1 — JAX-RS Resource Lifecycle

By default, JAX-RS creates a **new instance of each resource class for every incoming HTTP request** (per-request scope). This means instance variables cannot be used to store shared state — each request gets a fresh object. To maintain persistent in-memory data across requests, this project uses the **Singleton pattern** for both `RoomService` and `DataStore`. Both classes use a static instance accessed via `getInstance()`, ensuring all requests read from and write to the same maps. `ConcurrentHashMap` is used instead of `HashMap` to handle concurrent access safely — multiple simultaneous requests can read/write without corrupting data.

### Part 1 — HATEOAS

Hypermedia As The Engine Of Application State (HATEOAS) means including navigational links within API responses. The discovery endpoint demonstrates this by returning links to `/api/v1/rooms` and `/api/v1/sensors` rather than requiring clients to know URLs in advance. This benefits developers because clients can discover the API dynamically, reducing tight coupling to hardcoded URLs. When URL structures change, clients following links adapt automatically without code changes.

### Part 2 — IDs vs Full Objects in List Responses

Returning only IDs (e.g. `["LIB-301", "CS-101"]`) minimises bandwidth but forces the client to make N additional requests to fetch details — the N+1 problem. Returning full room objects (as this API does) costs more bandwidth upfront but saves round-trips. For small collections on a local network, full objects are preferable. For large collections, pagination and sparse fieldsets are the right solution.

### Part 2 — DELETE Idempotency

The DELETE operation **is idempotent** in this implementation. The first call deletes the room and returns 204. Every subsequent call with the same roomId returns 404 (room not found). The server state is identical after the first and all subsequent calls — the room is gone. The response code differs (204 vs 404) but idempotency concerns server state, not response codes, so this is correct behaviour.

### Part 3 — @Consumes and Content-Type Mismatch

`@Consumes(MediaType.APPLICATION_JSON)` declares that the endpoint only accepts `application/json` request bodies. If a client sends `text/plain` or `application/xml`, JAX-RS returns **415 Unsupported Media Type** automatically before the method body even executes. The runtime checks the `Content-Type` header against the `@Consumes` annotation and rejects mismatches.

### Part 3 — @QueryParam vs Path Segment for Filtering

`GET /api/v1/sensors?type=CO2` (query parameter) is superior to `GET /api/v1/sensors/type/CO2` (path segment) for filtering because query parameters are semantically for optional, variable filtering of a collection. Path segments imply a fixed resource hierarchy. Query parameters can be combined (`?type=CO2&status=ACTIVE`) without changing the URL structure. Path-based filtering also pollutes the URL hierarchy and can conflict with actual resource IDs.

### Part 4 — Sub-Resource Locator Benefits

The Sub-Resource Locator pattern keeps each resource class focused on one concern. `SensorResource` handles sensor CRUD; `SensorReadingResource` handles reading operations. Without this pattern, `SensorResource` would grow to contain all reading methods, making it harder to maintain. The pattern also allows the sub-resource to receive context (the `sensorId`) through its constructor, keeping the logic clean. In large APIs with many nested resources, this separation is essential for maintainability.

### Part 5 — HTTP 422 vs 404

A `404 Not Found` means the URL itself points to nothing — the resource at that path does not exist. A `422 Unprocessable Entity` means the request URL is valid, the JSON is well-formed, but the *content* contains a semantic error — specifically a reference to a resource that doesn't exist. When creating a sensor with `roomId: "FAKE-999"`, the URL `/api/v1/sensors` exists and is correct. The problem is inside the payload. Using 422 precisely communicates that the request format is right but the data is logically invalid.

### Part 5 — Stack Trace Security Risk

Exposing Java stack traces to API consumers reveals: fully qualified class names (exposing internal architecture), library names and versions (allowing attackers to look up known CVEs), file paths on the server, and business logic flow. An attacker can use this to craft targeted attacks against specific vulnerable library versions or exploit known weaknesses in the framework. This project's `GlobalExceptionMapper` logs the full trace server-side while returning only a generic message to clients.

### Part 5 — Logging Filters vs Per-Method Logging

Inserting `Logger.info()` in every resource method is a cross-cutting concern — identical logic duplicated dozens of times. If the log format needs to change, every method must be updated. JAX-RS filters implement this once and apply automatically to every request and response. They also run even for requests that never reach a resource method (e.g. 415 errors from content-type mismatches), giving complete observability. This is the same principle as AOP (Aspect-Oriented Programming).

---

## Project Structure

```
src/main/java/com/smartcampus/
├── ApplicationConfig.java          Bootstrap — registers all classes
├── model/
│   ├── Room.java
│   ├── Sensor.java
│   └── SensorReading.java
├── resource/
│   ├── DiscoveryResource.java      GET /api/v1/
│   ├── RoomResource.java           /api/v1/rooms
│   ├── SensorResource.java         /api/v1/sensors
│   └── SensorReadingResource.java  /api/v1/sensors/{id}/readings
├── service/
│   └── RoomService.java            Business logic + exception throwing
├── storage/
│   └── DataStore.java              In-memory ConcurrentHashMaps
├── exception/
│   ├── RoomNotEmptyException.java
│   ├── LinkedResourceNotFoundException.java
│   └── SensorUnavailableException.java
├── mapper/
│   ├── RoomNotEmptyExceptionMapper.java        → 409
│   ├── LinkedResourceNotFoundExceptionMapper.java → 422
│   ├── SensorUnavailableExceptionMapper.java   → 403
│   └── GlobalExceptionMapper.java              → 500
└── filter/
    └── LoggingFilter.java          Logs all requests and responses
```

---

## Technology Stack

- **Java 17**
- **JAX-RS 2.1 (Jersey 2.32)**
- **Jackson** (JSON serialisation via jersey-media-json-jackson)
- **Apache Tomcat 9** (servlet container)
- **Maven** (build tool)

> ⚠️ No Spring Boot. No database. In-memory storage only, as required by the specification.