package ru.yandex.practicum.service.shared.dto;

import ru.yandex.practicum.service.shared.model.Compilation;

import java.util.List;

public class CompilationDtoMapper {
    public static CompilationDto toCompilationDto(List<EventShortDto> events, Compilation compilation) {
        return CompilationDto.builder()
                .events(events)
                .id(compilation.getId())
                .pinned(compilation.isPinned())
                .title(compilation.getTitle())
                .build();
    }
}
