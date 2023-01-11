package ru.yandex.practicum.service.shared.dto;

import ru.yandex.practicum.service.shared.model.Category;
import ru.yandex.practicum.service.shared.model.DateTimeFormat;
import ru.yandex.practicum.service.shared.model.Event;
import ru.yandex.practicum.service.shared.model.User;

public class EventDtoMapper {
    public static EventShortDto toShortDto(Event event, Category category, User user) {
        return EventShortDto.builder()
                .annotation(event.getAnnotation())
                .category(new CategoryDto(category.getId(), category.getName()))
                .confirmedRequests(event.getConfirmedRequests())
                .eventDate(event.getEventDate().format(new DateTimeFormat().getFormatter()))
                .id(event.getId())
                .initiator(new UserShortDto(user.getId(), user.getName()))
                .paid(event.isPaid())
                .title(event.getTitle())
                .views(event.getViews())
                .build();
    }

    public static EventFullDto toFullDto(Event event, Category category, User user) {
        String published;
        if (event.getPublishedOn() == null) {
            published = null;
        } else {
            published = event.getPublishedOn().format(new DateTimeFormat().getFormatter());
        }

        return EventFullDto.builder()
                .annotation(event.getAnnotation())
                .category(new CategoryDto(category.getId(), category.getName()))
                .confirmedRequests(event.getConfirmedRequests())
                .eventDate(event.getEventDate().format(new DateTimeFormat().getFormatter()))
                .id(event.getId())
                .initiator(new UserShortDto(user.getId(), user.getName()))
                .paid(event.isPaid())
                .title(event.getTitle())
                .views(event.getViews())
                .createdOn(event.getCreatedOn().format(new DateTimeFormat().getFormatter()))
                .description(event.getDescription())
                .location(new Location(event.getLatitude(), event.getLongtitude()))
                .participantLimit(event.getParticipantLimit())
                .publishedOn(published)
                .requestModeration(event.isModerationRequired())
                .state(event.getState().toString())
                .build();
    }
}
