package com.roadsaathi.backend.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

@Service
@Slf4j
public class FCMMessagingService {

    private final ResourceLoader resourceLoader;

    @Value("${app.firebase.config-path}")
    private String firebaseConfigPath;

    public FCMMessagingService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void init() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                Resource resource = resourceLoader.getResource(firebaseConfigPath);
                InputStream serviceAccount = resource.getInputStream();

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(com.google.auth.oauth2.GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("Firebase app initialized successfully");
            }
        } catch (Exception e) {
            log.error("Failed to initialize Firebase app", e);
        }
    }

    public void sendHazardAlert(List<String> fcmTokens, String clusterId, String hazardType,
                                double distanceKm, String aiBrief, double lat, double lng) {
        if (fcmTokens.isEmpty()) {
            return;
        }

        MulticastMessage message = MulticastMessage.builder()
                .setNotification(Notification.builder()
                        .setTitle("Road Hazard Alert")
                        .setBody(aiBrief != null ? aiBrief : "Hazard reported ahead")
                        .build())
                .putData("type", "HAZARD_ALERT")
                .putData("clusterId", clusterId)
                .putData("hazardType", hazardType)
                .putData("distanceKm", String.valueOf(distanceKm))
                .putData("aiBrief", aiBrief != null ? aiBrief : "")
                .putData("lat", String.valueOf(lat))
                .putData("lng", String.valueOf(lng))
                .addAllTokens(fcmTokens)
                .build();

        try {
            var response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
            log.info("Sent FCM hazard alert to {} devices, success: {}, failure: {}",
                    fcmTokens.size(), response.getSuccessCount(), response.getFailureCount());

            if (response.getFailureCount() > 0) {
                response.getResponses().forEach(r -> {
                    if (!r.isSuccessful()) {
                        log.warn("FCM send failed: {}", r.getError());
                    }
                });
            }
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM hazard alert", e);
        }
    }

    public void sendReportStatusUpdate(String fcmToken, String reportId, String newStatus) {
        Message message = Message.builder()
                .setNotification(Notification.builder()
                        .setTitle("Report Status Update")
                        .setBody("Your report #" + reportId + " is now " + newStatus)
                        .build())
                .putData("type", "STATUS_UPDATE")
                .putData("reportId", reportId)
                .putData("newStatus", newStatus)
                .setToken(fcmToken)
                .build();

        try {
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Sent status update FCM, messageId: {}", response);
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send status update FCM", e);
        }
    }
}
