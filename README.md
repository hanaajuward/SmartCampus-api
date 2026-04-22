# Smart Campus API

> **Module:** 5COSC022W Client-Server Architectures — University of Westminster  
> **Author:** Hanaa Ajuward  
> **Technology:** JAX-RS (Jersey 2.32) · Java 17 · Apache Tomcat 9.0 · Maven

A RESTful API for managing university campus rooms and IoT sensors. Facilities managers and automated building systems can register rooms, deploy sensors, record historical readings, and query live data — all through a clean, versioned HTTP interface built entirely with JAX-RS. All data is stored in-memory using `ConcurrentHashMap` with no database required.

---

## 1. API Design Overview

The API follows REST principles with a versioned base path of `/api/v1`. It is structured into five layers:

```
CLIENT (Postman / curl)
        |
        v
FILTER LAYER        — LoggingFilter logs every request and response
        |
        v
RESOURCE LAYER      — DiscoveryResource, RoomResource, SensorResource, SensorReadingResource
        |                 Handle HTTP routing, parse input, build responses
        v
SERVICE LAYER       — RoomService (Singleton)
        |                 All business logic, validation, exception throwing
        v
STORAGE LAYER       — DataStore (Singleton)
                          Three ConcurrentHashMaps: rooms, sensors, sensorReadings

If an exception is thrown in the service:
        |
        v
EXCEPTION MAPPER LAYER — JAX-RS intercepts and routes to matching ExceptionMapper
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

- **Java 17** — verify with `java -version`
- **Apache Maven 3.6+** — verify with `mvn -version`
- **Apache Tomcat 9.0** — download from [tomcat.apache.org](https://tomcat.apache.org)
- Any IDE (NetBeans recommended)

---

## 4. How to Build and Launch the Server

### Step 1 — Clone the repository

```bash
git clone https://github.com/hanaajuward/SmartCampusAPI.git
cd SmartCampusAPI
```

### Step 2 — Build the WAR file

```bash
mvn clean package
```

This generates: `target/SmartCampusAPI-1.0-SNAPSHOT.war`

Maven downloads all dependencies automatically on the first build. If it fails, ensure `JAVA_HOME` points to JDK 17.

### Step 3 — Deploy to Tomcat

Copy the WAR to Tomcat's webapps directory:

```bash
# Mac/Linux
cp target/SmartCampusAPI-1.0-SNAPSHOT.war /path/to/apache-tomcat-9.0/webapps/

# Windows
copy target\SmartCampusAPI-1.0-SNAPSHOT.war C:\tomcat\webapps\
```

### Step 4 — Start Tomcat

```bash
# Mac/Linux
/path/to/apache-tomcat-9.0/bin/startup.sh

# Windows
C:\tomcat\bin\startup.bat
```

Watch for: `INFO: Server startup in [X] milliseconds`

### Step 5 — Verify it's running

```bash
curl http://localhost:8080/SmartCampusAPI/api/v1/
```

You should receive the discovery JSON response. If you get a 404, check that Tomcat is running (`http://localhost:8080` should show the Tomcat welcome page) and that the WAR deployed without errors in `logs/catalina.out`.

### Step 6 — Stop Tomcat

```bash
# Mac/Linux
/path/to/apache-tomcat-9.0/bin/shutdown.sh

# Windows
C:\tomcat\bin\shutdown.bat
```

> **Note:** All data is in-memory. Restarting Tomcat resets everything back to the pre-loaded sample data.

### Alternative — NetBeans IDE

1. Open project in NetBeans
2. Right-click project → **Clean and Build**
3. Configure Tomcat 9 server under Tools → Servers if not already done
4. Right-click project → **Run** — NetBeans deploys automatically

---

## 5. Pre-Loaded Sample Data

The `DataStore` is pre-populated on startup for testing:

### Rooms

| ID | Name | Capacity | Assigned Sensors |
|---|---|---|---|
| `LIB-301` | Library Quiet Study | 50 | SENSOR-CO2-001, SENSOR-TEMP-001 |
| `CS-101` | Computer Science Lab | 35 | *(none — safe to delete)* |
| `ENG-202` | Engineering Workshop | 40 | SENSOR-CO2-003 |

### Sensors

| ID | Type | Status | Current Value | Room |
|---|---|---|---|---|
| `SENSOR-CO2-001` | CO2 | ACTIVE | 420.5 | LIB-301 |
| `SENSOR-TEMP-001` | Temperature | ACTIVE | 22.5 | LIB-301 |
| `SENSOR-CO2-003` | CO2 | MAINTENANCE | 0 | ENG-202 |

**Quick reference for testing:**

| What to test | Use this |
|---|---|
| Successful room deletion (204) | `DELETE /rooms/CS-101` |
| Failed room deletion — 409 | `DELETE /rooms/LIB-301` |
| Successful reading post (201) | `POST /sensors/SENSOR-CO2-001/readings` |
| Blocked reading — 403 | `POST /sensors/SENSOR-CO2-003/readings` |
| Filter by type | `GET /sensors?type=CO2` |
| Invalid room reference — 422 | `POST /sensors` with `"roomId": "FAKE"` |

---

## 6. Project Structure

```
SmartCampusAPI/
├── pom.xml
├── README.md
└── src/main/java/com/smartcampus/
    ├── ApplicationConfig.java              JAX-RS bootstrap — registers all classes
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
    │   └── RoomService.java                Singleton — all business logic
    ├── storage/
    │   └── DataStore.java                  Singleton — ConcurrentHashMaps + sample data
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

## 7. Part 1 — Service Architecture & Discovery

### Discovery Endpoint

**`GET /api/v1/`** — Returns API metadata and links to all resource collections.

```bash
curl -X GET http://localhost:8080/SmartCampusAPI/api/v1/
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

## 8. Part 2 — Room Management

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
curl -X GET http://localhost:8080/SmartCampusAPI/api/v1/rooms
```

**2. GET room by ID**
```bash
curl -X GET http://localhost:8080/SmartCampusAPI/api/v1/rooms/LIB-301
```

**3. POST — create new room**
```bash
curl -X POST http://localhost:8080/SmartCampusAPI/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id": "HALL-001", "name": "Main Lecture Hall", "capacity": 200}'
```

**4. DELETE — empty room (succeeds)**
```bash
curl -X DELETE http://localhost:8080/SmartCampusAPI/api/v1/rooms/CS-101
```

**5. DELETE — room with sensors (blocked)**
```bash
curl -X DELETE http://localhost:8080/SmartCampusAPI/api/v1/rooms/LIB-301
```

**6. GET — non-existent room**
```bash
curl -X GET http://localhost:8080/SmartCampusAPI/api/v1/rooms/FAKE-999
```

### Sample Responses

**GET /rooms — 200 OK:**
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

**POST /rooms — 201 Created:**
```json
{
  "id": "HALL-001",
  "name": "Main Lecture Hall",
  "capacity": 200,
  "sensorIds": []
}
```

**DELETE with sensors — 409 Conflict:**
```json
{
  "error": "Cannot delete room",
  "message": "Room LIB-301 cannot be deleted because it has 2 active sensor(s)",
  "roomId": "LIB-301",
  "activeSensors": 2
}
```

**DELETE empty room — 204 No Content:** *(empty body)*

**GET non-existent room — 404 Not Found:**
```json
{ "error": "Room not found" }
```

---

## 9. Part 3 — Sensor Operations & Linking

### Endpoints

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/sensors` | List all sensors (optional `?type=` filter) |
| POST | `/api/v1/sensors` | Register a sensor (validates roomId exists) |
| GET | `/api/v1/sensors/{sensorId}` | Get sensor by ID |

### curl Commands

**1. GET all sensors**
```bash
curl -X GET http://localhost:8080/SmartCampusAPI/api/v1/sensors
```

**2. GET sensors filtered by type**
```bash
curl -X GET "http://localhost:8080/SmartCampusAPI/api/v1/sensors?type=CO2"
```

**3. POST — create new sensor (valid roomId)**
```bash
curl -X POST http://localhost:8080/SmartCampusAPI/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"type": "CO2", "status": "ACTIVE", "currentValue": 420.5, "roomId": "LIB-301"}'
```

**4. GET sensor by ID**
```bash
curl -X GET http://localhost:8080/SmartCampusAPI/api/v1/sensors/SENSOR-CO2-001
```

**5. POST — invalid roomId (triggers 422)**
```bash
curl -X POST http://localhost:8080/SmartCampusAPI/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"type": "CO2", "status": "ACTIVE", "currentValue": 100, "roomId": "INVALID-ROOM"}'
```

**6. GET sensors filtered by Temperature**
```bash
curl -X GET "http://localhost:8080/SmartCampusAPI/api/v1/sensors?type=Temperature"
```

### Sample Responses

**GET /sensors — 200 OK:**
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

**GET /sensors?type=CO2 — 200 OK:**
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

**POST /sensors — 201 Created:**
```json
{
  "id": "generated-uuid-here",
  "type": "CO2",
  "status": "ACTIVE",
  "currentValue": 420.5,
  "roomId": "LIB-301"
}
```

**POST with invalid roomId — 422 Unprocessable Entity:**
```json
{
  "error": "Unprocessable Entity",
  "message": "Room not found with id: INVALID-ROOM",
  "resourceType": "Room",
  "resourceId": "INVALID-ROOM"
}
```

---

## 10. Part 4 — Deep Nesting with Sub-Resources

### Overview

Each sensor maintains a full timestamped history of all measurements. Readings are managed through a nested sub-resource under each sensor. When a reading is successfully posted, `currentValue` on the parent sensor is automatically updated as a side effect.

### Sub-Resource Locator Pattern

In `SensorResource.java`, a sub-resource locator method delegates `/readings` paths to `SensorReadingResource`:

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
curl -X GET http://localhost:8080/SmartCampusAPI/api/v1/sensors/SENSOR-CO2-001/readings
```

**2. POST a new reading**
```bash
curl -X POST http://localhost:8080/SmartCampusAPI/api/v1/sensors/SENSOR-CO2-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 430.5}'
```

**3. POST another reading to build history**
```bash
curl -X POST http://localhost:8080/SmartCampusAPI/api/v1/sensors/SENSOR-CO2-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 445.2}'
```

**4. GET all readings again (shows history)**
```bash
curl -X GET http://localhost:8080/SmartCampusAPI/api/v1/sensors/SENSOR-CO2-001/readings
```

**5. GET a specific reading by ID**
```bash
curl -X GET http://localhost:8080/SmartCampusAPI/api/v1/sensors/SENSOR-CO2-001/readings/{readingId}
```

**6. Verify side effect — sensor's currentValue should now be 445.2**
```bash
curl -X GET http://localhost:8080/SmartCampusAPI/api/v1/sensors/SENSOR-CO2-001
```

**7. POST to a MAINTENANCE sensor (triggers 403)**
```bash
curl -X POST http://localhost:8080/SmartCampusAPI/api/v1/sensors/SENSOR-CO2-003/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 100}'
```

**8. GET readings for a non-existent sensor (triggers 404)**
```bash
curl -X GET http://localhost:8080/SmartCampusAPI/api/v1/sensors/INVALID-SENSOR/readings
```

### Sample Responses

**GET /sensors/{sensorId}/readings — 200 OK:**
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

**POST /sensors/{sensorId}/readings — 201 Created:**
```json
{
  "id": "reading-uuid-003",
  "timestamp": 1734567890789,
  "value": 430.5
}
```

**GET /sensors/{sensorId} after posting readings — 200 OK (side effect visible):**
```json
{
  "id": "SENSOR-CO2-001",
  "type": "CO2",
  "status": "ACTIVE",
  "currentValue": 445.2,
  "roomId": "LIB-301"
}
```

**GET readings for non-existent sensor — 404 Not Found:**
```json
{ "error": "Sensor not found with id: INVALID-SENSOR" }
```

---

## 11. Part 5 — Advanced Error Handling & Logging

### Exception Mapper Strategy

All business rule violations are handled through JAX-RS `ExceptionMapper` classes. Custom exceptions are thrown in `RoomService` and propagate freely through the resource layer — no `try/catch` blocks intercept them — so the matching mapper converts them into structured JSON responses with the correct HTTP status code.

| Exception | Mapper | HTTP Status | Trigger |
|---|---|---|---|
| `RoomNotEmptyException` | `RoomNotEmptyExceptionMapper` | **409 Conflict** | Deleting a room with active sensors |
| `LinkedResourceNotFoundException` | `LinkedResourceNotFoundExceptionMapper` | **422 Unprocessable Entity** | POST sensor with non-existent roomId |
| `SensorUnavailableException` | `SensorUnavailableExceptionMapper` | **403 Forbidden** | POST reading to MAINTENANCE or OFFLINE sensor |
| Any `Throwable` | `GlobalExceptionMapper` | **500 Internal Server Error** | Any unexpected runtime error |

### Logging Filter

`LoggingFilter` implements both `ContainerRequestFilter` and `ContainerResponseFilter`. It intercepts every single request and response automatically without any code in individual resource methods.

**Server console output:**
```
INFO: → REQUEST: GET http://localhost:8080/SmartCampusAPI/api/v1/rooms
INFO: ← RESPONSE: GET http://localhost:8080/SmartCampusAPI/api/v1/rooms - Status: 200

INFO: → REQUEST: POST http://localhost:8080/SmartCampusAPI/api/v1/sensors
INFO: ← RESPONSE: POST http://localhost:8080/SmartCampusAPI/api/v1/sensors - Status: 201

INFO: → REQUEST: POST http://localhost:8080/SmartCampusAPI/api/v1/sensors/SENSOR-CO2-003/readings
INFO: ← RESPONSE: POST http://localhost:8080/SmartCampusAPI/api/v1/sensors/SENSOR-CO2-003/readings - Status: 403
```

### curl Commands for Error Testing

**1. Delete room with sensors — 409 Conflict**
```bash
curl -X DELETE http://localhost:8080/SmartCampusAPI/api/v1/rooms/LIB-301
```

**2. Create sensor with invalid roomId — 422 Unprocessable Entity**
```bash
curl -X POST http://localhost:8080/SmartCampusAPI/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"type": "CO2", "status": "ACTIVE", "currentValue": 100, "roomId": "INVALID-ROOM"}'
```

**3. Add reading to MAINTENANCE sensor — 403 Forbidden**
```bash
curl -X POST http://localhost:8080/SmartCampusAPI/api/v1/sensors/SENSOR-CO2-003/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 100}'
```

**4. Get non-existent sensor — 404 Not Found**
```bash
curl -X GET http://localhost:8080/SmartCampusAPI/api/v1/sensors/FAKE-SENSOR
```

**5. Missing required field — 400 Bad Request**
```bash
curl -X POST http://localhost:8080/SmartCampusAPI/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"roomId": "LIB-301"}'
```

### Sample Error Responses

**409 Conflict — Delete room with sensors:**
```json
{
  "error": "Cannot delete room",
  "message": "Room LIB-301 cannot be deleted because it has 2 active sensor(s)",
  "roomId": "LIB-301",
  "activeSensors": 2
}
```

**422 Unprocessable Entity — Invalid roomId:**
```json
{
  "error": "Unprocessable Entity",
  "message": "Room not found with id: INVALID-ROOM",
  "resourceType": "Room",
  "resourceId": "INVALID-ROOM"
}
```

**403 Forbidden — Sensor in MAINTENANCE:**
```json
{
  "error": "Forbidden",
  "message": "Sensor SENSOR-CO2-003 is currently in MAINTENANCE status and cannot accept new readings",
  "sensorId": "SENSOR-CO2-003",
  "currentStatus": "MAINTENANCE"
}
```

**500 Internal Server Error — unexpected error (no stack trace exposed):**
```json
{
  "error": "Internal Server Error",
  "message": "An unexpected error occurred. Please try again later."
}
```

---

## 12. Conceptual Report — Question Answers

### Part 1.1 — JAX-RS Resource Lifecycle

By default, JAX-RS creates a **new instance of each resource class for every incoming HTTP request** (per-request lifecycle). This means no state can be stored in resource instance variables — each request gets a completely fresh object with no memory of previous requests.

This directly impacts in-memory data management. If `RoomResource` held its own `HashMap<String, Room>`, it would be empty on every single request and data would never persist between calls. To solve this, the project uses the **Singleton pattern** for both `RoomService` and `DataStore`. A single static instance is shared across all requests via `getInstance()`, meaning every request reads from and writes to the same shared maps.

Thread safety is handled by `ConcurrentHashMap` instead of a regular `HashMap`. Since Tomcat processes multiple HTTP requests concurrently on separate threads, a plain `HashMap` would risk data corruption from simultaneous reads and writes. `ConcurrentHashMap` provides thread-safe operations without the performance cost of fully synchronised methods, preventing race conditions and data loss.

---

### Part 1.2 — HATEOAS

Hypermedia As The Engine Of Application State (HATEOAS) means including navigational links within API responses, rather than requiring clients to construct or hardcode URLs from external documentation.

The discovery endpoint at `GET /api/v1/` demonstrates this by returning links to `/api/v1/rooms` and `/api/v1/sensors`. This benefits client developers in several important ways. First, clients can discover all API entry points from a single request without reading documentation. Second, clients do not need to hardcode URL paths — they follow links, so if the URL structure changes, clients reading from the discovery response adapt automatically without code changes on their side. Third, it reduces coupling between client and server — the server can evolve its URL design as long as the discovery response stays updated. A fully HATEOAS-compliant API would include links in every response (e.g. a room object would link directly to its sensors list), pointing toward a self-navigating API.

---

### Part 2.1 — Returning IDs vs Full Objects in Lists

When returning a list of rooms, there are two choices. Returning only IDs (e.g. `["LIB-301", "CS-101"]`) minimises bandwidth but forces the client to make one additional GET request per room to fetch any details — this is the "N+1 problem". For 100 rooms, that means 101 HTTP requests, each with its own network round-trip latency.

Returning full room objects (as this API does) costs slightly more bandwidth per response but the client has everything it needs in a single round-trip. For a local campus network with a manageable number of rooms, this is clearly preferable. For large-scale scenarios, the correct solution is **pagination** — returning N full objects per page — combined with **sparse fieldsets** allowing clients to request only the fields they need (e.g. `?fields=id,name`). This API returns full objects which is appropriate for the scale described in the scenario.

---

### Part 2.2 — DELETE Idempotency

The DELETE operation **is idempotent** in this implementation. Idempotency means making the same request multiple times produces the same server state as making it once.

The first `DELETE /api/v1/rooms/CS-101` deletes the room and returns **204 No Content**. Every subsequent identical request returns **404 Not Found**. The server state is identical after both calls — CS-101 does not exist in either case. The response code differs (204 vs 404) but idempotency is defined in terms of server state, not response codes. This is correct REST behaviour — a client can safely retry a DELETE request if it is unsure whether the first one succeeded, without risk of unintended side effects.

---

### Part 3.1 — @Consumes and Content-Type Mismatch

`@Consumes(MediaType.APPLICATION_JSON)` declares that the endpoint only accepts request bodies with `Content-Type: application/json`. If a client sends `Content-Type: text/plain` or `Content-Type: application/xml`, JAX-RS automatically returns **HTTP 415 Unsupported Media Type** without the resource method ever being invoked.

The runtime checks the incoming `Content-Type` header during request routing and matches it against the `@Consumes` annotation. If no registered method accepts that content type, Jersey returns 415 immediately. This is a contract-enforcement mechanism that ensures the Jackson JSON deserialiser is never handed incompatible data. Without `@Consumes`, a client sending XML would cause Jackson to throw a parse exception, resulting in a misleading 500 error rather than the semantically correct 415.

---

### Part 3.2 — @QueryParam vs Path Segment for Filtering

`GET /api/v1/sensors?type=CO2` using `@QueryParam` is semantically and technically superior to a path-based alternative like `/api/v1/sensors/type/CO2`.

In REST, path segments identify a specific resource. Query parameters modify how a collection is filtered or viewed. Filtering is a modifier on a collection — not a resource identifier — so `?type=CO2` is the semantically correct placement. The path alternative falsely implies that `type/CO2` is a sub-resource of sensors rather than a filter instruction.

Query parameters are also naturally optional (`GET /sensors` still works with no filter), naturally composable (`?type=CO2&status=ACTIVE`), and avoid URL conflicts — a path segment like `/sensors/type` would collide with the `/{sensorId}` pattern in JAX-RS routing, causing it to try to resolve `"type"` as a sensor ID.

---

### Part 4.1 — Sub-Resource Locator Benefits

The Sub-Resource Locator pattern delegates handling of nested URL paths to a separate dedicated class rather than accumulating all methods in one growing controller.

Without this pattern, `SensorResource` would contain reading methods (`getAllReadings`, `addReading`, `getReadingById`) alongside sensor methods (`getAllSensors`, `createSensor`, `getSensorById`). As the API grows, the class becomes responsible for too many concerns, violating the Single Responsibility Principle and becoming difficult to maintain and test.

With the locator, `SensorResource` contains one delegating method and `SensorReadingResource` handles all reading logic in complete isolation. The `sensorId` context flows cleanly through the constructor rather than as a redundant path parameter in every reading method. In large APIs with deep nesting (buildings → floors → rooms → sensors → readings), this pattern is the only maintainable approach to resource organisation.

---

### Part 5.2 — HTTP 422 vs 404 for Missing Referenced Resource

A `404 Not Found` signals that the URL of the request itself points to a non-existent resource — the endpoint path is wrong. For example, `GET /api/v1/rooms/FAKE-999` returns 404 because nothing exists at that path.

A `422 Unprocessable Entity` signals that the URL is valid, the JSON is syntactically correct, but the semantic content of the payload is invalid. When a client POSTs `{"type": "CO2", "roomId": "FAKE-999"}` to `/api/v1/sensors`, the endpoint `/api/v1/sensors` absolutely exists and works. The problem is that the value of `roomId` references something that does not exist. Using 404 here would be misleading — it would suggest the `/sensors` endpoint itself is wrong. Using 422 precisely tells the client: "your request reached the right place and was understood, but the data you provided is logically invalid." This distinction helps clients diagnose the exact problem quickly.

---

### Part 5.4 — Stack Trace Security Risk

Exposing raw Java stack traces to external API consumers is a significant security vulnerability. Stack traces reveal:

- **Fully qualified class names** — e.g. `com.smartcampus.service.RoomService` — exposing the internal package structure and architecture
- **Library names and exact versions** — e.g. `org.glassfish.jersey.server.internal...` — allowing attackers to search CVE databases for known vulnerabilities in those precise versions and craft targeted exploits
- **Line numbers and method names** — giving attackers a precise map of the codebase to target with crafted inputs
- **Business logic flow** — from the call stack, an attacker can infer how the application processes requests and identify potential weak points

This project's `GlobalExceptionMapper` guards against all of these by logging the full stack trace server-side (visible only in `catalina.out` to administrators) while returning only the generic message `"An unexpected error occurred. Please try again later."` to the client. Full observability for developers is maintained without leaking any exploitable information externally.

---

### Part 5.5 — Logging Filters vs Per-Method Logging

Inserting `Logger.info()` calls into every resource method is a **cross-cutting concern** — the same boilerplate applied uniformly across every method without being central to any method's own purpose.

Per-method logging has three significant problems. It is duplicative — if the log format needs to change, every method must be updated individually. It is incomplete — requests rejected before reaching a resource method (e.g. 415 Unsupported Media Type errors) are never logged at all. It mixes concerns — logging code is entangled with business logic, making methods harder to read and test.

`LoggingFilter` solves all three problems. Written once and maintained in one place, it intercepts every single request and response automatically — including framework-level rejections — providing complete, consistent observability. Resource methods remain clean and focused purely on their own logic. This is the same principle as Aspect-Oriented Programming (AOP): separating cross-cutting concerns from core application logic is standard industry practice for production web services.