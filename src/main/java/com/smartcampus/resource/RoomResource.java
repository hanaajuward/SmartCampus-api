/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.resource;

/**
 *
 * @author Hanaa Ajuward
 */

import com.smartcampus.model.Room;
import com.smartcampus.service.RoomService;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {
    
    private final RoomService roomService = RoomService.getInstance();
    
    @GET
    public Response getAllRooms() {
        List<Room> rooms = roomService.getAllRooms();
        return Response.ok(rooms).build();
    }
    
    @POST
    public Response createRoom(Room room) {
        Room created = roomService.createRoom(room);
        return Response
            .created(URI.create("/api/v1/rooms/" + created.getId()))
            .entity(created)
            .build();
    }
    
    @GET
    @Path("/{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        Room room = roomService.getRoom(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\": \"Room not found\"}")
                .build();
        }
        return Response.ok(room).build();
    }
    
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = roomService.getRoom(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\": \"Room not found\"}")
                .build();
        }
        
        if (roomService.hasActiveSensors(roomId)) {
            return Response.status(Response.Status.CONFLICT)
                .entity("{\"error\": \"Cannot delete room: active sensors still assigned\"}")
                .build();
        }
        
        roomService.deleteRoom(roomId);
        return Response.noContent().build();
    }
}