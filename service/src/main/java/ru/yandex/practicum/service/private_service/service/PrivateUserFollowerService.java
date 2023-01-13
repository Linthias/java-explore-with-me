package ru.yandex.practicum.service.private_service.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.service.shared.dto.EventDtoMapper;
import ru.yandex.practicum.service.shared.dto.EventShortDto;
import ru.yandex.practicum.service.shared.dto.UserDtoMapper;
import ru.yandex.practicum.service.shared.dto.UserShortDto;
import ru.yandex.practicum.service.shared.exceptions.BadRequestException;
import ru.yandex.practicum.service.shared.exceptions.ForbiddenException;
import ru.yandex.practicum.service.shared.exceptions.NotFoundException;
import ru.yandex.practicum.service.shared.model.Category;
import ru.yandex.practicum.service.shared.model.Event;
import ru.yandex.practicum.service.shared.model.EventState;
import ru.yandex.practicum.service.shared.model.User;
import ru.yandex.practicum.service.shared.model.UserFollower;
import ru.yandex.practicum.service.shared.storage.CategoryRepository;
import ru.yandex.practicum.service.shared.storage.EventRepository;
import ru.yandex.practicum.service.shared.storage.UserFollowerRepository;
import ru.yandex.practicum.service.shared.storage.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Getter
@Component
public class PrivateUserFollowerService {
    private final UserFollowerRepository userFollowerRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;

    public UserShortDto addNewFollower(long userId, long followerId) {
        if (userId == followerId) {
            throw new BadRequestException("user id=" + userId + " can not follow themselves");
        }
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("user id=" + userId + " not found");
        }

        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new NotFoundException("user id=" + userId + " not found"));

        if (userFollowerRepository.existsByUserIdAndFollowerId(userId, followerId)) {
            throw new ForbiddenException("user id=" + userId + " already has follower id=" + followerId);
        }

        userFollowerRepository.save(UserFollower.builder()
                .userId(userId)
                .followerId(followerId)
                .build());

        return UserDtoMapper.toShortDto(follower);
    }

    public void removeFollower(long userId, long followerId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("user id=" + userId + " not found");
        }
        if (!userRepository.existsById(followerId)) {
            throw new NotFoundException("user id=" + followerId + " not found");
        }

        Long userFollowerId = userFollowerRepository.findUserFollowerId(userId, followerId)
                .orElseThrow(() -> new NotFoundException("user id=" + userId + " do not have follower id=" + followerId));

        userFollowerRepository.deleteById(userFollowerId);
    }

    public List<EventShortDto> getFolloweeEvents(long userId, long followerId, int from, int size) {
        if (userId == followerId) {
            throw new BadRequestException("user id=" + userId + " can not follow themselves");
        }

        User eventOwner = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("user id=" + userId + " not found"));

        if (!userRepository.existsById(followerId)) {
            throw new NotFoundException("user id=" + followerId + " not found");
        }
        if (!userFollowerRepository.existsByUserIdAndFollowerId(userId, followerId)) {
            throw new ForbiddenException("user id=" + userId + " do not have follower id=" + followerId);
        }

        List<Event> userEvents = eventRepository.findByInitiatorIdAndState(userId, EventState.PUBLISHED,
                PageRequest.of(from, size, Sort.by(Sort.Direction.ASC, "id")));

        List<Long> categoryIds = userEvents.stream()
                .map(Event::getCategoryId)
                .collect(Collectors.toList());

        List<Category> eventsCategories = categoryRepository.findByIdIn(categoryIds);

        List<EventShortDto> userEventsDtos = new ArrayList<>();
        for (Event event : userEvents) {
            Category category = eventsCategories.stream().filter(cat -> cat.getId() == event.getCategoryId()).findAny()
                    .orElseThrow(() -> new NotFoundException("category id=" + event.getCategoryId() + " not found"));

            userEventsDtos.add(EventDtoMapper.toShortDto(event, category, eventOwner));
        }

        return userEventsDtos;
    }
}
