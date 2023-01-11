package ru.yandex.practicum.statistics.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.statistics.dto.EndpointHitDto;
import ru.yandex.practicum.statistics.dto.EndpointHitDtoMapper;
import ru.yandex.practicum.statistics.dto.ViewStats;
import ru.yandex.practicum.statistics.model.DateTimeFormat;
import ru.yandex.practicum.statistics.model.EndpointHit;
import ru.yandex.practicum.statistics.storage.EndpointHitRepository;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Getter
@Component
public class EndpointHitService {
    private final EndpointHitRepository endpointHitRepository;
    private final EntityManager entityManager;

    public EndpointHitDto addHit(EndpointHitDto dto) {
        return EndpointHitDtoMapper
                .toEndpointHitDto(endpointHitRepository
                        .save(EndpointHitDtoMapper.toEndpointHit(dto)));
    }

    public List<ViewStats> getStats(String start, String end, String[] uris, boolean unique) {
        LocalDateTime startDate = LocalDateTime.parse(start, new DateTimeFormat().getFormatter());
        LocalDateTime endDate = LocalDateTime.parse(end, new DateTimeFormat().getFormatter());

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = builder.createTupleQuery();
        Root<EndpointHit> root = query.from(EndpointHit.class);
        List<Predicate> predicates = new ArrayList<>();

        Path<LocalDateTime> dateTimePath = root.get("timestamp");
        predicates.add(builder.between(dateTimePath, startDate, endDate));

        if (uris != null) { // ((A v B) v C) = A v B v C
            Predicate previousUriPredicate = builder.like(root.get("uri"), uris[0]);
            Predicate finalUriPredicate = null;

            for (String uri : uris) {
                Predicate currentUriPredicate = builder.like(root.get("uri"), uri);
                finalUriPredicate = builder.or(previousUriPredicate, currentUriPredicate);
                previousUriPredicate = finalUriPredicate;
            }
            predicates.add(finalUriPredicate);
        }

        if (unique) {
            query.multiselect(root.get("app"), root.get("uri"), builder.countDistinct(root.get("ip")));
        } else {
            query.multiselect(root.get("app"), root.get("uri"), builder.count(root.get("ip")));
        }

        query.where(predicates.toArray(new Predicate[]{}));
        query.groupBy(root.get("app"), root.get("uri"));

        List<ViewStats> statisticsDtos = new ArrayList<>();
        List<Tuple> tuples = entityManager.createQuery(query).getResultList();
        for (Tuple tuple : tuples) {
            statisticsDtos.add(ViewStats.builder()
                    .app(tuple.get(0, String.class))
                    .uri(tuple.get(1, String.class))
                    .hits(tuple.get(2, Long.class))
                    .build());
        }

        return statisticsDtos;
    }
}
