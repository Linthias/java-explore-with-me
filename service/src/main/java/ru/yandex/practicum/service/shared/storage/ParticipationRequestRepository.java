package ru.yandex.practicum.service.shared.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.service.shared.model.ParticipationRequest;
import ru.yandex.practicum.service.shared.model.RequestState;

import java.util.List;

@Repository
public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {
    List<ParticipationRequest> findByEventId(long eventId);

    List<ParticipationRequest> findByEventIdAndState(long eventId, RequestState state);

    List<ParticipationRequest> findByRequesterId(long userId);

    Integer countByEventIdAndRequesterId(long eventId, long userId);
}
