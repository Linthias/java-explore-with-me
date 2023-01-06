package ru.yandex.practicum.service.admin_service.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.service.admin_service.dto.AdminUpdateEventRequest;
import ru.yandex.practicum.service.admin_service.dto.NewCategoryDto;
import ru.yandex.practicum.service.admin_service.dto.NewCompilationDto;
import ru.yandex.practicum.service.admin_service.dto.NewUserRequest;
import ru.yandex.practicum.service.admin_service.service.AdminCategoryService;
import ru.yandex.practicum.service.admin_service.service.AdminCompilationService;
import ru.yandex.practicum.service.admin_service.service.AdminEventService;
import ru.yandex.practicum.service.admin_service.service.AdminUserService;
import ru.yandex.practicum.service.shared.dto.CategoryDto;
import ru.yandex.practicum.service.shared.dto.CompilationDto;
import ru.yandex.practicum.service.shared.dto.EventFullDto;
import ru.yandex.practicum.service.shared.dto.UserDto;

import javax.validation.Valid;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/admin")
public class AdminController {
    private final AdminEventService eventService;
    private final AdminCategoryService categoryService;
    private final AdminUserService userService;
    private final AdminCompilationService compilationService;

    @GetMapping("/events")
    public List<EventFullDto> getEvents(@RequestParam(required = false) long[] users,
                                        @RequestParam(required = false) String[] states,
                                        @RequestParam(required = false) long[] categories,
                                        @RequestParam(required = false) String rangeStart,
                                        @RequestParam(required = false) String rangeEnd,
                                        @RequestParam(required = false, defaultValue = "0") Integer from,
                                        @RequestParam(required = false, defaultValue = "10") Integer size) {
        log.info("GET: /admin/events?users={}&states={}&categories={}&rangeStart={}&rangeEnd={}&from={}&size={}",
                users, states, categories, rangeStart, rangeEnd, from, size);
        return eventService.getEvents(users, states, categories, rangeStart, rangeEnd, from, size);
    }

    @PutMapping("/events/{eventId}")
    public EventFullDto updateEvent(@PathVariable long eventId,
                                    @RequestBody AdminUpdateEventRequest updateEvent) { // не требует валидации по условию
        log.info("PUT: /admin/events/{}", eventId);
        return eventService.changeEvent(eventId, updateEvent);
    }

    @PatchMapping("/events/{eventId}/publish")
    public EventFullDto publishEvent(@PathVariable long eventId) {
        log.info("PATCH: /admin/events/{}/publish", eventId);
        return eventService.publishEvent(eventId);
    }

    @PatchMapping("/events/{eventId}/reject")
    public EventFullDto rejectEvent(@PathVariable long eventId) {
        log.info("PATCH: /admin/events/{}/reject", eventId);
        return eventService.rejectEvent(eventId);
    }

    @PatchMapping("/categories")
    public CategoryDto patchCategory(@Valid @RequestBody CategoryDto updateCategory) {
        log.info("PATCH: /admin/categories");
        return categoryService.changeCategory(updateCategory);
    }

    @PostMapping("/categories")
    public CategoryDto postCategory(@Valid @RequestBody NewCategoryDto newCategory) {
        log.info("POST: /admin/categories");
        return categoryService.addCategory(newCategory);
    }

    @DeleteMapping("/categories/{catId}")
    public void deleteCategory(@PathVariable long catId) {
        log.info("DELETE: /admin/categories/{}", catId);
        categoryService.removeCategory(catId);
    }

    @GetMapping("/users")
    public List<UserDto> getUsers(@RequestParam(required = false) Long[] ids,
                                  @RequestParam(required = false, defaultValue = "0") Integer from,
                                  @RequestParam(required = false, defaultValue = "10") Integer size) {
        log.info("GET: /admin/users?ids={}&from={}&size={}", ids, from, size);
        return userService.getUsers(ids, from, size);
    }

    @PostMapping("/users")
    public UserDto postUser(@Valid @RequestBody NewUserRequest newUser) {
        log.info("POST: /admin/users");
        return userService.addUser(newUser);
    }

    @DeleteMapping("/users/{userId}")
    public void deleteUser(@PathVariable long userId) {
        log.info("DELETE: /admin/users/{}", userId);
        userService.removeUser(userId);
    }

    @PostMapping("/compilations")
    public CompilationDto postCompilation(@Valid @RequestBody NewCompilationDto newCompilation) {
        log.info("POST: /admin/compilations");
        return compilationService.addCompilation(newCompilation);
    }

    @DeleteMapping("/compilations/{compId}")
    public void deleteCompilation(@PathVariable long compId) {
        log.info("DELETE: /admin/compilations/{}", compId);
        compilationService.removeCompilation(compId);
    }

    @DeleteMapping("/compilations/{compId}/events/{eventId}")
    public void deleteEventFromCompilation(@PathVariable long compId,
                                           @PathVariable long eventId) {
        log.info("DELETE: /admin/compilations/{}/events/{}", compId, eventId);
        compilationService.removeEventFromCompilation(compId, eventId);
    }

    @PatchMapping("/compilations/{compId}/events/{eventId}")
    public void postEventToCompilation(@PathVariable long compId,
                                       @PathVariable long eventId) {
        log.info("PATCH: /admin/compilations/{}/events/{}", compId, eventId);
        compilationService.addEventToCompilation(compId, eventId);
    }

    @DeleteMapping("/compilations/{compId}/pin")
    public void unpinCompilation(@PathVariable long compId) {
        log.info("DELETE: /admin/compilations/{}/pin", compId);
        compilationService.unpinCompilation(compId);
    }

    @PatchMapping("/compilations/{compId}/pin")
    public void pinCompilation(@PathVariable long compId) {
        log.info("PATCH: /admin/compilations/{}/pin", compId);
        compilationService.pinCompilation(compId);
    }
}
