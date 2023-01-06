package ru.yandex.practicum.statistics.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.statistics.model.EndpointHit;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EndpointHitRepository extends JpaRepository<EndpointHit, Long> {
    @Query(value = "select distinct uri from endpoint_hits", nativeQuery = true)
    List<String> findUris();
    @Query(value = "select * from endpoint_hits where upper(uri) like ('%' || upper(:paramUri) || '%')", nativeQuery = true)
    List<EndpointHit> customFindByUri(@Param("paramUri") String uri);
    @Query(value = "select distinct count(ip) from endpoint_hits where upper(uri) like ('%' || upper(:paramUri) || '%') and hit_timestamp between :start and :end", nativeQuery = true)
    Long countUniqueHitsForUri(@Param("paramUri") String uri,
                               @Param("start") LocalDateTime start,
                               @Param("end") LocalDateTime end);
    @Query(value = "select count(ip) from endpoint_hits where upper(uri) like ('%' || upper(:paramUri) || '%') and hit_timestamp between :start and :end", nativeQuery = true)
    Long countHitsForUri(@Param("paramUri") String uri,
                         @Param("start") LocalDateTime start,
                         @Param("end") LocalDateTime end);
}
