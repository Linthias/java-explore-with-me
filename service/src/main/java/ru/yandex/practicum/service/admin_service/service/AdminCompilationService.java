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
import ru.yandex.practicum.service.shared.model.Compilation;
import ru.yandex.practicum.service.shared.model.CompilationEvents;
import ru.yandex.practicum.service.shared.model.Event;
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
        List<EventShortDto> events = new ArrayList<>();
        for (Long id : newCompilation.getEvents()) {  // наполнение списка событий для CompilationDto
            Optional<Event> event = eventRepository.findById(id);
            if (event.isEmpty())
                throw new NotFoundException("event id=" + id + " not found");

            events.add(EventDtoMapper.toShortDto(event.get(),
                    categoryRepository.findById(event.get().getCategoryId()).get(),
                    userRepository.findById(event.get().getInitiatorId()).get()));
        }

        Compilation compilation = Compilation.builder()
                .isPinned(false)
                .title(newCompilation.getTitle())
                .build();
        if (newCompilation.getPinned() != null)
            compilation.setPinned(newCompilation.getPinned());

        compilation = compilationRepository.saveAndFlush(compilation);

        for (Long id : newCompilation.getEvents()) {
            compilationEventsRepository.save(CompilationEvents.builder()
                    .compilationId(compilation.getId())
                    .eventId(id)
                    .build());
        }

        return CompilationDtoMapper.toCompilationDto(events, compilation);
    }

    public void removeCompilation(long compId) {
        if (!compilationRepository.existsById(compId))
            throw new NotFoundException("compilation id=" + compId + " not found");

        compilationRepository.deleteById(compId);
    }

    public void removeEventFromCompilation(long compId, long eventId) {
        if (!compilationRepository.existsById(compId))
            throw new NotFoundException("compilation id=" + compId + " not found");
        Optional<CompilationEvents> eventInCompilation
                = compilationEventsRepository.findByCompilationIdAndEventId(compId, eventId);
        if (eventInCompilation.isEmpty())
            throw new NotFoundException("event id=" + eventId + " not found");

        compilationEventsRepository.deleteById(eventInCompilation.get().getId());
    }

    public void addEventToCompilation(long compId, long eventId) {
        if (!compilationRepository.existsById(compId))
            throw new NotFoundException("compilation id=" + compId + " not found");
        Optional<CompilationEvents> eventInCompilation
                = compilationEventsRepository.findByCompilationIdAndEventId(compId, eventId);
        if (eventInCompilation.isPresent())
            throw new ForbiddenException("event id=" + eventId + " already present in compilation id=" + compId);

        compilationEventsRepository.save(CompilationEvents.builder()
                .compilationId(compId)
                .eventId(eventId)
                .build());
    }

    public void unpinCompilation(long compId) {
        Optional<Compilation> compilation = compilationRepository.findById(compId);
        if (compilation.isEmpty())
            throw new NotFoundException("compilation id=" + compId + " not found");

        compilation.get().setPinned(false);
        compilationRepository.save(compilation.get());
    }

    public void pinCompilation(long compId) {
        Optional<Compilation> compilation = compilationRepository.findById(compId);
        if (compilation.isEmpty())
            throw new NotFoundException("compilation id=" + compId + " not found");

        compilation.get().setPinned(true);
        compilationRepository.save(compilation.get());
    }
}
