package com.example.moler.dto.Notification;

public record DergoNjoftimResponse(
        int marresit, // distinct recipients in the request
        int pajisjet  // devices (push tokens) the notification was queued for
) {}
