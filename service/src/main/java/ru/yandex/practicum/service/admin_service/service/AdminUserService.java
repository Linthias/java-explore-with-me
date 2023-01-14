package ru.yandex.practicum.service.admin_service.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.service.admin_service.dto.NewUserRequest;
import ru.yandex.practicum.service.shared.dto.UserDto;
import ru.yandex.practicum.service.shared.dto.UserDtoMapper;
import ru.yandex.practicum.service.shared.exceptions.ConflictException;
import ru.yandex.practicum.service.shared.exceptions.NotFoundException;
import ru.yandex.practicum.service.shared.model.User;
import ru.yandex.practicum.service.shared.storage.UserRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Getter
@Service
public class AdminUserService {
    private final UserRepository userRepository;

    public List<UserDto> getUsers(Long[] ids, int from, int size) {
        PageRequest usersSortedByIdAsc = PageRequest.of(from, size, Sort.by("id").ascending());
        List<User> users;
        if (ids != null) {
            List<Long> userIds = List.of(ids);
            users = userRepository.findAllByIdIn(userIds, usersSortedByIdAsc).toList();
        } else {
            users = userRepository.findAll(usersSortedByIdAsc).toList();
        }

        return users.stream()
                .map(UserDtoMapper::toFullDto)
                .collect(Collectors.toList());
    }

    public UserDto addUser(NewUserRequest newUser) {
        Set<String> userNames = new HashSet<>(userRepository.findUserNames());
        if (userNames.contains(newUser.getName())) {
            throw new ConflictException("user name=" + newUser.getName() + " already exists");
        }

        return UserDtoMapper.toFullDto(userRepository.save(User.builder()
                .name(newUser.getName())
                .email(newUser.getEmail())
                .build()));
    }

    public void removeUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("user id=" + userId + " not found");
        }

        userRepository.deleteById(userId);
    }

}
