**Smart Campus API**



**Overview**

**RESTful API for managing university campus rooms and sensors, built with JAX-RS (Jersey) on Apache Tomcat.**



**Technology Stack**

**- JAX-RS (Jersey 2.32)**

**- Apache Tomcat 9.0**

**- Java 17**

**- Maven**



**How to Build and Launch the Server**



**Prerequisites**

**- Java 17 installed**

**- Apache Tomcat 9.0 installed**



**Steps**

**1. Open project in NetBeans (or any IDE)**

**2. Right-click project → \*\*Clean and Build\*\***

**3. Copy `target/SmartCampusAPI-1.0-SNAPSHOT.war` to Tomcat's `webapps/` folder**

**4. Start Tomcat: Run `startup.bat` (Windows) or `startup.sh` (Mac/Linux) from Tomcat's `bin/` folder**

**5. Verify API is running at: http://localhost:8080/api/v1/**





**Part 1: Service Architecture**



**Discovery Endpoint**

**GET `/api/v1/`**



**```bash**

**curl -X GET http://localhost:8080/api/v1**



**Response:**

**json:**

**{**

&#x20; **"version": "1.0.0",**

&#x20; **"contact": "smartcampus@university.edu",**

&#x20; **"collections": {**

&#x20;   **"rooms": "/api/v1/rooms",**

&#x20;   **"sensors": "/api/v1/sensors"**

&#x20; **}**





**Part 2: Room Management**



**Endpoints**



**Method	Endpoint	Description**

**GET	/api/v1/rooms	List all rooms**

**POST	/api/v1/rooms	Create a new room**

**GET	/api/v1/rooms/{roomId}	Get room by ID**

**DELETE	/api/v1/rooms/{roomId}	Delete a room (if no sensors)**





**Sample curl Commands**

**1. GET all rooms**



**bash**

**curl -X GET http://localhost:8080/api/v1/rooms**



**2. GET room by ID**



**bash**

**curl -X GET http://localhost:8080/api/v1/rooms/LIB-301**



**3. POST create new room**



**bash**

**curl -X POST http://localhost:8080/api/v1/rooms \\**

&#x20; **-H "Content-Type: application/json" \\**

&#x20; **-d '{"name":"CS Computer Lab","capacity":35}'**



**4. DELETE a room (replace {id} with actual ID)**



**bash**

**curl -X DELETE http://localhost:8080/api/v1/rooms/{id}**



**5. GET discovery endpoint**



**bash**

**curl -X GET http://localhost:8080/api/v1/**





**Sample Responses**



**GET /rooms (200 OK):**



**json**

**\[**

&#x20; **{**

&#x20;   **"id": "LIB-301",**

&#x20;   **"name": "Library Quiet Study",**

&#x20;   **"capacity": 50,**

&#x20;   **"sensorIds": \[]**

&#x20; **}**

**]**



**POST /rooms (201 Created):**



**json**

**{**

&#x20; **"id": "abc-123-def",**

&#x20; **"name": "CS Lab",**

&#x20; **"capacity": 35,**

&#x20; **"sensorIds": \[]**

**}**



**DELETE with sensors (409 Conflict):**



**json**

**{**

&#x20; **"error": "Cannot delete room: active sensors still assigned"**

**}**




**Part 3: Sensor Operations and Linking**



**Endpoints**

**Method	Endpoint	Description**

**GET	/api/v1/sensors	List all sensors (optionally filtered by type)**

**POST	/api/v1/sensors	Create a new sensor (validates roomId exists)**

**GET	/api/v1/sensors/{sensorId}	Get sensor by ID**



**Sample curl Commands**

**1. GET all sensors**

**bash**

**curl -X GET http://localhost:8080/api/v1/sensors**



**2. GET sensors filtered by type**

**bash**

**curl -X GET "http://localhost:8080/api/v1/sensors?type=CO2"**



**3. POST create new sensor (with valid roomId)**

**bash**

**curl -X POST http://localhost:8080/api/v1/sensors \\**

&#x20; **-H "Content-Type: application/json" \\**

&#x20; **-d '{"type":"CO2","status":"ACTIVE","currentValue":420.5,"roomId":"LIB-301"}'**



**4. GET sensor by ID**

**bash**

**curl -X GET http://localhost:8080/api/v1/sensors/{sensorId}**



**5. POST with invalid roomId (should fail)**

**bash**

**curl -X POST http://localhost:8080/api/v1/sensors \\**

&#x20; **-H "Content-Type: application/json" \\**

&#x20; **-d '{"type":"CO2","status":"ACTIVE","currentValue":100,"roomId":"INVALID-ROOM"}'**



**Sample Responses**



**GET /sensors (200 OK):**

**json**

**\[**

&#x20; **{**

&#x20;   **"id": "sensor-001",**

&#x20;   **"type": "CO2",**

&#x20;   **"status": "ACTIVE",**

&#x20;   **"currentValue": 420.5,**

&#x20;   **"roomId": "LIB-301"**

&#x20; **},**

&#x20; **{**

&#x20;   **"id": "sensor-002",**

&#x20;   **"type": "Temperature",**

&#x20;   **"status": "ACTIVE",**

&#x20;   **"currentValue": 22.5,**

&#x20;   **"roomId": "LIB-301"**

&#x20; **}**

**]**



**GET /sensors?type=CO2 (200 OK):**

**json**

**\[**

&#x20; **{**

&#x20;   **"id": "sensor-001",**

&#x20;   **"type": "CO2",**

&#x20;   **"status": "ACTIVE",**

&#x20;   **"currentValue": 420.5,**

&#x20;   **"roomId": "LIB-301"**

&#x20; **}**

**]**



**POST /sensors (201 Created):**

**json**

**{**

&#x20; **"id": "sensor-003",**

&#x20; **"type": "CO2",**

&#x20; **"status": "ACTIVE",**

&#x20; **"currentValue": 420.5,**

&#x20; **"roomId": "LIB-301"**

**}**



**POST with invalid roomId (400 Bad Request):**

**json**

**{**

&#x20; **"error": "Room not found: INVALID-ROOM"**

**}**





**Part 4: Deep Nesting with Sub-Resources**



**Overview**



**The API supports historical tracking of sensor readings. Each sensor has its own reading history, accessible through nested sub-resources.**



**Endpoints**

**Method	Endpoint	Description**

**GET	/api/v1/sensors/{sensorId}/readings	Get all readings for a specific sensor**

**POST	/api/v1/sensors/{sensorId}/readings	Add a new reading for a specific sensor**

**GET	/api/v1/sensors/{sensorId}/readings/{readingId}	Get a specific reading by ID**



**Sub-Resource Locator Pattern**



**In SensorResource.java, a sub-resource locator method delegates to SensorReadingResource:**



**java**

**@Path("/{sensorId}/readings")**

**public SensorReadingResource getSensorReadingResource(@PathParam("sensorId") String sensorId) {**

&#x20;   **return new SensorReadingResource(sensorId);**

**}**





**Sample curl commands**



1. **GET all readings for a sensor (initially empty)

bash
curl -X GET http://localhost:8080/api/v1/sensors/SENSOR-CO2-001/readings**

2. **POST a new reading (updates sensor's currentValue)

bash
curl -X POST http://localhost:8080/api/v1/sensors/SENSOR-CO2-001/readings \\
  -H "Content-Type: application/json" \\
  -d '{"value":430.5}**

3. **POST another reading

bash
curl -X POST http://localhost:8080/api/v1/sensors/SENSOR-CO2-001/readings \\
  -H "Content-Type: application/json" \\
  -d '{"value":445.2}'**

4. **GET all readings again (shows history)

bash**

&#x20;  **curl -X GET http://localhost:8080/api/v1/sensors/SENSOR-CO2-001/readings**



**5. GET a specific reading by ID**



&#x20;  **bash**

&#x20;  **curl -X GET http://localhost:8080/api/v1/sensors/SENSOR-CO2-001/readings/{readingId}**



**6. Verify sensor's currentValue was updated (Side Effect)**



&#x20;  **bash**

&#x20;  **curl -X GET http://localhost:8080/api/v1/sensors/SENSOR-CO2-001**



**7. Try to get readings for non-existent sensor (404 error)**



&#x20;  **bash**

&#x20;  **curl -X GET http://localhost:8080/api/v1/sensors/INVALID-SENSOR/readings**





**Sample Responses**



**GET /sensors/{sensorId}/readings (200 OK):**



**json**

**\[**

&#x20; **{**

&#x20;   **"id": "reading-uuid-001",**

&#x20;   **"timestamp": 1734567890123,**

&#x20;   **"value": 430.5**

&#x20; **},**

&#x20; **{**

&#x20;   **"id": "reading-uuid-002",**

&#x20;   **"timestamp": 1734567890456,**

&#x20;   **"value": 445.2**

&#x20; **}**

**]**





**POST /sensors/{sensorId}/readings (201 Created):**



**json**

**{**

&#x20; **"id": "reading-uuid-003",**

&#x20; **"timestamp": 1734567890789,**

&#x20; **"value": 430.5**

**}**





**GET /sensors/{sensorId} after readings (200 OK) - Side Effect:**



**json**

**{**

&#x20; **"id": "SENSOR-CO2-001",**

&#x20; **"type": "CO2",**

&#x20; **"status": "ACTIVE",**

&#x20; **"currentValue": 445.2,**

&#x20; **"roomId": "LIB-301"**

**}**





**GET readings for non-existent sensor (404 Not Found):**



**json**

**{**

&#x20; **"error": "Sensor not found with id: INVALID-SENSOR"**

**}**





**Part 5: Advanced Error Handling \& Logging**

**Custom Exception Mappers**

**Exception	HTTP Status	When it occurs**

**RoomNotEmptyException	409 Conflict	Deleting room with active sensors**

**LinkedResourceNotFoundException	422 Unprocessable Entity	POST sensor with invalid roomId**

**SensorUnavailableException	403 Forbidden	POST reading to sensor in MAINTENANCE/OFFLINE**

**GlobalExceptionMapper	500 Internal Server Error	Any unexpected runtime error**

**Sample Error Responses**

**409 Conflict - Delete room with sensors:**



**json**

**{**

&#x20; **"error": "Cannot delete room: active sensors still assigned"**

**}**

**422 Unprocessable Entity - Invalid roomId:**



**json**

**{**

&#x20; **"error": "Unprocessable Entity",**

&#x20; **"message": "Room not found with id: INVALID-ROOM",**

&#x20; **"resourceType": "Room",**

&#x20; **"resourceId": "INVALID-ROOM"**

**}**

**403 Forbidden - Sensor in MAINTENANCE:**



**json**

**{**

&#x20; **"error": "Forbidden",**

&#x20; **"message": "Sensor SENSOR-CO2-003 is currently in MAINTENANCE status and cannot accept new readings",**

&#x20; **"sensorId": "SENSOR-CO2-003",**

&#x20; **"currentStatus": "MAINTENANCE"**

**}**

**500 Internal Server Error (no stack trace exposed):**



**json**

**{**

&#x20; **"error": "Internal Server Error",**

&#x20; **"message": "An unexpected error occurred. Please try again later."**

**}**

**Logging Filter**

**A LoggingFilter implements both ContainerRequestFilter and ContainerResponseFilter to log all requests and responses:**



**text**

**INFO: → REQUEST: GET http://localhost:8080/api/v1/rooms**

**INFO: ← RESPONSE: GET http://localhost:8080/api/v1/rooms - Status: 200**

**INFO: → REQUEST: POST http://localhost:8080/api/v1/sensors**

**INFO: ← RESPONSE: POST http://localhost:8080/api/v1/sensors - Status: 201**

**Sample curl Commands for Error Testing**

**1. Delete room with sensors (409 Conflict)**



**bash**

**curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301**

**2. Create sensor with invalid roomId (422 Unprocessable Entity)**



**bash**

**curl -X POST http://localhost:8080/api/v1/sensors \\**

&#x20; **-H "Content-Type: application/json" \\**

&#x20; **-d '{"type":"CO2","status":"ACTIVE","currentValue":100,"roomId":"INVALID-ROOM"}'**

**3. Add reading to MAINTENANCE sensor (403 Forbidden)**



**bash**

**curl -X POST http://localhost:8080/api/v1/sensors/SENSOR-CO2-003/readings \\**

&#x20; **-H "Content-Type: application/json" \\**

&#x20; **-d '{"value":100}'**

**4. Get non-existent sensor (404 Not Found)**



**bash**

**curl -X GET http://localhost:8080/api/v1/sensors/FAKE-SENSOR/readings**



**Project Structure**

**SmartCampusAPI/**

**├── src/main/java/com/smartcampus/**

**│   ├── ApplicationConfig.java**

**│   ├── exception/**

**│   │   ├── LinkedResourceNotFoundException.java**

**│   │   ├── RoomNotEmptyException.java**

**│   │   └── SensorUnavailableException.java**

**│   ├── filter/**

**│   │   └── LoggingFilter.java**

**│   ├── mapper/**

**│   │   ├── GlobalExceptionMapper.java**

**│   │   ├── LinkedResourceNotFoundExceptionMapper.java**

**│   │   ├── RoomNotEmptyExceptionMapper.java**

**│   │   └── SensorUnavailableExceptionMapper.java**

**│   ├── model/**

**│   │   ├── Room.java**

**│   │   ├── Sensor.java**

**│   │   └── SensorReading.java**

**│   ├── resource/**

**│   │   ├── DiscoveryResource.java**

**│   │   ├── RoomResource.java**

**│   │   ├── SensorResource.java**

**│   │   └── SensorReadingResource.java**

**│   ├── service/**

**│   │   └── RoomService.java**

**│   └── storage/**

**│       └── DataStore.java**

**├── pom.xml**

**└── README.md**







**Data Storage (Sample Data)**

**The API comes with pre-loaded sample data:**



**Room ID	Name	Capacity**

**LIB-301	Library Quiet Study	50**

**CS-101	Computer Science Lab	35**

**ENG-202	Engineering Workshop	40**

**Sensor ID	Type	Status	Room**

**SENSOR-CO2-001	CO2	ACTIVE	LIB-301**

**SENSOR-TEMP-001	Temperature	ACTIVE	LIB-301**

**SENSOR-CO2-003	CO2	MAINTENANCE	ENG-202**







**Author**

**Hanaa Ajuward**



**Course**

**Client-Server Architectures (5COSC022W) - University of Westminster**

