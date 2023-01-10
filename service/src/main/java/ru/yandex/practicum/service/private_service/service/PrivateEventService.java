package ru.yandex.practicum.service.private_service.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.service.private_service.dto.NewEventDto;
import ru.yandex.practicum.service.private_service.dto.UpdateEventRequest;
import ru.yandex.practicum.service.shared.dto.EventDtoMapper;
import ru.yandex.practicum.service.shared.dto.EventFullDto;
import ru.yandex.practicum.service.shared.dto.EventShortDto;
import ru.yandex.practicum.service.shared.exceptions.BadRequestException;
import ru.yandex.practicum.service.shared.exceptions.ForbiddenException;
import ru.yandex.practicum.service.shared.exceptions.NotFoundException;
import ru.yandex.practicum.service.shared.model.Category;
import ru.yandex.practicum.service.shared.model.DateTimeFormat;
import ru.yandex.practicum.service.shared.model.Event;
import ru.yandex.practicum.service.shared.model.EventState;
import ru.yandex.practicum.service.shared.model.User;
import ru.yandex.practicum.service.shared.storage.CategoryRepository;
import ru.yandex.practicum.service.shared.storage.EventRepository;
import ru.yandex.practicum.service.shared.storage.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Getter
@Component
public class PrivateEventService {
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public List<EventShortDto> getUsersEvents(long userId, int from, int size) {
        Optional<User> eventOwnerOptional = userRepository.findById(userId);
        if (eventOwnerOptional.isEmpty()) {
            throw new NotFoundException("user id=" + userId + " not found");
        }
        User eventOwner = eventOwnerOptional.get();

        List<Event> userEvents = eventRepository.findByInitiatorId(userId,
                        PageRequest.of(from, size, Sort.by(Sort.Direction.ASC, "id")));

        List<EventShortDto> userEventsDtos = new ArrayList<>();

        List<Long> categoryIds = new ArrayList<>();
        for (Event event : userEvents) {
            categoryIds.add(event.getCategoryId());
        }
        List<Category> eventsCategories = categoryRepository.findByIdIn(categoryIds);

        for (Event event : userEvents) {
            Category category = eventsCategories.stream()
                    .filter(cat -> cat.getId() == event.getCategoryId()).findAny().get();
            userEventsDtos.add(EventDtoMapper.toShortDto(event, category, eventOwner));
        }

        return userEventsDtos;
    }

    public EventFullDto changeEvent(long userId, UpdateEventRequest eventUpdate) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            throw new NotFoundException("user id=" + userId + " not found");
        }

        Optional<Event> eventOptional = eventRepository.findById(eventUpdate.getEventId());
        if (eventOptional.isEmpty()) {
            throw new NotFoundException("event id=" + eventUpdate.getEventId() + " not found");
        }
        Event event = eventOptional.get();

        if (event.getState().equals(EventState.PUBLISHED)) {
            throw new ForbiddenException("event id=" + event.getId() + " already published");
        }

        if (eventUpdate.getAnnotation() != null) {
            event.setAnnotation(eventUpdate.getAnnotation());
        }
        if (eventUpdate.getCategory() != null) {
            event.setCategoryId(eventUpdate.getCategory());
        }
        if (eventUpdate.getDescription() != null) {
            event.setDescription(eventUpdate.getDescription());
        }
        if (eventUpdate.getEventDate() != null) {
            LocalDateTime timeThreshold = LocalDateTime.now().plusHours(2);
            DateTimeFormatter formatter = new DateTimeFormat().getFormatter();

            if (timeThreshold.isBefore(LocalDateTime.parse(eventUpdate.getEventDate(), formatter))) {
                event.setEventDate(LocalDateTime.parse(eventUpdate.getEventDate(), formatter));
            } else {
                throw new BadRequestException("event can not start in less than 2 hour from now");
            }
        }
        if (eventUpdate.getPaid() != null) {
            event.setPaid(eventUpdate.getPaid());
        }
        if (eventUpdate.getParticipantLimit() != null) {
            event.setParticipantLimit(eventUpdate.getParticipantLimit());
        }
        if (eventUpdate.getTitle() != null) {
            event.setTitle(eventUpdate.getTitle());
        }

        return EventDtoMapper.toFullDto(eventRepository.save(event),
                categoryRepository.findById(event.getCategoryId()).get(),
                userRepository.findById(event.getInitiatorId()).get());
    }

    public EventFullDto addEvent(long userId, NewEventDto eventNew) {
        LocalDateTime timeThreshold = LocalDateTime.now().plusHours(2);
        DateTimeFormatter formatter = new DateTimeFormat().getFormatter();
        if (timeThreshold.isAfter(LocalDateTime.parse(eventNew.getEventDate(), formatter))) {
            throw new BadRequestException("event can not start in less than 2 hour from now");
        }

        Event event = Event.builder()
                .annotation(eventNew.getAnnotation())
                .categoryId(eventNew.getCategory())
                .description(eventNew.getDescription())
                .eventDate(LocalDateTime.parse(eventNew.getEventDate(), formatter))
                .latitude(eventNew.getLocation().getLat())
                .longtitude(eventNew.getLocation().getLon())
                .title(eventNew.getTitle())
                .initiatorId(userId)
                .createdOn(LocalDateTime.now())
                .publishedOn(null)
                .state(EventState.PENDING)
                .confirmedRequests(0)
                .build();

        if (eventNew.getPaid() != null) {
            event.setPaid(eventNew.getPaid());
        } else {
            event.setPaid(false);
        }
        if (eventNew.getParticipantLimit() != null) {
            event.setParticipantLimit(eventNew.getParticipantLimit());
        } else {
            event.setParticipantLimit(0);
        }
        if (eventNew.getRequestModeration() != null) {
            event.setModerationRequired(eventNew.getRequestModeration());
        } else {
            event.setModerationRequired(true);
        }

        return EventDtoMapper.toFullDto(eventRepository.save(event),
                categoryRepository.findById(event.getCategoryId()).get(),
                userRepository.findById(event.getInitiatorId()).get());
    }

    public EventFullDto getEvent(long userId, long eventId) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            throw new NotFoundException("user id=" + userId + " not found");
        }

        Optional<Event> eventOptional = eventRepository.findById(eventId);
        if (eventOptional.isEmpty()) {
            throw new NotFoundException("event id=" + eventId + " not found");
        }
        Event event = eventOptional.get();

        if (event.getInitiatorId() != userId) {
            throw new ForbiddenException("event initiator id=" + event.getInitiatorId() + " and user id=" + userId);
        }

        return EventDtoMapper.toFullDto(event,
                categoryRepository.findById(event.getCategoryId()).get(),
                user.get());
    }

    public EventFullDto cancelEvent(long userId, long eventId) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            throw new NotFoundException("user id=" + userId + " not found");
        }

        Optional<Event> eventOptional = eventRepository.findById(eventId);
        if (eventOptional.isEmpty()) {
            throw new NotFoundException("event id=" + eventId + " not found");
        }
        Event event = eventOptional.get();

        if (event.getInitiatorId() != userId) {
            throw new ForbiddenException("event initiator id=" + event.getInitiatorId() + " and user id=" + userId);
        }

        if (event.getState().equals(EventState.PUBLISHED)) {
            throw new ForbiddenException("event id=" + event.getId() + " already published");
        }

        event.setState(EventState.CANCELED);

        return EventDtoMapper.toFullDto(eventRepository.save(event),
                categoryRepository.findById(event.getCategoryId()).get(),
                user.get());
    }
}
