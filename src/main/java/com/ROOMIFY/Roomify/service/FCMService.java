package com.ROOMIFY.Roomify.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Service
public class FCMService {

    private boolean isInitialized = false;

    @PostConstruct
    public void initialize() {
        try {
            InputStream serviceAccount = new ClassPathResource("firebase-admin-sdk.json").getInputStream();

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                isInitialized = true;
                System.out.println("✅ Firebase Admin SDK initialized successfully!");
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to initialize Firebase: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendNotificationToUser(String fcmToken, String title, String body, Map<String, String> data) {
        if (!isInitialized) {
            System.out.println("⚠️ Firebase not initialized");
            return;
        }

        try {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Message.Builder messageBuilder = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(notification);

            // Add data if present
            if (data != null && !data.isEmpty()) {
                for (Map.Entry<String, String> entry : data.entrySet()) {
                    messageBuilder.putData(entry.getKey(), entry.getValue());
                }
            }

            String response = FirebaseMessaging.getInstance().send(messageBuilder.build());
            System.out.println("✅ Notification sent: " + response);

        } catch (Exception e) {
            System.err.println("❌ Failed to send notification: " + e.getMessage());
        }
    }

    public void sendMulticastNotification(List<String> fcmTokens, String title, String body, Map<String, String> data) {
        if (!isInitialized || fcmTokens == null || fcmTokens.isEmpty()) {
            return;
        }

        try {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            MulticastMessage.Builder messageBuilder = MulticastMessage.builder()
                    .addAllTokens(fcmTokens)
                    .setNotification(notification);

            if (data != null && !data.isEmpty()) {
                for (Map.Entry<String, String> entry : data.entrySet()) {
                    messageBuilder.putData(entry.getKey(), entry.getValue());
                }
            }

            BatchResponse response = FirebaseMessaging.getInstance().sendMulticast(messageBuilder.build());
            System.out.println("✅ Sent to " + response.getSuccessCount() + " devices, failed: " + response.getFailureCount());

        } catch (Exception e) {
            System.err.println("❌ Failed to send multicast: " + e.getMessage());
        }
    }
}