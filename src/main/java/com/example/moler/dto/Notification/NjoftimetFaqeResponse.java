package com.example.moler.dto.Notification;

import java.util.List;

/** One page of the user's notifications, newest first. */
public record NjoftimetFaqeResponse(
        List<NjoftimResponse> njoftimet,
        int faqja,       // current page, starting at 0
        int madhesia,    // page size
        long totali,     // total notifications
        boolean kaMeShume // true if there is a next page
) {}
