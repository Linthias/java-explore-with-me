package ru.yandex.practicum.service.private_service.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.service.private_service.dto.NewEventDto;
import ru.yandex.practicum.service.private_service.dto.ParticipationRequestDto;
import ru.yandex.practicum.service.private_service.dto.UpdateEventRequest;
import ru.yandex.practicum.service.private_service.service.PrivateEventService;
import ru.yandex.practicum.service.private_service.service.PrivateParticipationRequestService;
import ru.yandex.practicum.service.shared.dto.EventFullDto;
import ru.yandex.practicum.service.shared.dto.EventShortDto;

import javax.validation.Valid;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/users")
public class PrivateController {
    private final PrivateEventService eventService;
    private final PrivateParticipationRequestService participationRequestService;

    @GetMapping("/{userId}/events")
    public List<EventShortDto> getUsersEvents(@PathVariable long userId,
                                              @RequestParam(required = false, defaultValue = "0") Integer from,
                                              @RequestParam(required = false, defaultValue = "10") Integer size) {
        log.info("GET: /users/{}/events?from={}&size={}", userId, from, size);
        return eventService.getUsersEvents(userId, from, size);
    }

    @PatchMapping("/{userId}/events")
    public EventFullDto patchEvent(@PathVariable long userId,
                                   @Valid @RequestBody UpdateEventRequest eventUpdate) {
        log.info("PATCH: /users/{}/events", userId);
        return eventService.changeEvent(userId, eventUpdate);
    }

    @PostMapping("/{userId}/events")
    public EventFullDto postEvent(@PathVariable long userId,
                                  @Valid @RequestBody NewEventDto eventNew) {
        log.info("POST: /users/{}/events", userId);
        return eventService.addEvent(userId, eventNew);
    }

    @GetMapping("/{userId}/events/{eventId}")
    public EventFullDto getUserEvent(@PathVariable long userId,
                                     @PathVariable long eventId) {
        log.info("GET: /users/{}/events/{}", userId, eventId);
        return eventService.getEvent(userId, eventId);
    }

    @PatchMapping("/{userId}/events/{eventId}")
    public EventFullDto cancelUserEvent(@PathVariable long userId,
                                        @PathVariable long eventId) {
        log.info("PATCH: /users/{}/events/{}", userId, eventId);
        return eventService.cancelEvent(userId, eventId);
    }

    @GetMapping("/{userId}/events/{eventId}/requests")
    public List<ParticipationRequestDto> getEventRequests(@PathVariable long userId,
                                                          @PathVariable long eventId) {
        log.info("GET: /users/{}/events/{}/requests", userId, eventId);
        return participationRequestService.getEventRequests(userId, eventId);
    }

    @PatchMapping("/{userId}/events/{eventId}/requests/{reqId}/confirm")
    public ParticipationRequestDto acceptRequest(@PathVariable long userId,
                                                 @PathVariable long eventId,
                                                 @PathVariable long reqId) {
        log.info("PATCH: /users/{}/events/{}/requests/{}/confirm", userId, eventId, reqId);
        return participationRequestService.acceptRequest(userId, eventId, reqId);
    }

    @PatchMapping("/{userId}/events/{eventId}/requests/{reqId}/reject")
    public ParticipationRequestDto rejectRequest(@PathVariable long userId,
                                                 @PathVariable long eventId,
                                                 @PathVariable long reqId) {
        log.info("PATCH: /users/{}/events/{}/requests/{}/reject", userId, eventId, reqId);
        return participationRequestService.rejectRequest(userId, eventId, reqId);
    }

    @GetMapping("/{userId}/requests")
    public List<ParticipationRequestDto> getUserRequests(@PathVariable long userId) {
        log.info("GET: /users/{}/requests", userId);
        return participationRequestService.getUserRequests(userId);
    }

    @PostMapping("/{userId}/requests")
    public ParticipationRequestDto postRequest(@PathVariable long userId,
                                               @RequestParam long eventId) {
        log.info("POST: /users/{}/requests?eventId={}", userId, eventId);
        return participationRequestService.addRequest(userId, eventId);
    }

    @PatchMapping("/{userId}/requests/{reqId}/cancel")
    public ParticipationRequestDto cancelOwnRequest(@PathVariable long userId,
                                                    @PathVariable long reqId) {
        log.info("PATCH: /users/{}/requests/{}/cancel", userId, reqId);
        return participationRequestService.cancelOwnRequest(userId, reqId);
    }
}
