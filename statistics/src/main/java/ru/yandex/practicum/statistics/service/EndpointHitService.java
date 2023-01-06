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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Getter
@Component
public class EndpointHitService {
    private final EndpointHitRepository endpointHitRepository;

    public EndpointHitDto addHit(EndpointHitDto dto) {
        return EndpointHitDtoMapper
                .toEndpointHitDto(endpointHitRepository
                        .save(EndpointHitDtoMapper.toEndpointHit(dto)));
    }

    public List<ViewStats> getStats(String start, String end, String[] uris, boolean unique) {
        LocalDateTime startDate = LocalDateTime.parse(start, new DateTimeFormat().getFormatter());
        LocalDateTime endDate = LocalDateTime.parse(end, new DateTimeFormat().getFormatter());

        List<ViewStats> result = new ArrayList<>();
        List<String> uriList;
        if (uris != null)
            uriList = List.of(uris);
        else
            uriList = endpointHitRepository.findUris();

        for (String uri : uriList) {
            List<EndpointHit> hits = endpointHitRepository.customFindByUri(uri);
            if (!hits.isEmpty()) {
                String appName = hits.get(0).getApp();

                Long uriHits;
                if (unique)
                    uriHits = endpointHitRepository.countUniqueHitsForUri(uri, startDate, endDate);
                else
                    uriHits = endpointHitRepository.countHitsForUri(uri, startDate, endDate);

                result.add(ViewStats.builder()
                        .app(appName)
                        .uri(uri)
                        .hits(uriHits)
                        .build());
            }
        }

        return result;
    }
}
