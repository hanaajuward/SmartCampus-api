/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.resource;

/**
 *
 * @author Hanaa Ajuward
 */

import com.smartcampus.model.SensorReading;
import com.smartcampus.service.RoomService;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final RoomService roomService = RoomService.getInstance();
    private String sensorId;

    // No-arg constructor required by JAX-RS
    public SensorReadingResource() {}

    // Constructor called by sub-resource locator in SensorResource
    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    // GET /api/v1/sensors/{sensorId}/readings
    @GET
    public Response getAllReadings() {
        if (!roomService.sensorExists(sensorId)) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\": \"Sensor not found with id: " + sensorId + "\"}")
                .build();
        }
        List<SensorReading> readings = roomService.getSensorReadings(sensorId);
        return Response.ok(readings).build();
    }

    // POST /api/v1/sensors/{sensorId}/readings
    @POST
    public Response addReading(SensorReading reading) {
        if (!roomService.sensorExists(sensorId)) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\": \"Sensor not found with id: " + sensorId + "\"}")
                .build();
        }
        if (reading.getValue() < 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"Reading value cannot be negative\"}")
                .build();
        }

        // SensorUnavailableException will propagate to
        // SensorUnavailableExceptionMapper → 403
        SensorReading created = roomService.addSensorReading(sensorId, reading);
        return Response
            .created(URI.create("/api/v1/sensors/" + sensorId + "/readings/" + created.getId()))
            .entity(created)
            .build();
    }

    // GET /api/v1/sensors/{sensorId}/readings/{readingId}
    @GET
    @Path("/{readingId}")
    public Response getReadingById(@PathParam("readingId") String readingId) {
        if (!roomService.sensorExists(sensorId)) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\": \"Sensor not found with id: " + sensorId + "\"}")
                .build();
        }
        List<SensorReading> readings = roomService.getSensorReadings(sensorId);
        SensorReading found = readings.stream()
            .filter(r -> r.getId().equals(readingId))
            .findFirst()
            .orElse(null);

        if (found == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\": \"Reading not found with id: " + readingId + "\"}")
                .build();
        }
        return Response.ok(found).build();
    }
}