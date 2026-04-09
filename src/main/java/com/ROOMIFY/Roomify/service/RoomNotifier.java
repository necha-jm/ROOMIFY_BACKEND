package com.ROOMIFY.Roomify.service;

import com.ROOMIFY.Roomify.model.Room;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class RoomNotifier {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void notifyRoomUpdate(Room room) {
        // Sends update to all clients subscribed to /topic/rooms
        messagingTemplate.convertAndSend("/topic/rooms", room);
    }
}