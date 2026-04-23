# Smart Campus API

> **Module:** 5COSC022W Client-Server Architectures - University of Westminster  
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
RESOURCE LAYER      - DiscoveryResource, SensorRoom, SensorResource, SensorReadingResource
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
    │   ├── SensorRoom.java                 CRUD for /api/v1/rooms
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

By default, JAX-RS creates a new instance of each resource class for every incoming HTTP request. This means instance variables on resource classes are reset on every request and cannot hold persistent data.
To solve this, the project uses the Singleton pattern for both `RoomService` and `DataStore`. Each exposes a single shared instance via `getInstance()`, so all requests read from and write to the same maps regardless of which thread handles them.
Thread safety is handled by `ConcurrentHashMap` instead of a plain `HashMap`. Since Tomcat processes requests concurrently, a regular HashMap risks data corruption from simultaneous writes. `ConcurrentHashMap` provides thread-safe operations without locking the entire map on every access.

---

### Part 1.2 - HATEOAS and the Value of Hypermedia

HATEOAS (Hypermedia As The Engine Of Application State) means including navigational links inside API responses rather than requiring clients to hardcode URLs from documentation.
The discovery endpoint at `GET /api/v1/` demonstrates this by returning links to `/api/v1/rooms` and `/api/v1/sensors`. This benefits developers in three ways: clients do not need to hardcode URLs so they survive server-side URL changes automatically; the API becomes self-documenting since all entry points are discoverable from one request; and it reduces coupling between client and server. Static documentation goes out of date — HATEOAS responses are always accurate because they come from the live server.
---

### Part 2.1 - Implications of Returning IDs vs Full Room Objects

Returning only IDs produces a small payload but forces the client to make one extra GET request per room to fetch details, the N+1 problem. For 100 rooms, that means 101 HTTP requests, each adding network latency.
Returning full room objects (as this API does) costs slightly more bandwidth but delivers everything the client needs in a single round-trip. For a campus system with a modest number of rooms, this is clearly preferable. At a larger scale, the right solution is pagination combined with sparse fieldsets, but full objects are appropriate here.

---

### Part 2.2 - DELETE Idempotency

The DELETE operation is idempotent in this implementation. The first `DELETE /api/v1/rooms/CS-101` deletes the room and returns 204 No Content. Every subsequent identical request returns a 404 Not Found error. The server state is the same after both calls — CS-101 does not exist either way. Idempotency is defined by server state, not response codes, so this is correct behaviour. A client can safely retry a DELETE without risk of unintended side effects.

---

### Part 3.1 - @Consumes and Content-Type Mismatch

`@Consumes(MediaType.APPLICATION_JSON)` tells JAX-RS to only accept requests with `Content-Type: application/json`. If a client sends `text/plain` or `application/xml`, JAX-RS automatically returns HTTP 415 Unsupported Media Type before the method body even executes.
This enforces the API contract clearly and protects the Jackson deserialiser from incompatible input. Without `@Consumes`, a client sending XML would cause Jackson to throw a parse exception, returning a misleading 500 error instead of the accurate 415.

---

### Part 3.2 - @QueryParam vs Path Segment for Filtering

`GET /api/v1/sensors?type=CO2` using `@QueryParam` is superior to embedding the filter in the path as `/api/v1/sensors/type/CO2` for three reasons.

First, semantics path segments identify resources; query parameters filter collections. Embedding a filter in the path implies `type/CO2` is a sub-resource, which is misleading. Second, composability; `?type=CO2&status=ACTIVE` adds a second filter naturally; path-based filtering cannot do this cleanly. Third, routing conflicts; `/sensors/type` would collide with the `/{sensorId}` pattern, causing JAX-RS to treat `"type"` as a sensor ID.

---

### Part 4.1 - Benefits of the Sub-Resource Locator Pattern

Without the pattern, `SensorResource` would contain both sensor methods and all reading methods, growing into an unmaintainable class that violates the Single Responsibility Principle.
With the locator, `SensorResource` delegates everything under `/readings` to `SensorReadingResource` via one method. That class handles all reading logic in isolation, receives the `sensorId` cleanly through its constructor, and can be tested independently. In large APIs with deep nesting, this is the only scalable approach, as defining every nested path in one massive controller is unworkable.

---

### Part 5.2 - HTTP 422 vs 404 for a Missing Referenced Resource

The distinction between 404 and 422 comes down to **what is missing**.

A **404 Not Found** means the URL of the request itself points to a resource that does not exist, the endpoint path is wrong, or has nothing behind it. For example, `GET /api/v1/rooms/FAKE-999` returns 404 because nothing exists at that specific URL path.

A **422 Unprocessable Entity** indicates that the request URL is correct and the JSON body is valid, but the semantic content is logically invalid. For example, if a client POSTs `{"type": "CO2", "roomId": "FAKE-999"}` to `/api/v1/sensors`, the endpoint exists and accepts requests, but the `roomId` references a non-existent room. Using 404 would mislead clients into thinking the endpoint itself is unavailable. A 422 status clearly conveys: "Your request is valid, but the data is semantically incorrect," helping clients pinpoint the issue with `roomId`, not the URL.

---

### Part 5.4 - Security Risks of Exposing Stack Traces

Raw stack traces expose four categories of sensitive information. Class names and package structure reveal the internal architecture without needing source code access. Library names and versions allow attackers to search CVE databases for known vulnerabilities in those exact versions. Method names and line numbers give a precise roadmap of the codebase useful for crafting targeted inputs. Business logic flow from the call stack reveals how requests are processed and where weak points may exist.

This project's `GlobalExceptionMapper` logs the full trace server-side only, returning just "An unexpected error occurred" to the client, maintaining full observability for developers without leaking exploitable information externally.

---

### Part 5.5 - JAX-RS Filters vs Per-Method Logging

Manually inserting `Logger.info()` into every resource method has three problems: it is duplicative (the same boilerplate repeated everywhere, hard to update); incomplete (framework-level rejections like 415 errors never reach resource methods and so are never logged); and it mixes concerns (logging entangled with business logic makes methods harder to read and test).
`LoggingFilter` solves all three at once; written once, applied automatically to every request and response including framework rejections, and completely invisible to resource methods. This is the standard Aspect-Oriented Programming approach to cross-cutting concerns in production web services.