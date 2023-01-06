package ru.yandex.practicum.statistics.dto;

import ru.yandex.practicum.statistics.model.DateTimeFormat;
import ru.yandex.practicum.statistics.model.EndpointHit;

import java.time.LocalDateTime;

public class EndpointHitDtoMapper {
    public static EndpointHitDto toEndpointHitDto(EndpointHit endpointHit) {
        return EndpointHitDto.builder()
                .id(endpointHit.getId())
                .app(endpointHit.getApp())
                .uri(endpointHit.getUri())
                .ip(endpointHit.getIp())
                .timestamp(endpointHit.getTimestamp().format(new DateTimeFormat().getFormatter()))
                .build();
    }

    public static EndpointHit toEndpointHit(EndpointHitDto endpointHitDto) {
        return EndpointHit.builder()
                .app(endpointHitDto.getApp())
                .uri(endpointHitDto.getUri())
                .ip(endpointHitDto.getIp())
                .timestamp(LocalDateTime.parse(endpointHitDto.getTimestamp(), new DateTimeFormat().getFormatter()))
                .build();
    }
}
