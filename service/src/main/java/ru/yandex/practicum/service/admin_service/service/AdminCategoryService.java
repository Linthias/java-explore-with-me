package ru.yandex.practicum.service.admin_service.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.service.admin_service.dto.NewCategoryDto;
import ru.yandex.practicum.service.shared.dto.CategoryDto;
import ru.yandex.practicum.service.shared.dto.CategoryDtoMapper;
import ru.yandex.practicum.service.shared.exceptions.BadRequestException;
import ru.yandex.practicum.service.shared.exceptions.ConflictException;
import ru.yandex.practicum.service.shared.exceptions.ForbiddenException;
import ru.yandex.practicum.service.shared.exceptions.NotFoundException;
import ru.yandex.practicum.service.shared.model.Category;
import ru.yandex.practicum.service.shared.storage.CategoryRepository;
import ru.yandex.practicum.service.shared.storage.EventRepository;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
@Getter
@Component
public class AdminCategoryService {
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;

    public CategoryDto changeCategory(CategoryDto updateCategory) {
        Optional<Category> categoryOptional = categoryRepository.findById(updateCategory.getId());
        if (categoryOptional.isEmpty()) {
            throw new BadRequestException("category id=" + updateCategory.getId() + " not found");
        }
        Category categoryToChange = categoryOptional.get();

        Set<String> categoryNames = new HashSet<>(categoryRepository.findCategoryNames());
        if (categoryNames.contains(updateCategory.getName())) {
            throw new ConflictException("category name=" + updateCategory.getName() + " already exists");
        }

        categoryToChange.setName(updateCategory.getName());

        return CategoryDtoMapper.toCategoryDto(categoryRepository.save(categoryToChange));
    }

    public CategoryDto addCategory(NewCategoryDto newCategory) {
        Set<String> categoryNames = new HashSet<>(categoryRepository.findCategoryNames());
        if (categoryNames.contains(newCategory.getName())) {
            throw new ConflictException("category name=" + newCategory.getName() + " already exists");
        }

        return CategoryDtoMapper.toCategoryDto(categoryRepository.save(Category.builder()
                .name(newCategory.getName())
                .build()));
    }

    public void removeCategory(long catId) {
        if (!categoryRepository.existsById(catId)) {
            throw new NotFoundException("category id=" + catId + " not found");
        }

        if (eventRepository.countByCategoryId(catId) != 0) {
            throw new ForbiddenException("category id=" + catId + " still has events");
        }

        categoryRepository.deleteById(catId);
    }
}
