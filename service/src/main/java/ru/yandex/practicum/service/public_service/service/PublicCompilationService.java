package ru.yandex.practicum.service.public_service.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.service.shared.dto.CompilationDto;
import ru.yandex.practicum.service.shared.dto.CompilationDtoMapper;
import ru.yandex.practicum.service.shared.dto.EventDtoMapper;
import ru.yandex.practicum.service.shared.dto.EventShortDto;
import ru.yandex.practicum.service.shared.exceptions.NotFoundException;
import ru.yandex.practicum.service.shared.model.Category;
import ru.yandex.practicum.service.shared.model.Compilation;
import ru.yandex.practicum.service.shared.model.Event;
import ru.yandex.practicum.service.shared.model.User;
import ru.yandex.practicum.service.shared.storage.CategoryRepository;
import ru.yandex.practicum.service.shared.storage.CompilationEventsRepository;
import ru.yandex.practicum.service.shared.storage.CompilationRepository;
import ru.yandex.practicum.service.shared.storage.EventRepository;
import ru.yandex.practicum.service.shared.storage.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        Optional<Compilation> compilation = compilationRepository.findById(compId);
        if (compilation.isEmpty())
            throw new NotFoundException("compilation id=" + compId + " not found");

        List<Long> eventsIds = compilationEventsRepository.findEventIds(compId);
        List<Event> events = eventRepository.findByIdIn(eventsIds);

        List<EventShortDto> eventDtos = new ArrayList<>();
        for (Event event : events) {
            Optional<Category> category = categoryRepository.findById(event.getCategoryId());
            Optional<User> user = userRepository.findById(event.getInitiatorId());
            eventDtos.add(EventDtoMapper.toShortDto(event, category.get(), user.get()));
        }

        return CompilationDtoMapper.toCompilationDto(eventDtos, compilation.get());
    }

    public List<CompilationDto> getCompilations(Boolean pinned,
                                                int from,
                                                int size) {
        Page<Compilation> compilationPage = compilationRepository.findAll(PageRequest.of(from, size));

        List<Compilation> temp;
        if (pinned != null)
            if (pinned)
                temp = compilationPage.get().filter(Compilation::isPinned).collect(Collectors.toList());
            else
                temp = compilationPage.get().filter(compilation -> !compilation.isPinned()).collect(Collectors.toList());
        else
            temp = compilationPage.get().collect(Collectors.toList());

        List<CompilationDto> result = new ArrayList<>();
        for (Compilation compilation : temp) {
            List<Long> eventsIds = compilationEventsRepository.findEventIds(compilation.getId());
            List<Event> events = eventRepository.findByIdIn(eventsIds);

            List<EventShortDto> eventDtos = new ArrayList<>();
            for (Event event : events) {
                Optional<Category> category = categoryRepository.findById(event.getCategoryId());
                Optional<User> user = userRepository.findById(event.getInitiatorId());
                eventDtos.add(EventDtoMapper.toShortDto(event, category.get(), user.get()));
            }

            result.add(CompilationDtoMapper.toCompilationDto(eventDtos, compilation));
        }

        return result;
    }
}
