package ru.yandex.practicum.service.shared.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.service.shared.model.RequestState;

public interface RequestStateRepository extends JpaRepository<RequestState, Integer> {
}
