package ru.yandex.practicum.service.shared.dto;

import ru.yandex.practicum.service.shared.model.User;

public class UserDtoMapper {
    public static UserDto toFullDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    public static UserShortDto toShortDto(User user) {
        return UserShortDto.builder()
                .id(user.getId())
                .name(user.getName())
                .build();
    }
}
