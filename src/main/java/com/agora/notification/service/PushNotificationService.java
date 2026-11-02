package com.agora.notification.service;

import com.agora.notification.model.NotificationToken;
import com.agora.notification.repository.NotificationTokenRepository;
import com.agora.user.model.User;
import com.agora.user.repository.UserRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class PushNotificationService {
    private static final Logger logger = LoggerFactory.getLogger(PushNotificationService.class);
    
    @Autowired(required = false)
    private FirebaseMessaging firebaseMessaging;

    private final NotificationTokenRepository notificationTokenRepository;
    private final UserRepository userRepository;

    public void registerToken(Long userId, String firebaseToken, String platform) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        NotificationToken existingToken = notificationTokenRepository.findByFirebaseToken(firebaseToken)
            .orElse(null);

        if (existingToken != null) {
            // Update existing token
            existingToken.setLastUsed(LocalDateTime.now());
            existingToken.setPlatform(platform);
            notificationTokenRepository.save(existingToken);
        } else {
            // Create new token
            NotificationToken token = NotificationToken.builder()
                .user(user)
                .firebaseToken(firebaseToken)
                .platform(platform)
                .lastUsed(LocalDateTime.now())
                .build();
            notificationTokenRepository.save(token);
        }
    }

    public void sendNotificationToUser(Long userId, String title, String body) {
        NotificationToken token = notificationTokenRepository.findByUserId(userId)
            .orElse(null);

        if (token != null) {
            sendFirebaseNotification(token.getFirebaseToken(), title, body);
            logger.info("✅ Notification sent to user: {} via Firebase", userId);
        } else {
            logger.warn("⚠️ No Firebase token found for user: {}. User may not have registered device yet.", userId);
        }
    }

    public void sendNotificationToUser(Long userId, String title, String body, java.util.Map<String, String> data) {
        NotificationToken token = notificationTokenRepository.findByUserId(userId)
            .orElse(null);

        if (token != null) {
            sendFirebaseNotification(token.getFirebaseToken(), title, body, data);
            logger.info("✅ Notification sent to user: {} via Firebase with data", userId);
        } else {
            logger.warn("⚠️ No Firebase token found for user: {}. User may not have registered device yet.", userId);
        }
    }

    private void sendFirebaseNotification(String deviceToken, String title, String body) {
        sendFirebaseNotification(deviceToken, title, body, null);
    }

    private void sendFirebaseNotification(String deviceToken, String title, String body, java.util.Map<String, String> data) {
        if (firebaseMessaging == null) {
            logger.warn("FirebaseMessaging not initialized. Make sure FIREBASE_CREDENTIALS_PATH points to the service account JSON.");
            return;
        }

        try {
            Message.Builder messageBuilder = Message.builder()
                .setToken(deviceToken)
                .setNotification(Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build());

            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            String response = firebaseMessaging.send(messageBuilder.build());
            logger.info("✅ Firebase notification sent successfully. Message ID: {}", response);
        } catch (Exception e) {
            logger.error("❌ Failed to send Firebase notification to {}: {}", deviceToken, e.getMessage());
        }
    }

    @lombok.Data
    @lombok.Builder
    public static class PushMessage {
        private String to;
        private String sound;
        private String title;
        private String body;
        private java.util.Map<String, String> data;
    }
}
