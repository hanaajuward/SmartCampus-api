/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.mapper;

/**
 *
 * @author Hanaa Ajuward
 */

import com.smartcampus.exception.SensorUnavailableException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class SensorUnavailableExceptionMapper implements ExceptionMapper<SensorUnavailableException> {
    
    @Override
    public Response toResponse(SensorUnavailableException exception) {
        String jsonError = String.format(
            "{\"error\": \"Forbidden\", \"message\": \"%s\", \"sensorId\": \"%s\", \"currentStatus\": \"%s\"}",
            exception.getMessage(),
            exception.getSensorId(),
            exception.getStatus()
        );
        
        return Response.status(Response.Status.FORBIDDEN)  // 403 Forbidden
                .entity(jsonError)
                .build();
    }
}
