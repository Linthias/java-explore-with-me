package ru.yandex.practicum.service.private_service.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.service.private_service.dto.ParticipationRequestDto;
import ru.yandex.practicum.service.private_service.dto.ParticipationRequestDtoMapper;
import ru.yandex.practicum.service.shared.exceptions.ForbiddenException;
import ru.yandex.practicum.service.shared.exceptions.NotFoundException;
import ru.yandex.practicum.service.shared.model.Event;
import ru.yandex.practicum.service.shared.model.EventState;
import ru.yandex.practicum.service.shared.model.ParticipationRequest;
import ru.yandex.practicum.service.shared.model.RequestState;
import ru.yandex.practicum.service.shared.storage.EventRepository;
import ru.yandex.practicum.service.shared.storage.ParticipationRequestRepository;
import ru.yandex.practicum.service.shared.storage.UserFollowerRepository;
import ru.yandex.practicum.service.shared.storage.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Getter
@Component
public class PrivateParticipationRequestService {
    private final ParticipationRequestRepository participationRequestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final UserFollowerRepository userFollowerRepository;

    public List<ParticipationRequestDto> getEventRequests(long userId, long eventId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("user id=" + userId + " not found");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("event id=" + eventId + " not found"));

        if (event.getInitiatorId() != userId) {
            throw new ForbiddenException("event initiator id=" + event.getInitiatorId() + " and user id=" + userId);
        }

        List<ParticipationRequest> requests = participationRequestRepository.findByEventId(eventId);

        return requests.stream()
                .map(ParticipationRequestDtoMapper::toRequestDto)
                .collect(Collectors.toList());
    }

    public ParticipationRequestDto acceptRequest(long userId, long eventId, long reqId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("user id=" + userId + " not found");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("event id=" + eventId + " not found"));

        if (event.getInitiatorId() != userId) {
            throw new ForbiddenException("event initiator id=" + event.getInitiatorId() + " and user id=" + userId);
        }

        ParticipationRequest request = participationRequestRepository.findById(reqId)
                .orElseThrow(() -> new NotFoundException("participation request id=" + reqId + " not found"));

        // запрос уже одобрен или отклонен/одобрение не требуется/запрос от подписчика
        if (!request.getState().equals(RequestState.PENDING)) {
            throw new ForbiddenException("event id=" + eventId + " does not require moderation");
        }

        if (event.getConfirmedRequests() >= event.getParticipantLimit()) {
            request.setState(RequestState.REJECTED);
        } else {
            request.setState(RequestState.CONFIRMED);
            event.setConfirmedRequests(event.getConfirmedRequests() + 1);
            eventRepository.save(event);
        }

        ParticipationRequestDto acceptedRequestDto
                = ParticipationRequestDtoMapper.toRequestDto(participationRequestRepository.saveAndFlush(request));

        if (event.getConfirmedRequests() == event.getParticipantLimit()) {  // отклонение прочих заявок, если лимит достигнут
            List<ParticipationRequest> requestsToCancel
                    = participationRequestRepository.findByEventIdAndState(eventId, RequestState.PENDING);

            participationRequestRepository.saveAll(requestsToCancel.stream()
                    .peek(requestToCancel -> requestToCancel.setState(RequestState.REJECTED))
                    .collect(Collectors.toList()));
        }

        return acceptedRequestDto;
    }

    public ParticipationRequestDto rejectRequest(long userId, long eventId, long reqId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("user id=" + userId + " not found");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("event id=" + eventId + " not found"));

        if (event.getInitiatorId() != userId) {
            throw new ForbiddenException("event initiator id=" + event.getInitiatorId() + " and user id=" + userId);
        }

        ParticipationRequest request = participationRequestRepository.findById(reqId)
                .orElseThrow(() -> new NotFoundException("participation request id=" + reqId + " not found"));

        request.setState(RequestState.REJECTED);
        return ParticipationRequestDtoMapper.toRequestDto(participationRequestRepository.save(request));
    }

    public List<ParticipationRequestDto> getUserRequests(long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("user id=" + userId + " not found");
        }

        List<ParticipationRequest> userRequests = participationRequestRepository.findByRequesterId(userId);

        return userRequests.stream()
                .map(ParticipationRequestDtoMapper::toRequestDto)
                .collect(Collectors.toList());
    }

    public ParticipationRequestDto addRequest(long userId, long eventId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("user id=" + userId + " not found");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("event id=" + eventId + " not found"));

        if (event.getInitiatorId() == userId) { // нельзя подать заявку на собственное событие
            throw new ForbiddenException("event initiator id=" + event.getInitiatorId() + " can not request to enter own event");
        }
        if (!event.getState().equals(EventState.PUBLISHED)) { // нельзя подать заявку на неопубликованное событие
            throw new ForbiddenException("event id=" + eventId + " not published");
        }
        if (participationRequestRepository.countByEventIdAndRequesterId(eventId, userId) != 0) { // уже есть заявка
            throw new ForbiddenException("event id=" + eventId + " already has request from user id=" + userId);
        }
        if (event.getConfirmedRequests() == event.getParticipantLimit()
                && event.getParticipantLimit() > 0) {
            throw new ForbiddenException("event id=" + eventId + " already has max number of participants");
        }

        ParticipationRequest requestToAdd = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .eventId(eventId)
                .requesterId(userId)
                .build();

        // если у события нет модерации запросов/ограничения на кол-во заявок/запрос от подписчика
        if (event.getParticipantLimit() == 0
                || !event.isModerationRequired()
                || userFollowerRepository.existsByUserIdAndFollowerId(event.getInitiatorId(), userId)) {
            requestToAdd.setState(RequestState.CONFIRMED);
            event.setConfirmedRequests(event.getConfirmedRequests() + 1);
            eventRepository.save(event);
        } else {
            requestToAdd.setState(RequestState.PENDING);
        }

        ParticipationRequestDto requestDto
                = ParticipationRequestDtoMapper.toRequestDto(participationRequestRepository.saveAndFlush(requestToAdd));

        if (event.getConfirmedRequests() == event.getParticipantLimit()) {  // отклонение прочих заявок, если лимит достигнут
            List<ParticipationRequest> requestsToCancel
                    = participationRequestRepository.findByEventIdAndState(eventId, RequestState.PENDING);

            participationRequestRepository.saveAll(requestsToCancel.stream()
                    .peek(requestToCancel -> requestToCancel.setState(RequestState.REJECTED))
                    .collect(Collectors.toList()));
        }

        return requestDto;
    }

    public ParticipationRequestDto cancelOwnRequest(long userId, long reqId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("user id=" + userId + " not found");
        }

        ParticipationRequest requestToCancel = participationRequestRepository.findById(reqId)
                .orElseThrow(() -> new NotFoundException("participation request id=" + reqId + " not found"));

        if (requestToCancel.getRequesterId() != userId) {
            throw new ForbiddenException("user id=" + userId + " can not cancel not owned request id=" + reqId);
        }

        if (requestToCancel.getState().equals(RequestState.CONFIRMED)) { // если запрос уже одобрен
            Event eventToRemoveOneRequest = eventRepository.findById(requestToCancel.getEventId())
                    .orElseThrow(() -> new NotFoundException("event id=" + requestToCancel.getEventId() + " not found"));

            if (eventToRemoveOneRequest.getConfirmedRequests() > 0) {
                eventToRemoveOneRequest.setConfirmedRequests(eventToRemoveOneRequest.getConfirmedRequests() - 1);
            }
            eventRepository.save(eventToRemoveOneRequest);
        }
        requestToCancel.setState(RequestState.CANCELED);

        return ParticipationRequestDtoMapper.toRequestDto(participationRequestRepository.save(requestToCancel));
    }
}
