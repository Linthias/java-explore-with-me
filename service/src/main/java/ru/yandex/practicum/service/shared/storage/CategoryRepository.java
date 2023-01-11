package ru.yandex.practicum.service.shared.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.service.shared.model.Category;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    @Query(value = "select category_name from categories", nativeQuery = true)
    List<String> findCategoryNames();

    List<Category> findByIdIn(List<Long> categoryIds);
}
