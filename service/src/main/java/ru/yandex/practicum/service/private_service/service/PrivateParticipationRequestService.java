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

        if (!event.isModerationRequired() || event.getParticipantLimit() == 0) { // ???????????? ?????? ??????????????/?????????????????? ???? ??????????????????
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

        if (event.getConfirmedRequests() == event.getParticipantLimit()) {  // ???????????????????? ???????????? ????????????, ???????? ?????????? ??????????????????
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

        if (event.getInitiatorId() == userId) { // ???????????? ???????????? ???????????? ???? ?????????????????????? ??????????????
            throw new ForbiddenException("event initiator id=" + event.getInitiatorId() + " can not request to enter own event");
        }
        if (!event.getState().equals(EventState.PUBLISHED)) { // ???????????? ???????????? ???????????? ???? ???????????????????????????????? ??????????????
            throw new ForbiddenException("event id=" + eventId + " not published");
        }
        if (participationRequestRepository.countByEventIdAndRequesterId(eventId, userId) != 0) { // ?????? ???????? ????????????
            throw new ForbiddenException("event id=" + eventId + " already has request from user id=" + userId);
        }
        if (event.getConfirmedRequests() == event.getParticipantLimit()) {
            throw new ForbiddenException("event id=" + eventId + " already has max number of participants");
        }

        ParticipationRequest requestToAdd = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .eventId(eventId)
                .requesterId(userId)
                .build();

        if (event.getParticipantLimit() == 0 || !event.isModerationRequired()) {
            requestToAdd.setState(RequestState.CONFIRMED); // ???????? ?? ?????????????? ?????? ?????????????????? ????????????????
        } else {
            requestToAdd.setState(RequestState.PENDING);
        }

        return ParticipationRequestDtoMapper.toRequestDto(participationRequestRepository.save(requestToAdd));
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

        if (requestToCancel.getState().equals(RequestState.CONFIRMED)) { // ???????? ???????????? ?????? ??????????????
            Event eventToRemoveOneRequest = eventRepository.findById(requestToCancel.getEventId())
                    .orElseThrow(() -> new NotFoundException("event id=" + requestToCancel.getEventId() + " not found"));

            eventToRemoveOneRequest.setConfirmedRequests(eventToRemoveOneRequest.getConfirmedRequests() - 1);
            eventRepository.save(eventToRemoveOneRequest);
        }
        requestToCancel.setState(RequestState.CANCELED);

        return ParticipationRequestDtoMapper.toRequestDto(participationRequestRepository.save(requestToCancel));
    }
}
