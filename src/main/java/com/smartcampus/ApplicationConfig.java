/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus;

/**
 *
 * @author Hanaa Ajuward
 */


import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/api/v1")
public class ApplicationConfig extends Application {
    
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> resources = new HashSet<>();
        
        resources.add(com.smartcampus.resource.DiscoveryResource.class);
        resources.add(com.smartcampus.resource.SensorRoom.class);  
        resources.add(com.smartcampus.resource.SensorResource.class);
        
        // Exception Mappers
        resources.add(com.smartcampus.mapper.RoomNotEmptyExceptionMapper.class);
        resources.add(com.smartcampus.mapper.LinkedResourceNotFoundExceptionMapper.class);
        resources.add(com.smartcampus.mapper.SensorUnavailableExceptionMapper.class);
        resources.add(com.smartcampus.mapper.GlobalExceptionMapper.class);
        
        // Logging Filter
        resources.add(com.smartcampus.filter.LoggingFilter.class);
        
        return resources;
    }
}