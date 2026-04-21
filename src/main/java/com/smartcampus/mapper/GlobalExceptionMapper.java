/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.mapper;

/**
 *
 * @author Hanaa Ajuward
 */

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Logger;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {
    
    private static final Logger logger = Logger.getLogger(GlobalExceptionMapper.class.getName());
    
    @Override
    public Response toResponse(Throwable exception) {
        // Log the full stack trace for debugging (server-side only)
        logger.severe("Unexpected error occurred: " + exception.getMessage());
        exception.printStackTrace();
        
        // Return generic error to client (no stack trace exposure)
        String jsonError = "{\"error\": \"Internal Server Error\", \"message\": \"An unexpected error occurred. Please try again later.\"}";
        
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)  // 500
                .entity(jsonError)
                .build();
    }
}
