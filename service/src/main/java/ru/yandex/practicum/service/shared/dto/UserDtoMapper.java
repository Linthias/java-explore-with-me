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
}
