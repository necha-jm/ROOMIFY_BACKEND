package com.ROOMIFY.Roomify.model;

public class RoomMessage {

    public enum Action {
        ADD,
        UPDATE,
        DELETE
    }

    private Action action;
    private Room room;

    // Constructors
    public RoomMessage() {}
    public RoomMessage(Action action, Room room) {
        this.action = action;
        this.room = room;
    }

    // Getters and Setters
    public Action getAction() { return action; }
    public void setAction(Action action) { this.action = action; }

    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
}
