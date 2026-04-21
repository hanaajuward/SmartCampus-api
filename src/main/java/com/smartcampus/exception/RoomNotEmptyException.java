/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.exception;

/**
 *
 * @author Hanaa Ajuward
 */

public class RoomNotEmptyException extends RuntimeException {
    
    private final String roomId;
    private final int sensorCount;
    
    public RoomNotEmptyException(String roomId, int sensorCount) {
        super("Room " + roomId + " cannot be deleted because it has " + sensorCount + " active sensor(s)");
        this.roomId = roomId;
        this.sensorCount = sensorCount;
    }
    
    public String getRoomId() {
        return roomId;
    }
    
    public int getSensorCount() {
        return sensorCount;
    }
}