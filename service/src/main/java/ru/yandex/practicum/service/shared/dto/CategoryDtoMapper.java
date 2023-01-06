package ru.yandex.practicum.service.shared.dto;

import ru.yandex.practicum.service.shared.model.Category;

public class CategoryDtoMapper {
    public static CategoryDto toCategoryDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }
}
