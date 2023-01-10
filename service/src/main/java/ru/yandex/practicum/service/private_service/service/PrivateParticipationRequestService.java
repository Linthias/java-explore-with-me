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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

        Optional<Event> eventOptional = eventRepository.findById(eventId);
        if (eventOptional.isEmpty()) {
            throw new NotFoundException("event id=" + eventId + " not found");
        }

        Event event = eventOptional.get();

        if (event.getInitiatorId() != userId) {
            throw new ForbiddenException("event initiator id=" + event.getInitiatorId() + " and user id=" + userId);
        }

        List<ParticipationRequest> requests = participationRequestRepository.findByEventId(eventId);
        List<ParticipationRequestDto> requestDtos = new ArrayList<>();
        for (ParticipationRequest request : requests) {
            requestDtos.add(ParticipationRequestDtoMapper.toRequestDto(request));
        }

        return requestDtos;
    }

    public ParticipationRequestDto acceptRequest(long userId, long eventId, long reqId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("user id=" + userId + " not found");
        }

        Optional<Event> eventOptional = eventRepository.findById(eventId);
        if (eventOptional.isEmpty()) {
            throw new NotFoundException("event id=" + eventId + " not found");
        }
        Event event = eventOptional.get();

        if (event.getInitiatorId() != userId) {
            throw new ForbiddenException("event initiator id=" + event.getInitiatorId() + " and user id=" + userId);
        }

        Optional<ParticipationRequest> requestOptional = participationRequestRepository.findById(reqId);
        if (requestOptional.isEmpty()) {
            throw new NotFoundException("participation request id=" + reqId + " not found");
        }
        ParticipationRequest request = requestOptional.get();

        if (!event.isModerationRequired() || event.getParticipantLimit() == 0) {
            throw new ForbiddenException("event id=" + eventId + " does not require moderation"); // запрос уже одобрен/одобрение не требуется
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

            for (ParticipationRequest requestToCancel : requestsToCancel) {
                requestToCancel.setState(RequestState.REJECTED);
            }
            participationRequestRepository.saveAll(requestsToCancel);
        }

        return acceptedRequestDto;
    }

    public ParticipationRequestDto rejectRequest(long userId, long eventId, long reqId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("user id=" + userId + " not found");
        }

        Optional<Event> eventOptional = eventRepository.findById(eventId);
        if (eventOptional.isEmpty()) {
            throw new NotFoundException("event id=" + eventId + " not found");
        }
        Event event = eventOptional.get();

        if (event.getInitiatorId() != userId) {
            throw new ForbiddenException("event initiator id=" + event.getInitiatorId() + " and user id=" + userId);
        }

        Optional<ParticipationRequest> requestOptional = participationRequestRepository.findById(reqId);
        if (requestOptional.isEmpty()) {
            throw new NotFoundException("participation request id=" + reqId + " not found");
        }
        ParticipationRequest request = requestOptional.get();

        request.setState(RequestState.REJECTED);
        return ParticipationRequestDtoMapper.toRequestDto(participationRequestRepository.save(request));
    }

    public List<ParticipationRequestDto> getUserRequests(long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("user id=" + userId + " not found");
        }

        List<ParticipationRequest> userRequests = participationRequestRepository.findByRequesterId(userId);
        List<ParticipationRequestDto> userRequestsDtos = new ArrayList<>();
        for (ParticipationRequest request : userRequests) {
            userRequestsDtos.add(ParticipationRequestDtoMapper.toRequestDto(request));
        }

        return userRequestsDtos;
    }

    public ParticipationRequestDto addRequest(long userId, long eventId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("user id=" + userId + " not found");
        }

        Optional<Event> eventOptional = eventRepository.findById(eventId);
        if (eventOptional.isEmpty()) {
            throw new NotFoundException("event id=" + eventId + " not found");
        }
        Event event = eventOptional.get();

        if (event.getInitiatorId() == userId) { // нельзя подать заявку на собственное событие
            throw new ForbiddenException("event initiator id=" + event.getInitiatorId() + " can not request to enter own event");
        }
        if (!event.getState().equals(EventState.PUBLISHED)) { // нельзя подать заявку на неопубликованное событие
            throw new ForbiddenException("event id=" + eventId + " not published");
        }
        if (participationRequestRepository.countByEventIdAndRequesterId(eventId, userId) != 0) { // уже есть заявка
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
            requestToAdd.setState(RequestState.CONFIRMED); // если у события нет модерации запросов
        } else {
            requestToAdd.setState(RequestState.PENDING);
        }

        return ParticipationRequestDtoMapper.toRequestDto(participationRequestRepository.save(requestToAdd));
    }

    public ParticipationRequestDto cancelOwnRequest(long userId, long reqId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("user id=" + userId + " not found");
        }

        Optional<ParticipationRequest> requestOptional = participationRequestRepository.findById(reqId);
        if (requestOptional.isEmpty()) {
            throw new NotFoundException("participation request id=" + reqId + " not found");
        }
        ParticipationRequest requestToCancel = requestOptional.get();

        if (requestToCancel.getRequesterId() != userId) {
            throw new ForbiddenException("user id=" + userId + " can not cancel not owned request id=" + reqId);
        }

        if (requestToCancel.getState().equals(RequestState.CONFIRMED)) { // если запрос уже одобрен
            Optional<Event> eventOptional = eventRepository.findById(requestToCancel.getEventId());
            if (eventOptional.isEmpty()) {
                throw new NotFoundException("event id=" + requestToCancel.getEventId() + " not found");
            }
            Event eventToRemoveOneRequest = eventOptional.get();
            eventToRemoveOneRequest.setConfirmedRequests(eventToRemoveOneRequest.getConfirmedRequests() - 1);
            eventRepository.save(eventToRemoveOneRequest);
        }
        requestToCancel.setState(RequestState.CANCELED);

        return ParticipationRequestDtoMapper.toRequestDto(participationRequestRepository.save(requestToCancel));
    }
}
