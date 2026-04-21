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

**5. Verify API is running at: http://localhost:8080/SmartCampusAPI-1.0-SNAPSHOT/api/v1/**





**Part 1: Service Architecture**



**Discovery Endpoint**

**GET `/api/v1/`**



**```bash**

**curl -X GET http://localhost:8080/SmartCampusAPI-1.0-SNAPSHOT/api/v1**



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

**curl -X GET http://localhost:8080/SmartCampusAPI-1.0-SNAPSHOT/api/v1/rooms**



**2. GET room by ID**



**bash**

**curl -X GET http://localhost:8080/SmartCampusAPI-1.0-SNAPSHOT/api/v1/rooms/LIB-301**



**3. POST create new room**



**bash**

**curl -X POST http://localhost:8080/SmartCampusAPI-1.0-SNAPSHOT/api/v1/rooms \\**

&#x20; **-H "Content-Type: application/json" \\**

&#x20; **-d '{"name":"CS Lab","capacity":35}'**



**4. DELETE a room (replace {id} with actual ID)**



**bash**

**curl -X DELETE http://localhost:8080/SmartCampusAPI-1.0-SNAPSHOT/api/v1/rooms/{id}**



**5. GET discovery endpoint**



**bash**

**curl -X GET http://localhost:8080/SmartCampusAPI-1.0-SNAPSHOT/api/v1/**

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





