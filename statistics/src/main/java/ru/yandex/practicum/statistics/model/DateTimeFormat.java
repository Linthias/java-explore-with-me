package ru.yandex.practicum.statistics.model;

import lombok.Getter;

import java.time.format.DateTimeFormatter;

@Getter
public class DateTimeFormat {
    private final DateTimeFormatter formatter;

    public DateTimeFormat() {
        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    }
}
