package ru.yandex.practicum.service.public_service.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.service.shared.dto.CompilationDto;
import ru.yandex.practicum.service.shared.dto.CompilationDtoMapper;
import ru.yandex.practicum.service.shared.dto.EventDtoMapper;
import ru.yandex.practicum.service.shared.dto.EventShortDto;
import ru.yandex.practicum.service.shared.exceptions.NotFoundException;
import ru.yandex.practicum.service.shared.model.Category;
import ru.yandex.practicum.service.shared.model.Compilation;
import ru.yandex.practicum.service.shared.model.CompilationEvents;
import ru.yandex.practicum.service.shared.model.Event;
import ru.yandex.practicum.service.shared.model.User;
import ru.yandex.practicum.service.shared.storage.CategoryRepository;
import ru.yandex.practicum.service.shared.storage.CompilationEventsRepository;
import ru.yandex.practicum.service.shared.storage.CompilationRepository;
import ru.yandex.practicum.service.shared.storage.EventRepository;
import ru.yandex.practicum.service.shared.storage.UserRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
@Getter
@Component
public class PublicCompilationService {
    private final CompilationRepository compilationRepository;
    private final CompilationEventsRepository compilationEventsRepository;
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public CompilationDto getCompilationById(long compId) {
        Optional<Compilation> compilationOptional = compilationRepository.findById(compId);
        if (compilationOptional.isEmpty()) {
            throw new NotFoundException("compilation id=" + compId + " not found");
        }

        List<Long> eventsIds = compilationEventsRepository.findEventIds(compId);
        List<Event> events = eventRepository.findByIdIn(eventsIds);

        List<EventShortDto> eventDtos = eventListToShortDtoList(events);

        return CompilationDtoMapper.toCompilationDto(eventDtos, compilationOptional.get());
    }

    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        List<Compilation> compilations;
        if (pinned != null) {
            compilations = compilationRepository.findByPinned(pinned, PageRequest.of(from, size));
        } else {
            compilations = compilationRepository.findAll(PageRequest.of(from, size)).toList();
        }

        List<Long> compilationIds = new ArrayList<>();
        for (Compilation compilation : compilations) {
            compilationIds.add(compilation.getId());
        }
        List<CompilationEvents> compilationEventsList
                = compilationEventsRepository.findByCompilationIdIn(compilationIds);

        // set нужен для того, чтобы не выгружались дубли событий
        // (если одно событие оказалось в нескольких подборках)
        Set<Long> uniqueEventIds = new HashSet<>();
        for (CompilationEvents compilationEvent : compilationEventsList) {
            uniqueEventIds.add(compilationEvent.getEventId());
        }

        List<Event> AllUniqueEvents = eventRepository.findByIdIn(new ArrayList<>(uniqueEventIds));
        List<EventShortDto> AllUniqueEventDtos = eventListToShortDtoList(AllUniqueEvents);

        List<CompilationDto> compilationDtos = new ArrayList<>();
        for (Compilation compilation : compilations) {
            List<EventShortDto> eventDtos = new ArrayList<>();

            for (CompilationEvents compilationEvent : compilationEventsList) {
                if (compilationEvent.getCompilationId() == compilation.getId()) {

                    for (EventShortDto eventDto : AllUniqueEventDtos) {
                        if (compilationEvent.getEventId() == eventDto.getId()) {
                            eventDtos.add(eventDto);
                            break;
                        }
                    }
                }
            }

            compilationDtos.add(CompilationDtoMapper.toCompilationDto(eventDtos, compilation));
        }

        return compilationDtos;
    }

    private List<EventShortDto> eventListToShortDtoList(List<Event> events) {
        List<EventShortDto> dtoList = new ArrayList<>();

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

            dtoList.add(EventDtoMapper.toShortDto(event, category, user));
        }

        return dtoList;
    }
}
