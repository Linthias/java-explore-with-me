package ru.yandex.practicum.service.public_service.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
import ru.yandex.practicum.service.shared.storage.EventStateRepository;
import ru.yandex.practicum.service.shared.storage.UserRepository;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
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
    private final EventStateRepository eventStateRepository;
    private final EntityManager entityManager;

    public EventFullDto getEventById(long eventId) {
        Optional<Event> event = eventRepository.findById(eventId);
        if (event.isEmpty())
            throw new NotFoundException("event id=" + eventId + " not found");

        Optional<EventState> eventState = eventStateRepository.findById(event.get().getEventStateId());
        if (eventState.isEmpty())
            throw new NotFoundException("event state id=" + event.get().getEventStateId() + " not found");
        if (eventState.get().getName().equals("PENDING") || eventState.get().getName().equals("CANCELED"))
            throw new ForbiddenException("event id=" + eventId + " not published");

        Optional<Category> category = categoryRepository.findById(event.get().getCategoryId());
        if (category.isEmpty())
            throw new NotFoundException("category id=" + event.get().getCategoryId() + " not found");

        Optional<User> user = userRepository.findById(event.get().getInitiatorId());
        if (user.isEmpty())
            throw new NotFoundException("user id=" + event.get().getInitiatorId() + " not found");

        return EventDtoMapper.toFullDto(event.get(), category.get(), user.get(), eventState.get());
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
        //CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        //CriteriaQuery<Event> query = criteriaBuilder.createQuery(Event.class);
        //Root<Event> customer = query.from(Event.class);

        Page<Event> eventPage;
        if (sort == null)
            eventPage = eventRepository.findAll(PageRequest.of(from, size));
        else if ("EVENT_DATE".equals(sort))
            eventPage = eventRepository.findAll(PageRequest.of(from, size, Sort.by("eventDate").ascending()));
        else if ("VIEWS".equals(sort))
            eventPage = eventRepository.findAll(PageRequest.of(from, size, Sort.by("views").descending()));
        else
            throw new BadRequestException("wrong sort parameter: " + sort);

        List<Event> temp1 = eventPage.toList();
        List<Event> temp2 = new ArrayList<>();

        if (text != null && !"".equals(text)) {
            for (Event event : temp1) {
                if (event.getDescription().toLowerCase().contains(text.toLowerCase()) ||
                        event.getAnnotation().toLowerCase().contains(text.toLowerCase()))
                    temp2.add(event);
            }
            temp1 = temp2;
            temp2 = new ArrayList<>();
        }

        if (categories != null) {
            for (Event event : temp1) {
                for (int i = 0; i < categories.length; ++i) {
                    if (categories[i] == event.getCategoryId())
                        temp2.add(event);
                    break;
                }
            }
            temp1 = temp2;
            temp2 = new ArrayList<>();
        }

        if (paid != null) {
            if (paid)
                for (Event event : temp1) {
                    if (event.isPaid())
                        temp2.add(event);
                }
            else
                for (Event event : temp1) {
                    if (!event.isPaid())
                        temp2.add(event);
                }
            temp1 = temp2;
            temp2 = new ArrayList<>();
        }


        if (rangeStart != null) {
            LocalDateTime timestamp = LocalDateTime.parse(rangeStart, new DateTimeFormat().getFormatter());
            for (Event event : temp1) {
                if (event.getEventDate().isAfter(timestamp))
                    temp2.add(event);
            }
            temp1 = temp2;
            temp2 = new ArrayList<>();
        }

        if (rangeEnd != null) {
            LocalDateTime timestamp = LocalDateTime.parse(rangeEnd, new DateTimeFormat().getFormatter());
            for (Event event : temp1) {
                if (event.getEventDate().isBefore(timestamp))
                    temp2.add(event);
            }
            temp1 = temp2;
            temp2 = new ArrayList<>();
        }


        if (onlyAvailable) {
            for (Event event : temp1) {
                if (event.getConfirmedRequests() < event.getParticipantLimit())
                    temp2.add(event);
            }
            temp1 = temp2;
        }

        List<EventShortDto> result = new ArrayList<>();
        for (Event event : temp1) {
            Optional<Category> category = categoryRepository.findById(event.getCategoryId());
            Optional<User> user = userRepository.findById(event.getInitiatorId());
            result.add(EventDtoMapper.toShortDto(event, category.get(), user.get()));
        }

        return result;
    }
}
