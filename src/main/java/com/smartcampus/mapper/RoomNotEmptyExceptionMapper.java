/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.mapper;

/**
 *
 * @author Hanaa Ajuward
 */

import com.smartcampus.exception.RoomNotEmptyException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {
    
    @Override
    public Response toResponse(RoomNotEmptyException exception) {
        String jsonError = String.format(
            "{\"error\": \"Cannot delete room\", \"message\": \"%s\", \"roomId\": \"%s\", \"activeSensors\": %d}",
            exception.getMessage(),
            exception.getRoomId(),
            exception.getSensorCount()
        );
        
        return Response.status(Response.Status.CONFLICT)  // 409 Conflict
                .entity(jsonError)
                .build();
    }
}
