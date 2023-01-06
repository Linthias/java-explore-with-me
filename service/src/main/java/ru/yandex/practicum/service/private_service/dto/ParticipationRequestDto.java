package ru.yandex.practicum.service.private_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipationRequestDto {
    private String created;
    private long event;
    private long id;
    private long requester;
    private String status;
}
