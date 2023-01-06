package ru.yandex.practicum.service.private_service.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.service.private_service.dto.ParticipationRequestDto;
import ru.yandex.practicum.service.private_service.dto.ParticipationRequestDtoMapper;
import ru.yandex.practicum.service.shared.exceptions.ForbiddenException;
import ru.yandex.practicum.service.shared.exceptions.NotFoundException;
import ru.yandex.practicum.service.shared.model.Event;
import ru.yandex.practicum.service.shared.model.ParticipationRequest;
import ru.yandex.practicum.service.shared.model.RequestState;
import ru.yandex.practicum.service.shared.storage.EventRepository;
import ru.yandex.practicum.service.shared.storage.ParticipationRequestRepository;
import ru.yandex.practicum.service.shared.storage.RequestStateRepository;
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
    private final RequestStateRepository requestStateRepository;

    public List<ParticipationRequestDto> getEventRequests(long userId, long eventId) {
        if (!userRepository.existsById(userId))
            throw new NotFoundException("user id=" + userId + " not found");

        Optional<Event> event = eventRepository.findById(eventId);
        if (event.isEmpty())
            throw new NotFoundException("event id=" + eventId + " not found");

        if (event.get().getInitiatorId() != userId)
            throw new ForbiddenException("event initiator id=" + event.get().getInitiatorId() + " and user id=" + userId);

        List<ParticipationRequest> temp = participationRequestRepository.findByEventId(eventId);
        List<ParticipationRequestDto> result = new ArrayList<>();
        for (ParticipationRequest request : temp) {
            Optional<RequestState> state = requestStateRepository.findById(request.getStatusId());
            result.add(ParticipationRequestDtoMapper.toRequestDto(request, state.get()));
        }

        return result;
    }

    public ParticipationRequestDto acceptRequest(long userId, long eventId, long reqId) {
        if (!userRepository.existsById(userId))
            throw new NotFoundException("user id=" + userId + " not found");

        Optional<Event> event = eventRepository.findById(eventId);
        if (event.isEmpty())
            throw new NotFoundException("event id=" + eventId + " not found");

        if (event.get().getInitiatorId() != userId)
            throw new ForbiddenException("event initiator id=" + event.get().getInitiatorId() + " and user id=" + userId);

        Optional<ParticipationRequest> request = participationRequestRepository.findById(reqId);
        if (request.isEmpty())
            throw new NotFoundException("participation request id=" + reqId + " not found");

        if (!event.get().isModerationRequired() || event.get().getParticipantLimit() == 0)
            throw new ForbiddenException("event id=" + eventId + " does not require moderation"); // запрос уже одобрен/одобрение не требуется

        if (event.get().getConfirmedRequests() >= event.get().getParticipantLimit())
            request.get().setStatusId(3);
        else {
            request.get().setStatusId(2);
            event.get().setConfirmedRequests(event.get().getConfirmedRequests() + 1);
            eventRepository.save(event.get());
        }

        ParticipationRequestDto result
                = ParticipationRequestDtoMapper.toRequestDto(participationRequestRepository.saveAndFlush(request.get()),
                requestStateRepository.findById(request.get().getStatusId()).get());

        if (event.get().getConfirmedRequests() == event.get().getParticipantLimit()) {
            List<ParticipationRequest> requestsToCancel
                    = participationRequestRepository.findByEventIdAndStatusId(eventId, 1);

            for (ParticipationRequest requestToCancel : requestsToCancel) {
                requestToCancel.setStatusId(3);
                participationRequestRepository.save(requestToCancel);
            }
        }
        /*
        int currentParticipants = participationRequestRepository.countByEventIdAndStatusId(eventId, 2);
        if (event.get().getParticipantLimit() <= currentParticipants)
            request.get().setStatusId(3);
        else
            request.get().setStatusId(2);

        ParticipationRequestDto result
                = ParticipationRequestDtoMapper.toRequestDto(participationRequestRepository.saveAndFlush(request.get()),
                requestStateRepository.findById(request.get().getStatusId()).get());

        currentParticipants = participationRequestRepository.countByEventIdAndStatusId(eventId, 2);
        if (event.get().getParticipantLimit() == currentParticipants) {
            List<ParticipationRequest> requestsToCancel
                    = participationRequestRepository.findByEventIdAndStatusId(eventId, 1);

            for (ParticipationRequest requestToCancel : requestsToCancel) {
                requestToCancel.setStatusId(3);
                participationRequestRepository.save(requestToCancel);
            }
        }
         */

        return result;
    }

    public ParticipationRequestDto rejectRequest(long userId, long eventId, long reqId) {
        if (!userRepository.existsById(userId))
            throw new NotFoundException("user id=" + userId + " not found");

        Optional<Event> event = eventRepository.findById(eventId);
        if (event.isEmpty())
            throw new NotFoundException("event id=" + eventId + " not found");

        if (event.get().getInitiatorId() != userId)
            throw new ForbiddenException("event initiator id=" + event.get().getInitiatorId() + " and user id=" + userId);

        Optional<ParticipationRequest> request = participationRequestRepository.findById(reqId);
        if (request.isEmpty())
            throw new NotFoundException("participation request id=" + reqId + " not found");

        //if (!event.get().isModerationRequired() || event.get().getParticipantLimit() == 0)
        //    throw new RuntimeException(); // запрос уже одобрен/одобрение не требуется; возможно, здесь это не нужно
        // т.е. можно отклонить даже принятые запросы на участие

        request.get().setStatusId(3);
        return ParticipationRequestDtoMapper.toRequestDto(participationRequestRepository.save(request.get()),
                requestStateRepository.findById(request.get().getStatusId()).get());
    }

    public List<ParticipationRequestDto> getUserRequests(long userId) {
        if (!userRepository.existsById(userId))
            throw new NotFoundException("user id=" + userId + " not found");

        List<ParticipationRequest> temp = participationRequestRepository.findByRequesterId(userId);
        List<ParticipationRequestDto> result = new ArrayList<>();
        for (ParticipationRequest request : temp) {
            Optional<RequestState> state = requestStateRepository.findById(request.getStatusId());
            result.add(ParticipationRequestDtoMapper.toRequestDto(request, state.get()));
        }

        return result;
    }

    public ParticipationRequestDto addRequest(long userId, long eventId) {
        if (!userRepository.existsById(userId))
            throw new NotFoundException("user id=" + userId + " not found");

        Optional<Event> event = eventRepository.findById(eventId);
        if (event.isEmpty())
            throw new NotFoundException("event id=" + eventId + " not found");

        if (event.get().getInitiatorId() == userId) // нельзя подать заявку на собственное событие
            throw new ForbiddenException("event initiator id=" + event.get().getInitiatorId() + " can not request to enter own event");

        if (event.get().getEventStateId() != 2)
            throw new ForbiddenException("event id=" + eventId + " already published");   // нельзя подать заявку на неопубликованное событие

        if (participationRequestRepository.countByEventIdAndRequesterId(eventId, userId) != 0)
            throw new ForbiddenException("event id=" + eventId + " already has request from user id=" + userId);   // уже есть заявка

        if (event.get().getConfirmedRequests() == event.get().getParticipantLimit())
            throw new ForbiddenException("event id=" + eventId + " already has max number of participants");

        ParticipationRequest result = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .eventId(eventId)
                .requesterId(userId)
                .build();

        if (event.get().getParticipantLimit() == 0 || !event.get().isModerationRequired())
            result.setStatusId(2);
        else
            result.setStatusId(1);

        return ParticipationRequestDtoMapper.toRequestDto(participationRequestRepository.save(result),
                requestStateRepository.findById(result.getStatusId()).get());
    }

    public ParticipationRequestDto cancelOwnRequest(long userId, long reqId) {
        if (!userRepository.existsById(userId))
            throw new NotFoundException("user id=" + userId + " not found");

        Optional<ParticipationRequest> request = participationRequestRepository.findById(reqId);
        if (request.isEmpty())
            throw new NotFoundException("participation request id=" + reqId + " not found");
        if (request.get().getRequesterId() != userId)
            throw new ForbiddenException("user id=" + userId + " can not cancel not owned request id=" + reqId);

        if (request.get().getStatusId() == 2) { // если запрос уже одобрен
            Optional<Event> event = eventRepository.findById(request.get().getEventId());
            if (event.isEmpty())
                throw new NotFoundException("event id=" + request.get().getEventId() + " not found");
            event.get().setConfirmedRequests(event.get().getConfirmedRequests() - 1);
            eventRepository.save(event.get());
        }
        request.get().setStatusId(4);

        return ParticipationRequestDtoMapper.toRequestDto(participationRequestRepository.save(request.get()),
                requestStateRepository.findById(request.get().getStatusId()).get());
    }
}
