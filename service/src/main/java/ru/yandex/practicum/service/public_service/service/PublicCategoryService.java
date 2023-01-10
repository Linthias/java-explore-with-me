package ru.yandex.practicum.service.public_service.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.service.shared.dto.CategoryDto;
import ru.yandex.practicum.service.shared.dto.CategoryDtoMapper;
import ru.yandex.practicum.service.shared.exceptions.NotFoundException;
import ru.yandex.practicum.service.shared.model.Category;
import ru.yandex.practicum.service.shared.storage.CategoryRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Getter
@Component
public class PublicCategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryDto getCategoryById(long catId) {
        Optional<Category> category = categoryRepository.findById(catId);
        if (category.isEmpty()) {
            throw new NotFoundException("category id=" + catId + " not found");
        }

        return CategoryDtoMapper.toCategoryDto(category.get());
    }

    public List<CategoryDto> getCategories(int from, int size) {
        List<Category> categories = categoryRepository.findAll(PageRequest.of(from, size)).toList();

        List<CategoryDto> categoryDtos = new ArrayList<>();
        for (Category category : categories) {
            categoryDtos.add(CategoryDtoMapper.toCategoryDto(category));
        }

        return categoryDtos;
    }
}
