/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.storage;

/**
 *
 * @author Hanaa Ajuward
 */

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataStore {
    private static final DataStore INSTANCE = new DataStore();
    
    private final Map<String, Room> rooms;
    private final Map<String, Sensor> sensors;
    private final Map<String, List<SensorReading>> sensorReadings;

    private DataStore() {
        rooms = new ConcurrentHashMap<>();
        sensors = new ConcurrentHashMap<>();
        sensorReadings = new ConcurrentHashMap<>();
        
        // ========== SAMPLE DATA ==========
        
        // Rooms
        Room room1 = new Room("LIB-301", "Library Quiet Study", 50);
        Room room2 = new Room("CS-101", "Computer Science Lab", 35);
        Room room3 = new Room("ENG-202", "Engineering Workshop", 40);
        room1.addSensorId("SENSOR-CO2-001");
        room1.addSensorId("SENSOR-TEMP-001");
        
        rooms.put("LIB-301", room1);
        rooms.put("CS-101", room2);
        rooms.put("ENG-202", room3);
        
        // Sensors
        Sensor sensor1 = new Sensor("SENSOR-CO2-001", "CO2", "ACTIVE", 420.5, "LIB-301");
        Sensor sensor2 = new Sensor("SENSOR-TEMP-001", "Temperature", "ACTIVE", 22.5, "LIB-301");
        Sensor sensor3 = new Sensor("SENSOR-CO2-003", "CO2", "MAINTENANCE", 0, "ENG-202");

        sensors.put("SENSOR-CO2-001", sensor1);
        sensors.put("SENSOR-TEMP-001", sensor2);
        sensors.put("SENSOR-CO2-003", sensor3);
        
        // Empty reading lists for sensors
        sensorReadings.put("SENSOR-CO2-001", new ArrayList<>());
        sensorReadings.put("SENSOR-TEMP-001", new ArrayList<>());
        sensorReadings.put("SENSOR-CO2-003", new ArrayList<>());
    }

    public static DataStore getInstance() {
        return INSTANCE;
    }

    public Map<String, Room> getRooms() { return rooms; }
    public Map<String, Sensor> getSensors() { return sensors; }
    public Map<String, List<SensorReading>> getSensorReadings() { return sensorReadings; }
}