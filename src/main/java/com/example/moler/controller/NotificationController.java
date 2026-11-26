package com.example.moler.controller;

import com.example.moler.authentication.JwtUtil;
import com.example.moler.dto.Notification.DergoNjoftimRequest;
import com.example.moler.service.NotificationService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtUtil jwtUtil;

    public NotificationController(NotificationService notificationService, JwtUtil jwtUtil) {
        this.notificationService = notificationService;
        this.jwtUtil = jwtUtil;
    }

    // POST /notifications/send — manager sends a notification to workers and/or managers
    @PostMapping("/send")
    public ResponseEntity<?> dergoNjoftim(
            @RequestHeader("Authorization") String authorization,
            @RequestBody DergoNjoftimRequest request) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Access token is missing"));
        }

        String token = authorization.substring(7);

        // Only managers can send notifications
        if (!"ROLE_MENAXHER".equals(jwtUtil.extractRole(token))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Nevoitet qasja e menaxherit"));
        }

        if (request == null) {
            throw new IllegalArgumentException("Te dhenat e njoftimit mungojne");
        }

        // Accepted: delivery to Expo happens in the background
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                notificationService.dergoTeMarresTeVerifikuar(
                        request.marresit(),
                        request.titulli(),
                        request.mesazhi()
                )
        );
    }
}
