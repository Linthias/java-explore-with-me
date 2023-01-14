package ru.yandex.practicum.statistics.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.statistics.dto.EndpointHitDto;
import ru.yandex.practicum.statistics.dto.ViewStats;
import ru.yandex.practicum.statistics.service.EndpointHitService;

import javax.validation.Valid;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
public class StatisticsController {
    private final EndpointHitService endpointHitService;

    @PostMapping("/hit")
    public EndpointHitDto postHit(@Valid @RequestBody EndpointHitDto dto) {
        log.info("POST: /hit  body:{app={} uri={} ip={} timestamp={}}",
                dto.getApp(), dto.getUri(), dto.getIp(), dto.getTimestamp());
        return endpointHitService.addHit(dto);
    }

    @GetMapping("/stats")
    public List<ViewStats> getStatistics(@RequestParam String start,
                                         @RequestParam String end,
                                         @RequestParam(required = false) String[] uris,
                                         @RequestParam(required = false, defaultValue = "false") Boolean unique) {
        log.info("GET: /stats?start={}&end={}&uris={}&unique={}", start, end, uris, unique);
        return endpointHitService.getStats(start, end, uris, unique);
    }
}
