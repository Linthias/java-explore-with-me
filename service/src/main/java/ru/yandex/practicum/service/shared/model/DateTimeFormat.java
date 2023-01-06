package ru.yandex.practicum.service.shared.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.format.DateTimeFormatter;

@Getter
public class DateTimeFormat {
    private final DateTimeFormatter formatter;

    public DateTimeFormat() {
        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    }
}
