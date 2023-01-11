package ru.yandex.practicum.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EndpointHitDto {
    private Long id;

    @NotBlank
    String app;

    @NotBlank
    String uri;

    @NotBlank
    String ip;

    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$")
    String timestamp;
}
