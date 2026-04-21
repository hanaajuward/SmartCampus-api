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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RoomService {
    
    private static RoomService instance;
    private final ConcurrentHashMap<String, Room> rooms;
    
    private RoomService() {
        rooms = new ConcurrentHashMap<>();
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
        return rooms.remove(id);
    }
    
    public boolean roomExists(String id) {
        return rooms.containsKey(id);
    }
    
    public boolean hasActiveSensors(String roomId) {
        Room room = rooms.get(roomId);
        return room != null && !room.getSensorIds().isEmpty();
    }
}
