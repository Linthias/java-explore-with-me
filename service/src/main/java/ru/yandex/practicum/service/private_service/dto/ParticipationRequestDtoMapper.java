package ru.yandex.practicum.service.private_service.dto;

import ru.yandex.practicum.service.shared.model.DateTimeFormat;
import ru.yandex.practicum.service.shared.model.ParticipationRequest;
import ru.yandex.practicum.service.shared.model.RequestState;

public class ParticipationRequestDtoMapper {
    public static ParticipationRequestDto toRequestDto(ParticipationRequest request, RequestState state) {
        return ParticipationRequestDto.builder()
                .created(request.getCreated().format(new DateTimeFormat().getFormatter()))
                .event(request.getEventId())
                .id(request.getId())
                .requester(request.getRequesterId())
                .status(state.getName())
                .build();
    }
}
