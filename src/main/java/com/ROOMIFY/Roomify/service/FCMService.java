package com.ROOMIFY.Roomify.service;

import com.google.firebase.messaging.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class FCMService {

    // Send to single user
    public void sendNotificationToUser(
            String fcmToken,
            String title,
            String body,
            Map<String, String> data) {

        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putAllData(data)
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setSound("default")
                                    .build())
                            .build())
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);

            System.out.println("Notification sent: " + response);

        } catch (FirebaseMessagingException e) {
            e.printStackTrace();
        }
    }

    // Send to multiple users
    public void sendNotificationToMultipleUsers(
            List<String> tokens,
            String title,
            String body) {

        try {
            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(tokens)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();

            BatchResponse response =
                    FirebaseMessaging.getInstance().sendEachForMulticast(message);

            System.out.println("Success: " + response.getSuccessCount());

        } catch (FirebaseMessagingException e) {
            e.printStackTrace();
        }
    }
}
