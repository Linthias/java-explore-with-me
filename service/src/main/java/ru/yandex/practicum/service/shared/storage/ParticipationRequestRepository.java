package ru.yandex.practicum.service.shared.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.service.shared.model.ParticipationRequest;

import java.util.List;

@Repository
public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {
    List<ParticipationRequest> findByEventId(long eventId);
    Integer countByEventIdAndStatusId(long eventId, int statusId);
    List<ParticipationRequest> findByEventIdAndStatusId(long eventId, int statusId);
    List<ParticipationRequest> findByRequesterId(long userId);
    Integer countByEventIdAndRequesterId(long eventId, long userId);
}
