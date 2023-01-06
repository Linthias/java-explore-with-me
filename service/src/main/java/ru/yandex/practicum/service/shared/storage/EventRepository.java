package ru.yandex.practicum.service.shared.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.service.shared.model.Event;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByIdIn(List<Long> ids);
    Integer countByCategoryId(long catId);
}
