/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.model;

/**
 *
 * @author Hanaa Ajuward
 */

public class Sensor {
    private String id;           // Unique identifier, e.g., "TEMP-001"
    private String type;         // Category, e.g., "Temperature", "Occupancy", "CO2"
    private String status;       // Current state: "ACTIVE", "MAINTENANCE", or "OFFLINE"
    private double currentValue; // The most recent measurement recorded
    private String roomId;       // Foreign key linking to the Room where the sensor is located
    
    // Default constructor for JSON deserialization
    public Sensor() {}
    
    // Parameterized constructor
    public Sensor(String id, String type, String status, double currentValue, String roomId) {
        this.id = id;
        this.type = type;
        this.status = status;
        this.currentValue = currentValue;
        this.roomId = roomId;
    }
    
    // Getters
    public String getId() { return id; }
    public String getType() { return type; }
    public String getStatus() { return status; }
    public double getCurrentValue() { return currentValue; }
    public String getRoomId() { return roomId; }
    
    // Setters
    public void setId(String id) { this.id = id; }
    public void setType(String type) { this.type = type; }
    public void setStatus(String status) { this.status = status; }
    public void setCurrentValue(double currentValue) { this.currentValue = currentValue; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
}
