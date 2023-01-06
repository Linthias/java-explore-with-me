package ru.yandex.practicum.service.admin_service.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.service.admin_service.dto.AdminUpdateEventRequest;
import ru.yandex.practicum.service.shared.dto.EventDtoMapper;
import ru.yandex.practicum.service.shared.dto.EventFullDto;
import ru.yandex.practicum.service.shared.exceptions.BadRequestException;
import ru.yandex.practicum.service.shared.exceptions.ForbiddenException;
import ru.yandex.practicum.service.shared.exceptions.NotFoundException;
import ru.yandex.practicum.service.shared.model.DateTimeFormat;
import ru.yandex.practicum.service.shared.model.Event;
import ru.yandex.practicum.service.shared.storage.CategoryRepository;
import ru.yandex.practicum.service.shared.storage.EventRepository;
import ru.yandex.practicum.service.shared.storage.EventStateRepository;
import ru.yandex.practicum.service.shared.storage.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Getter
@Component
public class AdminEventService {
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final EventStateRepository eventStateRepository;

    public List<EventFullDto> getEvents(long[] users,
                                        String[] states,
                                        long[] categories,
                                        String rangeStart,
                                        String rangeEnd,
                                        int from,
                                        int size) {
        Page<Event> eventPage = eventRepository.findAll(PageRequest.of(from, size));
        List<Event> temp = eventPage.toList();

        if (users != null) {
            Set<Long> userIds = new HashSet<>();
            for (int i = 0; i < users.length; ++i) {
                userIds.add(users[i]);
            }
            temp = temp.stream().filter(event
                    -> userIds.contains(event.getInitiatorId())).collect(Collectors.toList());
        }
        if (states != null) {
            Set<Integer> eventStates = new HashSet<>();
            for (int i = 0; i < states.length; ++i) {
                if ("PENDING".equals(states[i].toUpperCase()))
                    eventStates.add(1);
                if ("PUBLISHED".equals(states[i].toUpperCase()))
                    eventStates.add(2);
                if ("CANCELED".equals(states[i].toUpperCase()))
                    eventStates.add(3);
            }
            temp = temp.stream().filter(event
                    -> eventStates.contains(event.getEventStateId())).collect(Collectors.toList());
        }
        if (categories != null) {
            Set<Long> categoryIds = new HashSet<>();
            for (int i = 0; i < categories.length; ++i) {
                categoryIds.add(categories[i]);
            }
            temp = temp.stream().filter(event
                    -> categoryIds.contains(event.getCategoryId())).collect(Collectors.toList());
        }
        if (rangeStart != null) {
            LocalDateTime timestamp = LocalDateTime.parse(rangeStart, new DateTimeFormat().getFormatter());
            temp = temp.stream().filter(event -> event.getEventDate().isAfter(timestamp)).collect(Collectors.toList());
        }
        if (rangeEnd != null) {
            LocalDateTime timestamp = LocalDateTime.parse(rangeEnd, new DateTimeFormat().getFormatter());
            temp = temp.stream().filter(event -> event.getEventDate().isBefore(timestamp)).collect(Collectors.toList());
        }

        List<EventFullDto> result = new ArrayList<>();
        for (Event event : temp) {
            result.add(EventDtoMapper.toFullDto(event,
                    categoryRepository.findById(event.getCategoryId()).get(),
                    userRepository.findById(event.getInitiatorId()).get(),
                    eventStateRepository.findById(event.getEventStateId()).get()));
        }

        return result;
    }

    public EventFullDto changeEvent(long eventId, AdminUpdateEventRequest updateEvent) {
        Optional<Event> event = eventRepository.findById(eventId);
        if (event.isEmpty())
            throw new NotFoundException("event id=" + eventId + " not found");

        if (updateEvent.getAnnotation() != null)
            event.get().setAnnotation(updateEvent.getAnnotation());
        if (updateEvent.getCategory() != null)
            event.get().setCategoryId(updateEvent.getCategory());
        if (updateEvent.getDescription() != null)
            event.get().setDescription(updateEvent.getDescription());
        if (updateEvent.getEventDate() != null)
            event.get().setEventDate(LocalDateTime.parse(updateEvent.getEventDate(),
                    new DateTimeFormat().getFormatter()));
        if (updateEvent.getLocation() != null) {
            event.get().setLatitude(updateEvent.getLocation().getLat());
            event.get().setLongtitude(updateEvent.getLocation().getLon());
        }
        if (updateEvent.getPaid() != null)
            event.get().setPaid(updateEvent.getPaid());
        if (updateEvent.getParticipantLimit() != null)
            event.get().setParticipantLimit(updateEvent.getParticipantLimit());
        if (updateEvent.getRequestModeration() != null)
            event.get().setModerationRequired(updateEvent.getRequestModeration());
        if (updateEvent.getTitle() != null)
            event.get().setTitle(updateEvent.getTitle());

        return EventDtoMapper.toFullDto(eventRepository.save(event.get()),
                categoryRepository.findById(event.get().getCategoryId()).get(),
                userRepository.findById(event.get().getInitiatorId()).get(),
                eventStateRepository.findById(event.get().getEventStateId()).get());
    }

    public EventFullDto publishEvent(long eventId) {
        Optional<Event> event = eventRepository.findById(eventId);
        if (event.isEmpty())
            throw new NotFoundException("event id=" + eventId + " not found");

        if (event.get().getEventStateId() != 1)
            throw new ForbiddenException("event id=" + eventId + " already published or canceled");

        LocalDateTime publicationTime = LocalDateTime.now();
        if (event.get().getEventDate().isBefore(publicationTime.plusHours(1)))
            throw new BadRequestException("event id=" + eventId + " starts in less than 1 hour from now");

        event.get().setPublishedOn(publicationTime);
        event.get().setEventStateId(2);
        return EventDtoMapper.toFullDto(eventRepository.save(event.get()),
                categoryRepository.findById(event.get().getCategoryId()).get(),
                userRepository.findById(event.get().getInitiatorId()).get(),
                eventStateRepository.findById(event.get().getEventStateId()).get());
    }

    public EventFullDto rejectEvent(long eventId) {
        Optional<Event> event = eventRepository.findById(eventId);
        if (event.isEmpty())
            throw new NotFoundException("event id=" + eventId + " not found");

        if (event.get().getEventStateId() == 2)
            throw new ForbiddenException("event id=" + eventId + " already published");

        event.get().setEventStateId(3);

        return EventDtoMapper.toFullDto(eventRepository.save(event.get()),
                categoryRepository.findById(event.get().getCategoryId()).get(),
                userRepository.findById(event.get().getInitiatorId()).get(),
                eventStateRepository.findById(event.get().getEventStateId()).get());
    }
}
