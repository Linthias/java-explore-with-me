package ru.yandex.practicum.service.shared.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.service.shared.model.EventState;

public interface EventStateRepository extends JpaRepository<EventState, Integer> {
}
