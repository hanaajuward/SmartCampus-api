/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.service;

/**
 *
 * @author Hanaa Ajuward
 */


import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.storage.DataStore;
import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.exception.SensorUnavailableException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class RoomService {
    
    private static RoomService instance;
    private final DataStore dataStore;
    
    private RoomService() {
        dataStore = DataStore.getInstance();
    }
    
    public static synchronized RoomService getInstance() {
        if (instance == null) {
            instance = new RoomService();
        }
        return instance;
    }
    
    // ========== Room Methods ==========
    public List<Room> getAllRooms() {
        return new ArrayList<>(dataStore.getRooms().values());
    }
    
    public Room getRoom(String id) {
        return dataStore.getRooms().get(id);
    }
    
    public Room createRoom(Room room) {
        if (room.getId() == null || room.getId().isEmpty()) {
            room.setId(UUID.randomUUID().toString());
        }
        dataStore.getRooms().put(room.getId(), room);
        return room;
    }
    
    public Room deleteRoom(String id) {
        Room room = dataStore.getRooms().get(id);
        if (room != null && !room.getSensorIds().isEmpty()) {
            // Throw custom exception for 409 Conflict
            throw new RoomNotEmptyException(id, room.getSensorIds().size());
        }
        return dataStore.getRooms().remove(id);
    }
    
    public boolean roomExists(String id) {
        return dataStore.getRooms().containsKey(id);
    }
    
    public boolean hasActiveSensors(String roomId) {
        Room room = dataStore.getRooms().get(roomId);
        return room != null && !room.getSensorIds().isEmpty();
    }
    
    // ========== Sensor Methods ==========
    public List<Sensor> getAllSensors(String typeFilter) {
        List<Sensor> allSensors = new ArrayList<>(dataStore.getSensors().values());
        
        if (typeFilter != null && !typeFilter.isEmpty()) {
            return allSensors.stream()
                .filter(s -> s.getType().equalsIgnoreCase(typeFilter))
                .collect(Collectors.toList());
        }
        return allSensors;
    }
    
    public Sensor getSensor(String id) {
        return dataStore.getSensors().get(id);
    }
    
    public Sensor createSensor(Sensor sensor) {
        // Validate room exists - throws LinkedResourceNotFoundException if not found
        if (!roomExists(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException("Room", sensor.getRoomId());
        }
        
        // Generate ID if not provided
        if (sensor.getId() == null || sensor.getId().isEmpty()) {
            sensor.setId(UUID.randomUUID().toString());
        }
        
        // Set default status if not provided
        if (sensor.getStatus() == null || sensor.getStatus().isEmpty()) {
            sensor.setStatus("ACTIVE");
        }
        
        // Store sensor
        dataStore.getSensors().put(sensor.getId(), sensor);
        
        // Initialize empty reading history for this sensor
        dataStore.getSensorReadings().putIfAbsent(sensor.getId(), new ArrayList<>());
        
        // Link sensor to room
        Room room = dataStore.getRooms().get(sensor.getRoomId());
        room.addSensorId(sensor.getId());
        
        return sensor;
    }
    
    public boolean sensorExists(String id) {
        return dataStore.getSensors().containsKey(id);
    }
    
    public List<Sensor> getSensorsByRoom(String roomId) {
        return dataStore.getSensors().values().stream()
            .filter(s -> s.getRoomId().equals(roomId))
            .collect(Collectors.toList());
    }
    
    // ========== Sensor Reading Methods ==========
    public List<SensorReading> getSensorReadings(String sensorId) {
        return dataStore.getSensorReadings().getOrDefault(sensorId, new ArrayList<>());
    }
    
    public SensorReading addSensorReading(String sensorId, SensorReading reading) {
        // Check if sensor exists - throws LinkedResourceNotFoundException if not found
        if (!sensorExists(sensorId)) {
            throw new LinkedResourceNotFoundException("Sensor", sensorId);
        }
        
        // Check sensor status - if MAINTENANCE or OFFLINE, throw SensorUnavailableException
        Sensor sensor = dataStore.getSensors().get(sensorId);
        if ("MAINTENANCE".equals(sensor.getStatus()) || "OFFLINE".equals(sensor.getStatus())) {
            throw new SensorUnavailableException(sensorId, sensor.getStatus());
        }
        
        // Generate ID if not provided
        if (reading.getId() == null || reading.getId().isEmpty()) {
            reading.setId(UUID.randomUUID().toString());
        }
        
        // Set timestamp if not provided
        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }
        
        // Add reading to history
        dataStore.getSensorReadings().putIfAbsent(sensorId, new ArrayList<>());
        dataStore.getSensorReadings().get(sensorId).add(reading);
        
        // SIDE EFFECT: Update the sensor's currentValue
        sensor.setCurrentValue(reading.getValue());
        
        return reading;
    }
}