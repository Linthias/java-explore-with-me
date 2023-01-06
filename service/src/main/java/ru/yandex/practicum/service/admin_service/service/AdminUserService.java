package ru.yandex.practicum.service.admin_service.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.service.admin_service.dto.NewUserRequest;
import ru.yandex.practicum.service.shared.dto.UserDto;
import ru.yandex.practicum.service.shared.dto.UserDtoMapper;
import ru.yandex.practicum.service.shared.exceptions.ConflictException;
import ru.yandex.practicum.service.shared.exceptions.NotFoundException;
import ru.yandex.practicum.service.shared.model.User;
import ru.yandex.practicum.service.shared.storage.UserRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Getter
@Component
public class AdminUserService {
    private final UserRepository userRepository;

    public List<UserDto> getUsers(Long[] ids, int from, int size) {
        Page<User> userPage;
        if (ids != null) {
            List<Long> userIds = List.of(ids);
            userPage = userRepository
                    .findAllByIdIn(userIds, PageRequest.of(from, size, Sort.by("id").ascending()));
        } else
            userPage = userRepository.findAll(PageRequest.of(from, size, Sort.by("id").ascending()));

        List<User> temp = userPage.toList();
        List<UserDto> result = new ArrayList<>();
        for (User user : temp) {
            result.add(UserDtoMapper.toFullDto(user));
        }
        return result;
    }

    public UserDto addUser(NewUserRequest newUser) {
        Set<String> userNames = new HashSet<>(userRepository.findUserNames());
        if (userNames.contains(newUser.getName()))
            throw new ConflictException("user name=" + newUser.getName() + " already exists");

        return UserDtoMapper.toFullDto(userRepository.save(User.builder()
                .name(newUser.getName())
                .email(newUser.getEmail())
                .build()));
    }

    public void removeUser(Long userId) {
        if (!userRepository.existsById(userId))
            throw new NotFoundException("user id=" + userId + " not found");

        userRepository.deleteById(userId);
    }

}
