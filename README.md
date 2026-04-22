# Smart Campus API

> **Module:** 5COSC022W Client-Server Architectures — University of Westminster  
> **Author:** Hanaa Ajuward  
> **Technology:** JAX-RS (Jersey 2.32) · Java 17 · Apache Tomcat 9.0 · Maven

A RESTful API for managing university campus rooms and IoT sensors. Facilities managers and automated building systems can register rooms, deploy sensors, and record historical readings through a clean, versioned HTTP interface built entirely with JAX-RS. All data is stored in-memory using `ConcurrentHashMap` with no database required.

---

## 1. API Design Overview

The API follows REST principles with a versioned base path of `/api/v1`. It is structured into five layers:

```
CLIENT (Postman / curl)
        |
        v
FILTER LAYER        - LoggingFilter logs every request and response
        |
        v
RESOURCE LAYER      - DiscoveryResource, RoomResource, SensorResource, SensorReadingResource
        |                 Handle HTTP routing, parse input, build responses
        v
SERVICE LAYER       - RoomService (Singleton)
        |                 All business logic, validation, exception throwing
        v
STORAGE LAYER       - DataStore (Singleton)
                          Three ConcurrentHashMaps: rooms, sensors, sensorReadings

If an exception is thrown in the service layer:
        |
        v
EXCEPTION MAPPER LAYER - JAX-RS intercepts and routes to matching ExceptionMapper
                          Returns structured JSON with the correct HTTP status code
```

### Complete Endpoint Map

```
GET    /api/v1/                                    Discovery
GET    /api/v1/rooms                               List all rooms
POST   /api/v1/rooms                               Create a room
GET    /api/v1/rooms/{roomId}                      Get room by ID
DELETE /api/v1/rooms/{roomId}                      Delete room (409 if sensors exist)
GET    /api/v1/sensors                             List all sensors (?type= filter)
POST   /api/v1/sensors                             Register sensor (422 if roomId invalid)
GET    /api/v1/sensors/{sensorId}                  Get sensor by ID
GET    /api/v1/sensors/{sensorId}/readings         List all readings for sensor
POST   /api/v1/sensors/{sensorId}/readings         Add reading (403 if MAINTENANCE/OFFLINE)
GET    /api/v1/sensors/{sensorId}/readings/{id}    Get specific reading
```

---

## 2. Technology Stack

| Component | Version |
|---|---|
| Java | 17 |
| JAX-RS (Jersey) | 2.32 |
| Jackson (JSON) | via jersey-media-json-jackson 2.32 |
| Apache Tomcat | 9.0 |
| Maven | 3.6+ |

---

## 3. Prerequisites

- **Java 17** - verify with `java -version`
- **Apache Maven 3.6+** - verify with `mvn -version`
- **Apache Tomcat 9.0** - download from [tomcat.apache.org](https://tomcat.apache.org)
- Any IDE (NetBeans recommended)

---

## 4. How to Build and Launch the Server

### Step 1 - Clone the repository

```bash
git clone https://github.com/hanaajuward/SmartCampusAPI.git
cd SmartCampusAPI
```

### Step 2 - Build the WAR file

```bash
mvn clean package
```

This generates: `target/SmartCampusAPI-1.0-SNAPSHOT.war`

Maven downloads all dependencies automatically on the first build.

### Step 3 - Deploy to Tomcat

Copy the WAR to Tomcat's webapps directory. The filename you use here determines your base URL:

```bash
# Mac/Linux - serves at http://localhost:8080/api/v1/
cp target/SmartCampusAPI-1.0-SNAPSHOT.war /path/to/apache-tomcat-9.0/webapps/ROOT.war

# Windows - same result
copy target\SmartCampusAPI-1.0-SNAPSHOT.war C:\tomcat\webapps\ROOT.war
```

### Step 4 - Start Tomcat

```bash
# Mac/Linux
/path/to/apache-tomcat-9.0/bin/startup.sh

# Windows
C:\tomcat\bin\startup.bat
```

Watch for: `INFO: Server startup in [X] milliseconds`

### Step 5 - Verify it is running

```bash
curl http://localhost:8080/api/v1/
```

Expected output:
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

### Step 6 - Stop Tomcat

```bash
# Mac/Linux
/path/to/apache-tomcat-9.0/bin/shutdown.sh

# Windows
C:\tomcat\bin\shutdown.bat
```

> **Important:** All data is in-memory only. Restarting Tomcat resets everything back to the pre-loaded sample data.

### Alternative - NetBeans IDE

1. Open project in NetBeans
2. Right-click project → **Clean and Build**
3. Configure Tomcat 9 server under Tools → Servers if not already set up
4. Right-click project → **Run** - NetBeans deploys and opens the browser automatically

---

## 5. Pre-Loaded Sample Data

The `DataStore` is pre-populated on startup for immediate testing:

### Rooms

| ID | Name | Capacity | Assigned Sensors |
|---|---|---|---|
| `LIB-301` | Library Quiet Study | 50 | SENSOR-CO2-001, SENSOR-TEMP-001 |
| `CS-101` | Computer Science Lab | 35 | *(none - safe to delete)* |
| `ENG-202` | Engineering Workshop | 40 | SENSOR-CO2-003 |

### Sensors

| ID | Type | Status | Current Value | Room |
|---|---|---|---|---|
| `SENSOR-CO2-001` | CO2 | ACTIVE | 420.5 | LIB-301 |
| `SENSOR-TEMP-001` | Temperature | ACTIVE | 22.5 | LIB-301 |
| `SENSOR-CO2-003` | CO2 | MAINTENANCE | 0 | ENG-202 |

### Quick Test Reference

| Scenario | Expected Result |
|---|---|
| `DELETE /rooms/CS-101` | 204 - no sensors, succeeds |
| `DELETE /rooms/LIB-301` | 409 - has 2 sensors, blocked |
| `DELETE /rooms/ENG-202` | 409 - has 1 sensor, blocked |
| `POST /sensors/SENSOR-CO2-001/readings` | 201 - ACTIVE, succeeds |
| `POST /sensors/SENSOR-CO2-003/readings` | 403 - MAINTENANCE, blocked |
| `GET /sensors?type=CO2` | Returns SENSOR-CO2-001 and SENSOR-CO2-003 |
| `POST /sensors` with valid roomId | 201 - succeeds |
| `POST /sensors` with non-existent roomId | 422 - blocked |

---

## 6. Project Structure

```
SmartCampusAPI/
├── pom.xml
├── README.md
└── src/main/java/com/smartcampus/
    ├── ApplicationConfig.java              JAX-RS bootstrap - registers all classes
    ├── model/
    │   ├── Room.java                       id, name, capacity, sensorIds[]
    │   ├── Sensor.java                     id, type, status, currentValue, roomId
    │   └── SensorReading.java              id, timestamp, value
    ├── resource/
    │   ├── DiscoveryResource.java          GET /api/v1/
    │   ├── RoomResource.java               CRUD for /api/v1/rooms
    │   ├── SensorResource.java             CRUD + sub-resource locator
    │   └── SensorReadingResource.java      Sub-resource: /sensors/{id}/readings
    ├── service/
    │   └── RoomService.java                Singleton - all business logic
    ├── storage/
    │   └── DataStore.java                  Singleton - ConcurrentHashMaps + sample data
    ├── exception/
    │   ├── RoomNotEmptyException.java      thrown → 409
    │   ├── LinkedResourceNotFoundException.java    thrown → 422
    │   └── SensorUnavailableException.java thrown → 403
    ├── mapper/
    │   ├── RoomNotEmptyExceptionMapper.java
    │   ├── LinkedResourceNotFoundExceptionMapper.java
    │   ├── SensorUnavailableExceptionMapper.java
    │   └── GlobalExceptionMapper.java      catch-all → 500
    └── filter/
        └── LoggingFilter.java              logs all requests and responses
```

---

## 7. Part 1 - Service Architecture & Discovery

### Discovery Endpoint

**`GET /api/v1/`** - Returns API metadata and navigational links to all resource collections.

```bash
curl -X GET http://localhost:8080/api/v1/
```

**Response 200 OK:**
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

## 8. Part 2 - Room Management

### Endpoints

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/rooms` | List all rooms |
| POST | `/api/v1/rooms` | Create a new room |
| GET | `/api/v1/rooms/{roomId}` | Get room by ID |
| DELETE | `/api/v1/rooms/{roomId}` | Delete a room (blocked if sensors exist) |

### curl Commands

**1. GET all rooms**
```bash
curl -X GET http://localhost:8080/api/v1/rooms
```

**2. GET room by ID**
```bash
curl -X GET http://localhost:8080/api/v1/rooms/LIB-301
```

**3. POST - create new room**
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id": "HALL-001", "name": "Main Lecture Hall", "capacity": 200}'
```

**4. DELETE - empty room (succeeds)**
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/CS-101
```

**5. DELETE - room with sensors (blocked - triggers 409)**
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

**6. GET - non-existent room (triggers 404)**
```bash
curl -X GET http://localhost:8080/api/v1/rooms/FAKE-999
```

### Sample Responses

**GET /rooms - 200 OK:**
```json
[
  {
    "id": "LIB-301",
    "name": "Library Quiet Study",
    "capacity": 50,
    "sensorIds": ["SENSOR-CO2-001", "SENSOR-TEMP-001"]
  },
  {
    "id": "CS-101",
    "name": "Computer Science Lab",
    "capacity": 35,
    "sensorIds": []
  },
  {
    "id": "ENG-202",
    "name": "Engineering Workshop",
    "capacity": 40,
    "sensorIds": ["SENSOR-CO2-003"]
  }
]
```

**POST /rooms - 201 Created:**
```json
{
  "id": "HALL-001",
  "name": "Main Lecture Hall",
  "capacity": 200,
  "sensorIds": []
}
```

**DELETE room with sensors - 409 Conflict:**
```json
{
  "error": "Cannot delete room",
  "message": "Room LIB-301 cannot be deleted because it has 2 active sensor(s)",
  "roomId": "LIB-301",
  "activeSensors": 2
}
```

**DELETE empty room - 204 No Content:** *(empty body)*

**GET non-existent room - 404 Not Found:**
```json
{ "error": "Room not found" }
```

---

## 9. Part 3 - Sensor Operations & Linking

### Endpoints

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/sensors` | List all sensors (optional `?type=` filter) |
| POST | `/api/v1/sensors` | Register a sensor (validates roomId exists) |
| GET | `/api/v1/sensors/{sensorId}` | Get sensor by ID |

### curl Commands

**1. GET all sensors**
```bash
curl -X GET http://localhost:8080/api/v1/sensors
```

**2. GET sensors filtered by type**
```bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=CO2"
```

**3. POST - create sensor with valid roomId**
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"type": "CO2", "status": "ACTIVE", "currentValue": 420.5, "roomId": "LIB-301"}'
```

**4. GET sensor by ID**
```bash
curl -X GET http://localhost:8080/api/v1/sensors/SENSOR-CO2-001
```

**5. POST - invalid roomId (triggers 422)**
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"type": "CO2", "status": "ACTIVE", "currentValue": 100, "roomId": "INVALID-ROOM"}'
```

**6. GET sensors filtered by Temperature**
```bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=Temperature"
```

### Sample Responses

**GET /sensors - 200 OK:**
```json
[
  {
    "id": "SENSOR-CO2-001",
    "type": "CO2",
    "status": "ACTIVE",
    "currentValue": 420.5,
    "roomId": "LIB-301"
  },
  {
    "id": "SENSOR-TEMP-001",
    "type": "Temperature",
    "status": "ACTIVE",
    "currentValue": 22.5,
    "roomId": "LIB-301"
  },
  {
    "id": "SENSOR-CO2-003",
    "type": "CO2",
    "status": "MAINTENANCE",
    "currentValue": 0.0,
    "roomId": "ENG-202"
  }
]
```

**GET /sensors?type=CO2 - 200 OK:**
```json
[
  {
    "id": "SENSOR-CO2-001",
    "type": "CO2",
    "status": "ACTIVE",
    "currentValue": 420.5,
    "roomId": "LIB-301"
  },
  {
    "id": "SENSOR-CO2-003",
    "type": "CO2",
    "status": "MAINTENANCE",
    "currentValue": 0.0,
    "roomId": "ENG-202"
  }
]
```

**POST /sensors - 201 Created:**
```json
{
  "id": "generated-uuid-here",
  "type": "CO2",
  "status": "ACTIVE",
  "currentValue": 420.5,
  "roomId": "LIB-301"
}
```

**POST with invalid roomId - 422 Unprocessable Entity:**
```json
{
  "error": "Unprocessable Entity",
  "message": "Room not found with id: INVALID-ROOM",
  "resourceType": "Room",
  "resourceId": "INVALID-ROOM"
}
```

---

## 10. Part 4 - Deep Nesting with Sub-Resources

### Overview

Each sensor maintains a full timestamped history of all measurements. Readings are accessed through a nested sub-resource under each sensor. When a reading is successfully posted, `currentValue` on the parent sensor is automatically updated as a side effect, ensuring data consistency across the API.

### Sub-Resource Locator Pattern

In `SensorResource.java`, a sub-resource locator method delegates all `/readings` paths to `SensorReadingResource`:

```java
@Path("/{sensorId}/readings")
public SensorReadingResource getSensorReadingResource(@PathParam("sensorId") String sensorId) {
    return new SensorReadingResource(sensorId);
}
```

JAX-RS calls this method first to get a `SensorReadingResource` instance with the `sensorId` injected through the constructor, then invokes the correct HTTP method on that instance.

### Endpoints

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/sensors/{sensorId}/readings` | Get all readings for a sensor |
| POST | `/api/v1/sensors/{sensorId}/readings` | Add a new reading |
| GET | `/api/v1/sensors/{sensorId}/readings/{readingId}` | Get a specific reading by ID |

### curl Commands

**1. GET all readings for a sensor (initially empty)**
```bash
curl -X GET http://localhost:8080/api/v1/sensors/SENSOR-CO2-001/readings
```

**2. POST a new reading**
```bash
curl -X POST http://localhost:8080/api/v1/sensors/SENSOR-CO2-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 430.5}'
```

**3. POST another reading to build history**
```bash
curl -X POST http://localhost:8080/api/v1/sensors/SENSOR-CO2-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 445.2}'
```

**4. GET all readings again (shows history)**
```bash
curl -X GET http://localhost:8080/api/v1/sensors/SENSOR-CO2-001/readings
```

**5. GET a specific reading by ID**
```bash
curl -X GET http://localhost:8080/api/v1/sensors/SENSOR-CO2-001/readings/{readingId}
```

**6. Verify side effect - currentValue should now be 445.2**
```bash
curl -X GET http://localhost:8080/api/v1/sensors/SENSOR-CO2-001
```

**7. POST to a MAINTENANCE sensor (triggers 403)**
```bash
curl -X POST http://localhost:8080/api/v1/sensors/SENSOR-CO2-003/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 100}'
```

**8. GET readings for non-existent sensor (triggers 404)**
```bash
curl -X GET http://localhost:8080/api/v1/sensors/INVALID-SENSOR/readings
```

### Sample Responses

**GET /sensors/{sensorId}/readings - 200 OK:**
```json
[
  {
    "id": "reading-uuid-001",
    "timestamp": 1734567890123,
    "value": 430.5
  },
  {
    "id": "reading-uuid-002",
    "timestamp": 1734567890456,
    "value": 445.2
  }
]
```

**POST /sensors/{sensorId}/readings - 201 Created:**
```json
{
  "id": "reading-uuid-003",
  "timestamp": 1734567890789,
  "value": 430.5
}
```

**GET /sensors/{sensorId} after readings - 200 OK (side effect confirmed):**
```json
{
  "id": "SENSOR-CO2-001",
  "type": "CO2",
  "status": "ACTIVE",
  "currentValue": 445.2,
  "roomId": "LIB-301"
}
```

**GET readings for non-existent sensor - 404 Not Found:**
```json
{ "error": "Sensor not found with id: INVALID-SENSOR" }
```

---

## 11. Part 5 - Advanced Error Handling & Logging

### Exception Mapper Strategy

All business rule violations are handled through JAX-RS `ExceptionMapper` classes. Custom exceptions are thrown in `RoomService` and propagate freely through the resource layer — no `try/catch` blocks intercept them — so the matching mapper converts them into structured JSON responses with the correct HTTP status code. This means all error logic lives in exactly one place per error type.

| Exception | Mapper | HTTP Status | Trigger |
|---|---|---|---|
| `RoomNotEmptyException` | `RoomNotEmptyExceptionMapper` | **409 Conflict** | Deleting a room with active sensors |
| `LinkedResourceNotFoundException` | `LinkedResourceNotFoundExceptionMapper` | **422 Unprocessable Entity** | POST sensor with non-existent roomId |
| `SensorUnavailableException` | `SensorUnavailableExceptionMapper` | **403 Forbidden** | POST reading to MAINTENANCE or OFFLINE sensor |
| Any `Throwable` | `GlobalExceptionMapper` | **500 Internal Server Error** | Any unexpected runtime error |

### Logging Filter

`LoggingFilter` implements both `ContainerRequestFilter` and `ContainerResponseFilter`, intercepting every single request and response automatically without any logging code in individual resource methods.

**Server console output example:**
```
INFO: → REQUEST: GET http://localhost:8080/api/v1/rooms
INFO: ← RESPONSE: GET http://localhost:8080/api/v1/rooms - Status: 200

INFO: → REQUEST: POST http://localhost:8080/api/v1/sensors
INFO: ← RESPONSE: POST http://localhost:8080/api/v1/sensors - Status: 201

INFO: → REQUEST: POST http://localhost:8080/api/v1/sensors/SENSOR-CO2-003/readings
INFO: ← RESPONSE: POST http://localhost:8080/api/v1/sensors/SENSOR-CO2-003/readings - Status: 403
```

### curl Commands for Error Testing

**1. Delete room with sensors - 409 Conflict**
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

**2. Create sensor with invalid roomId - 422 Unprocessable Entity**
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"type": "CO2", "status": "ACTIVE", "currentValue": 100, "roomId": "INVALID-ROOM"}'
```

**3. Add reading to MAINTENANCE sensor - 403 Forbidden**
```bash
curl -X POST http://localhost:8080/api/v1/sensors/SENSOR-CO2-003/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 100}'
```

**4. Get non-existent sensor - 404 Not Found**
```bash
curl -X GET http://localhost:8080/api/v1/sensors/FAKE-SENSOR
```

**5. Missing required field - 400 Bad Request**
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"roomId": "LIB-301"}'
```

### Sample Error Responses

**409 Conflict - Delete room with sensors:**
```json
{
  "error": "Cannot delete room",
  "message": "Room LIB-301 cannot be deleted because it has 2 active sensor(s)",
  "roomId": "LIB-301",
  "activeSensors": 2
}
```

**422 Unprocessable Entity - Invalid roomId:**
```json
{
  "error": "Unprocessable Entity",
  "message": "Room not found with id: INVALID-ROOM",
  "resourceType": "Room",
  "resourceId": "INVALID-ROOM"
}
```

**403 Forbidden - Sensor in MAINTENANCE:**
```json
{
  "error": "Forbidden",
  "message": "Sensor SENSOR-CO2-003 is currently in MAINTENANCE status and cannot accept new readings",
  "sensorId": "SENSOR-CO2-003",
  "currentStatus": "MAINTENANCE"
}
```

**500 Internal Server Error - no stack trace exposed:**
```json
{
  "error": "Internal Server Error",
  "message": "An unexpected error occurred. Please try again later."
}
```

---

## 12. Conceptual Report - Question Answers

---

### Part 1.1 - JAX-RS Resource Lifecycle and In-Memory Data Management

By default, JAX-RS creates a **new instance of each resource class for every incoming HTTP request**. This is called the per-request lifecycle. Every time a client calls `GET /api/v1/rooms`, JAX-RS instantiates a brand new `RoomResource` object, handles the request, and then discards it. This means that any instance variables defined on a resource class are reset on every request - they cannot be used to store persistent data.

This has a critical consequence for in-memory data management. If `RoomResource` stored its own `HashMap<String, Room>` as an instance variable, that map would be created empty on every request and data would never survive between calls. To solve this, the project uses the **Singleton pattern** for both `RoomService` and `DataStore`. Each uses a private constructor and exposes a single shared instance through a static `getInstance()` method. Every resource class calls `RoomService.getInstance()` in its own constructor, so all requests — regardless of which thread handles them — read from and write to the exact same shared maps.

Thread safety is addressed by using `ConcurrentHashMap` rather than a plain `HashMap`. Apache Tomcat processes multiple HTTP requests concurrently using a thread pool. A regular `HashMap` is not thread-safe and can produce corrupted data or lost writes when accessed simultaneously from multiple threads. `ConcurrentHashMap` provides thread-safe reads and fine-grained locking on individual write operations, preventing race conditions without the performance cost of locking the entire map on every access.

---

### Part 1.2 — HATEOAS and the Value of Hypermedia

Hypermedia As The Engine Of Application State (HATEOAS) is a REST constraint where API responses include navigational links to related resources and possible next actions, rather than requiring clients to construct or hardcode URLs from external documentation.

The discovery endpoint at `GET /api/v1/` demonstrates this by returning:
```json
{
  "collections": {
    "rooms": "/api/v1/rooms",
    "sensors": "/api/v1/sensors"
  }
}
```

This approach benefits client developers in several important ways. First, clients do not need to hardcode URL paths anywhere in their code — they follow links from the discovery response, so if the server changes its URL structure in a future version, clients adapting to the discovery response continue working without modification. Second, the API becomes self-documenting: a developer can discover every entry point from a single initial request. Third, it reduces tight coupling between client and server implementations — the server can evolve its URL design independently as long as the links in responses stay consistent. A fully HATEOAS-compliant API extends this to every response (for example, a room object would include a direct link to its sensor list and a link to delete itself), enabling clients to navigate the entire API dynamically without any prior knowledge of its structure. This is considered an advanced RESTful design because it makes APIs genuinely discoverable and resilient to change, which static documentation can never achieve.

---

### Part 2.1 — Implications of Returning IDs vs Full Room Objects

When returning a list of rooms there are two approaches, each with different trade-offs.

Returning **only IDs** (e.g. `["LIB-301", "CS-101", "ENG-202"]`) produces a very small response payload. However, if the client needs any details about those rooms — their names, capacities, or sensor lists — it must make one separate GET request per room. This is the "N+1 problem": for a list of 100 rooms, the client must make 101 HTTP requests in total, each adding its own network latency. This dramatically increases total load time and places unnecessary repeated load on the server.

Returning **full room objects** (as this API does) produces a larger single response, but the client receives everything it needs in one round-trip. For a campus system with a modest number of rooms over a local network, this is clearly preferable — the overhead of a slightly larger payload is negligible compared to the latency of making dozens of additional HTTP calls.

For very large collections at production scale, the correct solution is **pagination** — returning a manageable page of full objects at a time — combined with optional **sparse fieldsets** that allow clients to specify exactly which fields they need (e.g. `?fields=id,name`). This API returns full objects which is appropriate for the scale described in the scenario.

---

### Part 2.2 — DELETE Idempotency

The DELETE operation **is idempotent** in this implementation. Idempotency means that sending the same request multiple times produces the same server state as sending it once — regardless of how many times the request is repeated.

Concretely: the first `DELETE /api/v1/rooms/CS-101` finds the room, deletes it, and returns **204 No Content**. Every subsequent identical request finds no room with that ID and returns **404 Not Found**. The server state is identical after both calls — CS-101 no longer exists in either case. The HTTP response code differs between the first and subsequent calls (204 vs 404) but idempotency is defined in terms of **server state**, not response codes. The resource is absent after the first call and remains absent after every subsequent call, so the operation is idempotent.

This is the correct and expected behaviour for RESTful DELETE. It means a client can safely retry a DELETE request if a network issue left it uncertain whether the original request succeeded, without any risk of unintended side effects.

---

### Part 3.1 — @Consumes and Content-Type Mismatch

`@Consumes(MediaType.APPLICATION_JSON)` is a contract declaration — it tells the JAX-RS runtime that this endpoint will only accept request bodies with `Content-Type: application/json`. If a client sends `Content-Type: text/plain` or `Content-Type: application/xml`, JAX-RS automatically returns **HTTP 415 Unsupported Media Type** without the resource method ever being invoked. The framework inspects the incoming `Content-Type` header during request routing and rejects any mismatch before the method body executes.

This matters for two reasons. First, it enforces the API contract clearly — clients receive a specific and accurate error code telling them exactly what went wrong, rather than a confusing internal error. Second, it protects the Jackson JSON deserialiser, which only understands JSON, from receiving incompatible input. Without `@Consumes`, a client sending XML would cause Jackson to throw a parse exception during deserialization, resulting in a misleading 500 Internal Server Error rather than the semantically accurate 415. The annotation acts as a gatekeeper that fails fast with a correct error before any processing begins.

---

### Part 3.2 — @QueryParam vs Path Segment for Filtering

`GET /api/v1/sensors?type=CO2` using `@QueryParam` is semantically and technically superior to embedding the filter in the path as `/api/v1/sensors/type/CO2`.

In REST, **path segments identify a specific resource or resource type**. A path like `/sensors/SENSOR-CO2-001` identifies a specific sensor. A path like `/sensors/type/CO2` incorrectly implies that `type/CO2` is a sub-resource of the sensors collection, which is architecturally misleading. **Query parameters, by contrast, are specifically designed to modify or filter how a collection is presented** — they are optional additions to a base resource request.

Beyond semantics, the query parameter approach has several practical advantages. It is naturally **optional** — `GET /sensors` returns all sensors with no filter applied, which the path-based design cannot express cleanly. It is naturally **composable** — `?type=CO2&status=ACTIVE` adds a second filter without any URL restructuring. It also avoids **routing conflicts** — a path like `/sensors/type` would collide with the existing `/{sensorId}` path parameter in JAX-RS routing, causing the runtime to incorrectly try to resolve the literal string `"type"` as a sensor ID.

---

### Part 4.1 — Benefits of the Sub-Resource Locator Pattern

The Sub-Resource Locator pattern delegates handling of nested URL paths to a separate, dedicated class rather than accumulating every nested route in one large controller.

Without this pattern, `SensorResource` would need to contain `getAllReadings()`, `addReading()`, and `getReadingById()` methods alongside `getAllSensors()`, `createSensor()`, and `getSensorById()`. As the API grows with more nested resources, a single class becomes responsible for too many unrelated concerns — a violation of the Single Responsibility Principle. The class becomes difficult to navigate, test individually, and maintain without risking unintended changes to unrelated methods.

With the locator pattern, `SensorResource` contains a single delegating method that says "anything under `/readings` belongs to `SensorReadingResource`". That class then handles all reading-specific logic in complete isolation. The `sensorId` context flows cleanly through the constructor, so every reading method has access to it without requiring redundant path parameters on each method signature.

This separation also improves testability — `SensorReadingResource` can be unit-tested independently. In large real-world APIs with deep nesting (for example: buildings → floors → rooms → sensors → readings → alerts), this pattern is the only scalable approach to resource organisation. Defining every nested path in one massive controller class is unmaintainable at any non-trivial scale.

---

### Part 5.2 — HTTP 422 vs 404 for a Missing Referenced Resource

The distinction between 404 and 422 comes down to **what is missing**.

A **404 Not Found** means the URL of the request itself points to a resource that does not exist — the endpoint path is wrong or has nothing behind it. For example, `GET /api/v1/rooms/FAKE-999` returns 404 because nothing exists at that specific URL path.

A **422 Unprocessable Entity** means the request URL is correct, the JSON body is syntactically valid, but the **semantic content** of that body is logically invalid. When a client POSTs `{"type": "CO2", "roomId": "FAKE-999"}` to `/api/v1/sensors`, the endpoint `/api/v1/sensors` absolutely exists and handles POST requests correctly. The JSON is valid. The only problem is that the value of `roomId` references a room that does not exist — a broken reference inside an otherwise valid request. Using 404 here would be misleading because it would suggest the `/sensors` endpoint itself does not exist, which is false. Using 422 precisely communicates to the client: "your request reached the right endpoint and your JSON is well-formed, but the data you provided is semantically invalid." This distinction allows clients to diagnose and fix the problem accurately — they know immediately that the issue is the `roomId` value, not the URL they are calling.

---

### Part 5.4 — Security Risks of Exposing Stack Traces

Exposing raw Java stack traces to external API consumers is a serious security vulnerability. Stack traces can reveal several categories of sensitive information that attackers can exploit:

**Internal architecture:** Fully qualified class names such as `com.smartcampus.service.RoomService` expose the package structure, naming conventions, and design patterns used internally. An attacker gains a map of the codebase without access to the source code.

**Library versions:** Stack traces typically include frames from third-party libraries, for example `org.glassfish.jersey.server.internal.routing.RouterModule$RootRouteBuilder`. This reveals not only which libraries are in use but their exact versions, allowing attackers to search CVE databases for known, unpatched vulnerabilities in those precise versions and craft targeted exploits.

**Method names and line numbers:** These give attackers a precise roadmap of how the code executes. They can identify exactly which method failed, what it was processing, and at which line — information useful for crafting inputs designed to trigger specific code paths or bypass validation logic.

**Business logic flow:** From the call stack sequence, an attacker can infer how the application processes requests, which components interact, and where potential injection or manipulation points may exist.

This project's `GlobalExceptionMapper` mitigates all of these risks. It logs the complete stack trace to the server-side log file (`catalina.out`) where only administrators can see it, while returning only the generic message `"An unexpected error occurred. Please try again later."` to the client. Full internal observability is maintained for developers without exposing any exploitable information externally.

---

### Part 5.5 — JAX-RS Filters vs Per-Method Logging

Inserting `Logger.info()` calls manually into every resource method is an example of a **cross-cutting concern** — functionality that applies uniformly across the entire codebase but is not central to the purpose of any individual method.

Per-method logging has three fundamental problems. It is **duplicative**: the same boilerplate log statement must be written and maintained in every single method. If the log format changes — for example, to add a correlation ID or timestamp — every method must be updated individually, which is error-prone and time-consuming. It is **incomplete**: requests that are rejected by the JAX-RS framework before reaching a resource method — such as 415 Unsupported Media Type errors or 405 Method Not Allowed responses — are never logged at all, creating blind spots in observability. It also **mixes concerns**: logging code becomes entangled with business logic, making resource methods longer, harder to read, and harder to unit test cleanly.

`LoggingFilter` solves all three problems simultaneously. It is written once and maintained in exactly one place. It intercepts every single request and response — including framework-level rejections that never reach a resource method — providing complete, consistent observability across the entire API. Resource methods remain clean and focused purely on their own logic. This follows the Aspect-Oriented Programming (AOP) principle of separating cross-cutting concerns from core business logic, and is the standard industry practice for request/response logging in production web services.