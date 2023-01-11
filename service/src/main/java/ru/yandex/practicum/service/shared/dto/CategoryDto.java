package ru.yandex.practicum.service.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryDto {
    @NotNull
    private Long id;
    @NotBlank
    private String name;
}
