/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.resource;

/**
 *
 * @author Hanaa Ajuward
 */


import com.smartcampus.model.Sensor;
import com.smartcampus.service.RoomService;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final RoomService roomService = RoomService.getInstance();

    // GET /api/v1/sensors - Get all sensors (optionally filtered by type)
    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        List<Sensor> sensors = roomService.getAllSensors(type);
        return Response.ok(sensors).build();
    }

    // POST /api/v1/sensors - Create new sensor (with room validation)
    @POST
    public Response createSensor(Sensor sensor) {
        // Validate required fields
        if (sensor.getType() == null || sensor.getType().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"Sensor type is required\"}")
                .build();
        }
        if (sensor.getRoomId() == null || sensor.getRoomId().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"roomId is required\"}")
                .build();
        }

        // LinkedResourceNotFoundException will propagate to
        // LinkedResourceNotFoundExceptionMapper → 422
        Sensor created = roomService.createSensor(sensor);
        return Response
            .created(URI.create("/api/v1/sensors/" + created.getId()))
            .entity(created)
            .build();
    }

    // GET /api/v1/sensors/{sensorId} - Get specific sensor
    @GET
    @Path("/{sensorId}")
    public Response getSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = roomService.getSensor(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\": \"Sensor not found with id: " + sensorId + "\"}")
                .build();
        }
        return Response.ok(sensor).build();
    }

    // Sub-resource locator — delegates nested /readings paths to SensorReadingResource
    @Path("/{sensorId}/readings")
    public SensorReadingResource getSensorReadingResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }
}