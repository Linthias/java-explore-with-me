package ru.yandex.practicum.service.public_service.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.service.shared.dto.CategoryDto;
import ru.yandex.practicum.service.shared.dto.CategoryDtoMapper;
import ru.yandex.practicum.service.shared.exceptions.NotFoundException;
import ru.yandex.practicum.service.shared.storage.CategoryRepository;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Getter
@Component
public class PublicCategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryDto getCategoryById(long catId) {
        return CategoryDtoMapper.toCategoryDto(categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("category id=" + catId + " not found")));
    }

    public List<CategoryDto> getCategories(int from, int size) {
        return categoryRepository.findAll(PageRequest.of(from, size)).toList().stream()
                .map(CategoryDtoMapper::toCategoryDto)
                .collect(Collectors.toList());
    }
}
