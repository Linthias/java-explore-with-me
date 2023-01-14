package ru.yandex.practicum.service.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventShortDto {
    protected String annotation;
    protected CategoryDto category;
    protected long confirmedRequests;
    protected String eventDate;
    protected Long id;
    protected UserShortDto initiator;
    protected boolean paid;
    protected String title;
    protected long views;
}
