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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RoomService {
    
    private static RoomService instance;
    private final ConcurrentHashMap<String, Room> rooms;
    private final ConcurrentHashMap<String, Sensor> sensors;  // Sensor storage
    
    private RoomService() {
        rooms = new ConcurrentHashMap<>();
        sensors = new ConcurrentHashMap<>();  
        
        // Sample room
        Room sample = new Room("LIB-301", "Library Quiet Study", 50);
        rooms.put(sample.getId(), sample);
    }
    
    public static synchronized RoomService getInstance() {
        if (instance == null) {
            instance = new RoomService();
        }
        return instance;
    }
    
    // ========== Room Methods ==========
    public List<Room> getAllRooms() {
        return new ArrayList<>(rooms.values());
    }
    
    public Room getRoom(String id) {
        return rooms.get(id);
    }
    
    public Room createRoom(Room room) {
        if (room.getId() == null || room.getId().isEmpty()) {
            room.setId(UUID.randomUUID().toString());
        }
        rooms.put(room.getId(), room);
        return room;
    }
    
    public Room deleteRoom(String id) {
        Room room = rooms.get(id);
        if (room != null && !room.getSensorIds().isEmpty()) {
            throw new IllegalStateException("Room has active sensors: " + room.getSensorIds().size());
        }
        return rooms.remove(id);
    }
    
    public boolean roomExists(String id) {
        return rooms.containsKey(id);
    }
    
    public boolean hasActiveSensors(String roomId) {
        Room room = rooms.get(roomId);
        return room != null && !room.getSensorIds().isEmpty();
    }
    
    // ========== Sensor Methods ==========
    
    // Get all sensors (optionally filtered by type)
    public List<Sensor> getAllSensors(String typeFilter) {
        List<Sensor> allSensors = new ArrayList<>(sensors.values());
        
        if (typeFilter != null && !typeFilter.isEmpty()) {
            return allSensors.stream()
                .filter(s -> s.getType().equalsIgnoreCase(typeFilter))
                .collect(Collectors.toList());
        }
        return allSensors;
    }
    
    // Get sensor by ID
    public Sensor getSensor(String id) {
        return sensors.get(id);
    }
    
    // Create new sensor (with room validation)
    public Sensor createSensor(Sensor sensor) {
        // Validate room exists
        if (!roomExists(sensor.getRoomId())) {
            throw new IllegalArgumentException("Room not found: " + sensor.getRoomId());
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
        sensors.put(sensor.getId(), sensor);
        
        // Link sensor to room
        Room room = rooms.get(sensor.getRoomId());
        room.addSensorId(sensor.getId());
        
        return sensor;
    }
    
    // Update sensor current value 
    public Sensor updateSensorValue(String sensorId, double newValue) {
        Sensor sensor = sensors.get(sensorId);
        if (sensor != null) {
            sensor.setCurrentValue(newValue);
        }
        return sensor;
    }
    
    // Check if sensor exists
    public boolean sensorExists(String id) {
        return sensors.containsKey(id);
    }
    
    // Get sensors by room ID
    public List<Sensor> getSensorsByRoom(String roomId) {
        return sensors.values().stream()
            .filter(s -> s.getRoomId().equals(roomId))
            .collect(Collectors.toList());
    }
}