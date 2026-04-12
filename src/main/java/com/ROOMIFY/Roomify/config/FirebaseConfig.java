package com.ROOMIFY.Roomify.config;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class FirebaseConfig {

    @PostConstruct
    public void init() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {

                GoogleCredentials credentials =
                        GoogleCredentials.fromStream(
                                new ClassPathResource("firebase-service-account.json").getInputStream()
                        );

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .build();

                FirebaseApp.initializeApp(options);

                System.out.println("Firebase initialized successfully");
            }

        } catch (IOException e) {
            throw new RuntimeException("Firebase initialization failed", e);
        }
    }
}
