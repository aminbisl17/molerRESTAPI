package com.example.moler.dto.Notification;

import java.util.List;

public record DergoNjoftimRequest(
        List<Marresi> marresit, // one or more workers / managers
        String titulli,         // notification title (subject)
        String mesazhi          // notification body
) {}
