package ru.yandex.practicum.service.admin_service.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.service.admin_service.dto.NewCompilationDto;
import ru.yandex.practicum.service.shared.dto.CompilationDto;
import ru.yandex.practicum.service.shared.dto.CompilationDtoMapper;
import ru.yandex.practicum.service.shared.dto.EventDtoMapper;
import ru.yandex.practicum.service.shared.dto.EventShortDto;
import ru.yandex.practicum.service.shared.exceptions.ForbiddenException;
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
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Getter
@Component
public class AdminCompilationService {
    private final CompilationRepository compilationRepository;
    private final CompilationEventsRepository compilationEventsRepository;
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public CompilationDto addCompilation(NewCompilationDto newCompilation) {
        List<Event> eventsForNewCompilation = eventRepository.findByIdIn(newCompilation.getEvents());
        List<EventShortDto> eventsForNewCompilationDtos = new ArrayList<>();

        List<Long> categoryIds = new ArrayList<>();
        List<Long> initiatorIds = new ArrayList<>();
        for (Event event : eventsForNewCompilation) {
            categoryIds.add(event.getCategoryId());
            initiatorIds.add(event.getInitiatorId());
        }
        List<Category> eventsCategories = categoryRepository.findByIdIn(categoryIds);
        List<User> eventsOwners = userRepository.findByIdIn(initiatorIds);
        for (Event event : eventsForNewCompilation) {
            Category category = eventsCategories.stream().filter(cat -> cat.getId() == event.getCategoryId()).findAny().get();
            User user = eventsOwners.stream().filter(user1 -> user1.getId() == event.getInitiatorId()).findAny().get();
            eventsForNewCompilationDtos.add(EventDtoMapper.toShortDto(event, category, user));
        }

        Compilation compilationToAdd = Compilation.builder()
                .pinned(false)
                .title(newCompilation.getTitle())
                .build();
        if (newCompilation.getPinned() != null) {
            compilationToAdd.setPinned(newCompilation.getPinned());
        }

        compilationToAdd = compilationRepository.saveAndFlush(compilationToAdd);

        List<CompilationEvents> newCompilationEventsList = new ArrayList<>();
        for (EventShortDto eventDto : eventsForNewCompilationDtos) {
            newCompilationEventsList.add(CompilationEvents.builder()
                    .compilationId(compilationToAdd.getId())
                    .eventId(eventDto.getId())
                    .build());
        }

        compilationEventsRepository.saveAll(newCompilationEventsList);

        return CompilationDtoMapper.toCompilationDto(eventsForNewCompilationDtos, compilationToAdd);
    }

    public void removeCompilation(long compId) {
        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("compilation id=" + compId + " not found");
        }

        compilationRepository.deleteById(compId);
    }

    public void removeEventFromCompilation(long compId, long eventId) {
        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("compilation id=" + compId + " not found");
        }

        Optional<CompilationEvents> eventInCompilation
                = compilationEventsRepository.findByCompilationIdAndEventId(compId, eventId);
        if (eventInCompilation.isEmpty()) {
            throw new NotFoundException("event id=" + eventId + " not found");
        }

        compilationEventsRepository.deleteById(eventInCompilation.get().getId());
    }

    public void addEventToCompilation(long compId, long eventId) {
        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("compilation id=" + compId + " not found");
        }

        Optional<CompilationEvents> eventInCompilation
                = compilationEventsRepository.findByCompilationIdAndEventId(compId, eventId);
        if (eventInCompilation.isPresent()) {
            throw new ForbiddenException("event id=" + eventId + " already present in compilation id=" + compId);
        }

        compilationEventsRepository.save(CompilationEvents.builder()
                .compilationId(compId)
                .eventId(eventId)
                .build());
    }

    public void unpinCompilation(long compId) {
        Optional<Compilation> compilationOptional = compilationRepository.findById(compId);
        if (compilationOptional.isEmpty()) {
            throw new NotFoundException("compilation id=" + compId + " not found");
        }
        Compilation compilationToUnpin = compilationOptional.get();

        compilationToUnpin.setPinned(false);
        compilationRepository.save(compilationToUnpin);
    }

    public void pinCompilation(long compId) {
        Optional<Compilation> compilationOptional = compilationRepository.findById(compId);
        if (compilationOptional.isEmpty()) {
            throw new NotFoundException("compilation id=" + compId + " not found");
        }
        Compilation compilationToPin = compilationOptional.get();

        compilationToPin.setPinned(true);
        compilationRepository.save(compilationToPin);
    }
}
