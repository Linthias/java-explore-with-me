package ru.yandex.practicum.service.public_service.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
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
import java.util.Optional;

@RequiredArgsConstructor
@Getter
@Component
public class PublicEventService {
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;

    public EventFullDto getEventById(long eventId) {
        Optional<Event> eventOptional = eventRepository.findById(eventId);
        if (eventOptional.isEmpty()) {
            throw new NotFoundException("event id=" + eventId + " not found");
        }
        Event event = eventOptional.get();

        if (event.getState().equals(EventState.PENDING) || event.getState().equals(EventState.CANCELED)) {
            throw new ForbiddenException("event id=" + eventId + " not published");
        }

        Optional<Category> eventCategory = categoryRepository.findById(event.getCategoryId());
        if (eventCategory.isEmpty()) {
            throw new NotFoundException("category id=" + event.getCategoryId() + " not found");
        }

        Optional<User> eventInitiator = userRepository.findById(event.getInitiatorId());
        if (eventInitiator.isEmpty()) {
            throw new NotFoundException("user id=" + event.getInitiatorId() + " not found");
        }

        return EventDtoMapper.toFullDto(event, eventCategory.get(), eventInitiator.get());
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

        if (text != null && !"".equals(text)) {
            Predicate textInAnnotation = builder.like(root.get("annotation"), text);
            Predicate textInDescription = builder.like(root.get("description"), text);

            predicates.add(builder.or(textInAnnotation, textInDescription));
        }
        if (categories != null) {
            CriteriaBuilder.In<Long> categoriesIn = builder.in(root.get("categoryId"));
            for (long categoryId : categories) {
                categoriesIn.value(categoryId);
            }
            predicates.add(categoriesIn);
        }
        if (paid != null) {
            predicates.add(builder.equal(root.get("isPaid"), paid));
        }
        if (rangeStart != null) {
            LocalDateTime startLimit = LocalDateTime.parse(rangeStart, new DateTimeFormat().getFormatter());
            Path<LocalDateTime> dateTimePath = root.get("eventDate");

            predicates.add(builder.greaterThanOrEqualTo(dateTimePath, startLimit));
        }
        if (rangeEnd != null) {
            LocalDateTime endLimit = LocalDateTime.parse(rangeEnd, new DateTimeFormat().getFormatter());
            Path<LocalDateTime> dateTimePath = root.get("eventDate");

            predicates.add(builder.lessThanOrEqualTo(dateTimePath, endLimit));
        }
        if (onlyAvailable) {
            predicates.add(builder.lessThan(root.get("confirmedRequests"), root.get("participantLimit")));
        }
        if (sort == null) {
            query.select(root).where(predicates.toArray(new Predicate[]{}));
        } else if ("EVENT_DATE".equals(sort)) {
            query.select(root).where(predicates.toArray(new Predicate[]{})).orderBy(builder.asc(root.get("eventDate")));
        } else if ("VIEWS".equals(sort)) {
            query.select(root).where(predicates.toArray(new Predicate[]{})).orderBy(builder.desc(root.get("views")));
        } else {
            throw new BadRequestException("wrong sort parameter: " + sort);
        }

        List<Event> events = entityManager.createQuery(query).setFirstResult(from).setMaxResults(size).getResultList();

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
            Category category = loadedCategories.stream().filter(cat -> cat.getId() == event.getCategoryId()).findAny().get();
            User user = loadedUsers.stream().filter(user1 -> user1.getId() == event.getInitiatorId()).findAny().get();
            eventsDtos.add(EventDtoMapper.toShortDto(event, category, user));
        }

        return eventsDtos;
    }
}
