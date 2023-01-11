package ru.yandex.practicum.service.public_service.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import ru.yandex.practicum.service.public_service.dto.EndpointHitDto;
import ru.yandex.practicum.service.shared.model.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StatisticsClient {
    private final RestTemplate rest;
    private static final String API_PREFIX = "/hit";

    @Autowired
    public StatisticsClient(@Value("${stats-server.url}") String serverUrl, RestTemplateBuilder builder) {
        this.rest = builder
                .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl))
                .requestFactory(HttpComponentsClientHttpRequestFactory::new)
                .build();
    }

    public void sendRequestInfo(String appName, String clientIp, String endpointPath) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));


        HttpEntity<EndpointHitDto> requestEntity = new HttpEntity<>(EndpointHitDto.builder()
                .app(appName)
                .uri(endpointPath)
                .ip(clientIp)
                .timestamp(LocalDateTime.now().format(new DateTimeFormat().getFormatter()))
                .build(),
                headers);

        rest.exchange(API_PREFIX, HttpMethod.POST, requestEntity, Object.class);
    }
}
