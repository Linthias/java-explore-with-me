package ru.yandex.practicum.service.public_service.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Getter
@Service
public class PublicEventService {
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;

    private static final String DATE_SORT = "EVENT_DATE";
    private static final String VIEWS_SORT = "VIEWS";
    private static final String BLANK_STRING = "";
    private static final String EVENT_ANNOTATION = "annotation";
    private static final String EVENT_DESCRIPTION = "description";
    private static final String EVENT_CATEGORY = "categoryId";
    private static final String EVENT_PAID = "paid";
    private static final String EVENT_DATE = "eventDate";
    private static final String EVENT_CONFIRMED_REQUESTS = "confirmedRequests";
    private static final String EVENT_PARTICIPANT_LIMIT = "participantLimit";
    private static final String EVENT_VIEWS = "views";

    public EventFullDto getEventById(long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("event id=" + eventId + " not found"));

        if (event.getState().equals(EventState.PENDING) || event.getState().equals(EventState.CANCELED)) {
            throw new ForbiddenException("event id=" + eventId + " not published");
        }

        Category eventCategory = categoryRepository.findById(event.getCategoryId())
                .orElseThrow(() -> new NotFoundException("category id=" + event.getCategoryId() + " not found"));

        User eventInitiator = userRepository.findById(event.getInitiatorId())
                .orElseThrow(() -> new NotFoundException("user id=" + event.getInitiatorId() + " not found"));

        return EventDtoMapper.toFullDto(event, eventCategory, eventInitiator);
    }

    public List<EventShortDto> getEvents(String text,
                                    int[] categories,
                                    Boolean paid,
                                    String rangeStart,
                                    String rangeEnd,
                                    boolean onlyAvailable,
                                    String sort,
                                    int from,
                                    int size) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Event> query = builder.createQuery(Event.class);
        Root<Event> root = query.from(Event.class);
        List<Predicate> predicates = new ArrayList<>();

        if (text != null && !BLANK_STRING.equals(text)) {
            Predicate textInAnnotation = builder.like(root.get(EVENT_ANNOTATION), text);
            Predicate textInDescription = builder.like(root.get(EVENT_DESCRIPTION), text);

            predicates.add(builder.or(textInAnnotation, textInDescription));
        }
        if (categories != null) {
            CriteriaBuilder.In<Long> categoriesIn = builder.in(root.get(EVENT_CATEGORY));
            for (long categoryId : categories) {
                categoriesIn.value(categoryId);
            }
            predicates.add(categoriesIn);
        }
        if (paid != null) {
            predicates.add(builder.equal(root.get(EVENT_PAID), paid));
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
        if (onlyAvailable) {
            predicates.add(builder.lessThan(root.get(EVENT_CONFIRMED_REQUESTS), root.get(EVENT_PARTICIPANT_LIMIT)));
        }
        if (sort == null) {
            query.select(root).where(predicates.toArray(new Predicate[]{}));
        } else if (DATE_SORT.equals(sort)) {
            query.select(root).where(predicates.toArray(new Predicate[]{})).orderBy(builder.asc(root.get(EVENT_DATE)));
        } else if (VIEWS_SORT.equals(sort)) {
            query.select(root).where(predicates.toArray(new Predicate[]{})).orderBy(builder.desc(root.get(EVENT_VIEWS)));
        } else {
            throw new BadRequestException("wrong sort parameter: " + sort);
        }

        List<Event> events = entityManager.createQuery(query)
                .setFirstResult(from)
                .setMaxResults(size)
                .getResultList();

        List<EventShortDto> eventsDtos = new ArrayList<>();

        List<Long> categoryIds = new ArrayList<>();
        List<Long> initiatorIds = new ArrayList<>();
        for (Event event : events) {
            categoryIds.add(event.getCategoryId());
            initiatorIds.add(event.getInitiatorId());
        }

        List<Category> loadedCategories = categoryRepository.findByIdIn(categoryIds);
        List<User> loadedUsers = userRepository.findByIdIn(initiatorIds);

        for (Event event : events) {
            Category category = loadedCategories.stream().filter(cat -> cat.getId() == event.getCategoryId()).findAny()
                    .orElseThrow(() -> new NotFoundException("category id=" + event.getCategoryId() + " not found"));

            User user = loadedUsers.stream().filter(user1 -> user1.getId() == event.getInitiatorId()).findAny()
                    .orElseThrow(() -> new NotFoundException("user id=" + event.getInitiatorId() + " not found"));

            eventsDtos.add(EventDtoMapper.toShortDto(event, category, user));
        }

        return eventsDtos;
    }
}
