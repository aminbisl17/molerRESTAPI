package com.example.moler.dto.PushToken;

public record PushTokenRequest(
        String token,    // ExponentPushToken[...]
        String platform  // optional: "ios" / "android" (or "i" / "a"); ignored on delete
) {}
