package ru.yandex.practicum.service.admin_service.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.service.admin_service.dto.AdminUpdateEventRequest;
import ru.yandex.practicum.service.shared.dto.EventDtoMapper;
import ru.yandex.practicum.service.shared.dto.EventFullDto;
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

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Getter
@Service
public class AdminEventService {
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;

    private static final String PENDING_STATE = "PENDING";
    private static final String PUBLISHED_STATE = "PUBLISHED";
    private static final String CANCELED_STATE = "CANCELED";
    private static final String EVENT_INITIATOR = "initiatorId";
    private static final String EVENT_STATE = "state";
    private static final String EVENT_CATEGORY = "categoryId";
    private static final String EVENT_DATE = "eventDate";

    public List<EventFullDto> getEvents(long[] users,
                                        String[] states,
                                        long[] categories,
                                        String rangeStart,
                                        String rangeEnd,
                                        int from,
                                        int size) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Event> query = builder.createQuery(Event.class);
        Root<Event> root = query.from(Event.class);
        List<Predicate> predicates = new ArrayList<>();

        if (users != null) {
            CriteriaBuilder.In<Long> usersIn = builder.in(root.get(EVENT_INITIATOR));
            for (long userId : users) {
                usersIn.value(userId);
            }
            predicates.add(usersIn);
        }
        if (states != null) {
            Set<EventState> eventStates = new HashSet<>();
            for (String state : states) {
                if (PENDING_STATE.equalsIgnoreCase(state))
                    eventStates.add(EventState.PENDING);
                if (PUBLISHED_STATE.equalsIgnoreCase(state))
                    eventStates.add(EventState.PUBLISHED);
                if (CANCELED_STATE.equalsIgnoreCase(state))
                    eventStates.add(EventState.CANCELED);
            }

            CriteriaBuilder.In<EventState> statesIn = builder.in(root.get(EVENT_STATE));
            for (EventState state : eventStates) {
                statesIn.value(state);
            }
            predicates.add(statesIn);
        }
        if (categories != null) {
            CriteriaBuilder.In<Long> categoriesIn = builder.in(root.get(EVENT_CATEGORY));
            for (long categoryId : categories) {
                categoriesIn.value(categoryId);
            }
            predicates.add(categoriesIn);
        }
        if (rangeStart != null) {
            LocalDateTime startLimit = LocalDateTime.parse(rangeStart, new DateTimeFormat().getFormatter());
            Path<LocalDateTime> dateTimePath = root.get(EVENT_DATE);

            predicates.add(builder.greaterThanOrEqualTo(dateTimePath, startLimit));
        }
        if (rangeEnd != null) {
            LocalDateTime endLimit = LocalDateTime.parse(rangeEnd, new DateTimeFormat().getFormatter());
            Path<LocalDateTime> dateTimePath = root.get(EVENT_DATE);

            predicates.add(builder.lessThanOrEqualTo(dateTimePath, endLimit));
        }

        query.select(root).where(predicates.toArray(new Predicate[]{}));
        List<Event> events = entityManager.createQuery(query)
                .setFirstResult(from)
                .setMaxResults(size)
                .getResultList();

        List<EventFullDto> eventDtos = new ArrayList<>();

        List<Long> categoryIds = new ArrayList<>();
        List<Long> initiatorIds = new ArrayList<>();
        for (Event event : events) {
            categoryIds.add(event.getCategoryId());
            initiatorIds.add(event.getInitiatorId());
        }
        List<Category> eventCategories = categoryRepository.findByIdIn(categoryIds);
        List<User> eventOwners = userRepository.findByIdIn(initiatorIds);
        for (Event event : events) {
            Category category = eventCategories.stream().filter(cat -> cat.getId() == event.getCategoryId()).findAny()
                    .orElseThrow(() -> new NotFoundException("category id=" + event.getCategoryId() + " not found"));

            User user = eventOwners.stream().filter(user1 -> user1.getId() == event.getInitiatorId()).findAny()
                    .orElseThrow(() -> new NotFoundException("user id=" + event.getInitiatorId() + " not found"));

            eventDtos.add(EventDtoMapper.toFullDto(event, category, user));
        }

        return eventDtos;
    }

    public EventFullDto changeEvent(long eventId, AdminUpdateEventRequest updateEvent) {
        Event eventToChange = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("event id=" + eventId + " not found"));

        if (updateEvent.getAnnotation() != null) {
            eventToChange.setAnnotation(updateEvent.getAnnotation());
        }
        if (updateEvent.getCategory() != null) {
            eventToChange.setCategoryId(updateEvent.getCategory());
        }
        if (updateEvent.getDescription() != null) {
            eventToChange.setDescription(updateEvent.getDescription());
        }
        if (updateEvent.getEventDate() != null) {
            DateTimeFormatter formatter = new DateTimeFormat().getFormatter();
            eventToChange.setEventDate(LocalDateTime.parse(updateEvent.getEventDate(), formatter));
        }
        if (updateEvent.getLocation() != null) {
            eventToChange.setLatitude(updateEvent.getLocation().getLat());
            eventToChange.setLongtitude(updateEvent.getLocation().getLon());
        }
        if (updateEvent.getPaid() != null) {
            eventToChange.setPaid(updateEvent.getPaid());
        }
        if (updateEvent.getParticipantLimit() != null) {
            eventToChange.setParticipantLimit(updateEvent.getParticipantLimit());
        }
        if (updateEvent.getRequestModeration() != null) {
            eventToChange.setModerationRequired(updateEvent.getRequestModeration());
        }
        if (updateEvent.getTitle() != null) {
            eventToChange.setTitle(updateEvent.getTitle());
        }

        return EventDtoMapper.toFullDto(eventRepository.save(eventToChange),
                categoryRepository.findById(eventToChange.getCategoryId())
                        .orElseThrow(()
                                -> new NotFoundException("category id=" + eventToChange.getCategoryId() + " not found")),
                userRepository.findById(eventToChange.getInitiatorId())
                        .orElseThrow(()
                                -> new NotFoundException("user id=" + eventToChange.getInitiatorId() + " not found")));
    }

    public EventFullDto publishEvent(long eventId) {
        Event eventToPublish = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("event id=" + eventId + " not found"));

        if (!eventToPublish.getState().equals(EventState.PENDING)) {
            throw new ForbiddenException("event id=" + eventId + " already published or canceled");
        }

        LocalDateTime publicationTime = LocalDateTime.now();
        if (eventToPublish.getEventDate().isBefore(publicationTime.plusHours(1))) {
            throw new BadRequestException("event id=" + eventId + " starts in less than 1 hour from now");
        }

        eventToPublish.setPublishedOn(publicationTime);
        eventToPublish.setState(EventState.PUBLISHED);

        return EventDtoMapper.toFullDto(eventRepository.save(eventToPublish),
                categoryRepository.findById(eventToPublish.getCategoryId())
                        .orElseThrow(()
                                -> new NotFoundException("category id=" + eventToPublish.getCategoryId() + " not found")),
                userRepository.findById(eventToPublish.getInitiatorId())
                        .orElseThrow(()
                                -> new NotFoundException("user id=" + eventToPublish.getInitiatorId() + " not found")));
    }

    public EventFullDto rejectEvent(long eventId) {
        Event eventToReject = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("event id=" + eventId + " not found"));

        if (eventToReject.getState().equals(EventState.PUBLISHED)) {
            throw new ForbiddenException("event id=" + eventId + " already published");
        }

        eventToReject.setState(EventState.CANCELED);

        return EventDtoMapper.toFullDto(eventRepository.save(eventToReject),
                categoryRepository.findById(eventToReject.getCategoryId())
                        .orElseThrow(()
                                -> new NotFoundException("category id=" + eventToReject.getCategoryId() + " not found")),
                userRepository.findById(eventToReject.getInitiatorId())
                        .orElseThrow(()
                                -> new NotFoundException("user id=" + eventToReject.getInitiatorId() + " not found")));
    }
}
