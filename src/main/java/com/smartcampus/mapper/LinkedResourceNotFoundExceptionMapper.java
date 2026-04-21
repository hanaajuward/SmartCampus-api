/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.mapper;

/**
 *
 * @author Hanaa Ajuward
 */

import com.smartcampus.exception.LinkedResourceNotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {
    
    @Override
    public Response toResponse(LinkedResourceNotFoundException exception) {
        String jsonError = String.format(
            "{\"error\": \"Unprocessable Entity\", \"message\": \"%s\", \"resourceType\": \"%s\", \"resourceId\": \"%s\"}",
            exception.getMessage(),
            exception.getResourceType(),
            exception.getResourceId()
        );
        
        return Response.status(422)  // 422 Unprocessable Entity
                .entity(jsonError)
                .build();
    }
}
