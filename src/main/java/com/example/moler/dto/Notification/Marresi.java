package com.example.moler.dto.Notification;

/**
 * One notification recipient: a worker or a manager by id.
 */
public record Marresi(
        String roli, // "puntor" or "menaxher"
        Long id
) {
    public static Marresi puntor(Long id) {
        return new Marresi("puntor", id);
    }

    public static Marresi menaxher(Long id) {
        return new Marresi("menaxher", id);
    }
}
