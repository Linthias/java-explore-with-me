package ru.yandex.practicum.service.private_service.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
import ru.yandex.practicum.service.shared.model.User;
import ru.yandex.practicum.service.shared.storage.CategoryRepository;
import ru.yandex.practicum.service.shared.storage.EventRepository;
import ru.yandex.practicum.service.shared.storage.EventStateRepository;
import ru.yandex.practicum.service.shared.storage.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Getter
@Component
public class PrivateEventService {
    private final EventRepository eventRepository;
    private final EventStateRepository eventStateRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public List<EventShortDto> getUsersEvents(long userId, int from, int size) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty())
            throw new NotFoundException("user id=" + userId + " not found");

        Page<Event> eventPage
                = eventRepository.findAll(PageRequest.of(from, size, Sort.by(Sort.Direction.ASC, "id")));

        List<Event> temp
                = eventPage.get().filter(event -> event.getInitiatorId() == userId).collect(Collectors.toList());

        List<EventShortDto> result = new ArrayList<>();
        for (Event event : temp) {
            Optional<Category> category = categoryRepository.findById(event.getCategoryId());
            user = userRepository.findById(event.getInitiatorId());
            result.add(EventDtoMapper.toShortDto(event, category.get(), user.get()));
        }
        return result;
    }

    public EventFullDto changeEvent(long userId, UpdateEventRequest eventUpdate) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty())
            throw new NotFoundException("user id=" + userId + " not found");

        Optional<Event> event = eventRepository.findById(eventUpdate.getEventId());
        if (event.isEmpty())
            throw new NotFoundException("event id=" + eventUpdate.getEventId() + " not found");

        if (event.get().getEventStateId() == 2)
            throw new ForbiddenException("event id=" + event.get().getId() + " already published");

        if (eventUpdate.getAnnotation() != null)
            event.get().setAnnotation(eventUpdate.getAnnotation());
        if (eventUpdate.getCategory() != null)
            event.get().setCategoryId(eventUpdate.getCategory());
        if (eventUpdate.getDescription() != null)
            event.get().setDescription(eventUpdate.getDescription());
        if (eventUpdate.getEventDate() != null) {
            if (LocalDateTime.now().plusHours(2)
                    .isBefore(LocalDateTime.parse(eventUpdate.getEventDate(),
                    new DateTimeFormat().getFormatter())))
            event.get().setEventDate(LocalDateTime.parse(eventUpdate.getEventDate(),
                    new DateTimeFormat().getFormatter()));
            else
                throw new BadRequestException("event can not start in less than 2 hour from now");
        }
        if (eventUpdate.getPaid() != null)
            event.get().setPaid(eventUpdate.getPaid());
        if (eventUpdate.getParticipantLimit() != null)
            event.get().setParticipantLimit(eventUpdate.getParticipantLimit());
        if (eventUpdate.getTitle() != null)
            event.get().setTitle(eventUpdate.getTitle());

        return EventDtoMapper.toFullDto(eventRepository.save(event.get()),
                categoryRepository.findById(event.get().getCategoryId()).get(),
                userRepository.findById(event.get().getInitiatorId()).get(),
                eventStateRepository.findById(1).get());
    }

    public EventFullDto addEvent(long userId, NewEventDto eventNew) {
        if (LocalDateTime.now().plusHours(2)
                .isAfter(LocalDateTime.parse(eventNew.getEventDate(),
                        new DateTimeFormat().getFormatter())))
            throw new BadRequestException("event can not start in less than 2 hour from now");

        Event event = Event.builder()
                .annotation(eventNew.getAnnotation())
                .categoryId(eventNew.getCategory())
                .description(eventNew.getDescription())
                .eventDate(LocalDateTime.parse(eventNew.getEventDate(),
                        new DateTimeFormat().getFormatter()))
                .latitude(eventNew.getLocation().getLat())
                .longtitude(eventNew.getLocation().getLon())
                .title(eventNew.getTitle())
                .initiatorId(userId)
                .createdOn(LocalDateTime.now())
                .publishedOn(null)
                .eventStateId(1)
                .confirmedRequests(0)
                .build();

        if (eventNew.getPaid() != null)
            event.setPaid(eventNew.getPaid());
        else
            event.setPaid(false);
        if (eventNew.getParticipantLimit() != null)
            event.setParticipantLimit(eventNew.getParticipantLimit());
        else
            event.setParticipantLimit(0);
        if (eventNew.getRequestModeration() != null)
            event.setModerationRequired(eventNew.getRequestModeration());
        else
            event.setModerationRequired(true);

        return EventDtoMapper.toFullDto(eventRepository.save(event),
                categoryRepository.findById(event.getCategoryId()).get(),
                userRepository.findById(event.getInitiatorId()).get(),
                eventStateRepository.findById(1).get());
    }

    public EventFullDto getEvent(long userId, long eventId) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty())
            throw new NotFoundException("user id=" + userId + " not found");

        Optional<Event> event = eventRepository.findById(eventId);
        if (event.isEmpty())
            throw new NotFoundException("event id=" + eventId + " not found");

        if (event.get().getInitiatorId() != userId)
            throw new ForbiddenException("event initiator id=" + event.get().getInitiatorId() + " and user id=" + userId);

        return EventDtoMapper.toFullDto(event.get(),
                categoryRepository.findById(event.get().getCategoryId()).get(),
                user.get(),
                eventStateRepository.findById(event.get().getEventStateId()).get());
    }

    public EventFullDto cancelEvent(long userId, long eventId) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty())
            throw new NotFoundException("user id=" + userId + " not found");

        Optional<Event> event = eventRepository.findById(eventId);
        if (event.isEmpty())
            throw new NotFoundException("event id=" + eventId + " not found");

        if (event.get().getInitiatorId() != userId)
            throw new ForbiddenException("event initiator id=" + event.get().getInitiatorId() + " and user id=" + userId);

        if (event.get().getEventStateId() == 2)
            throw new ForbiddenException("event id=" + event.get().getId() + " already published");

        event.get().setEventStateId(3); // теперь статус CANCELED

        return EventDtoMapper.toFullDto(eventRepository.save(event.get()),
                categoryRepository.findById(event.get().getCategoryId()).get(),
                user.get(),
                eventStateRepository.findById(event.get().getEventStateId()).get());
    }
}
