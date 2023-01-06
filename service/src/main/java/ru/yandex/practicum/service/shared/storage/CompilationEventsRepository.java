package ru.yandex.practicum.service.shared.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.service.shared.model.CompilationEvents;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompilationEventsRepository extends JpaRepository<CompilationEvents, Long> {
    //List<CompilationEvents> findByCompilationId(long compId);
    @Query(value = "select event_id from compilations_events where compilation_id = :compId", nativeQuery = true)
    List<Long> findEventIds(@Param("compId") long compId);
    Optional<CompilationEvents> findByCompilationIdAndEventId(long compId, long eventId);
}
