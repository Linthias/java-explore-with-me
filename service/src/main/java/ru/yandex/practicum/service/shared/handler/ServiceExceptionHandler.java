package ru.yandex.practicum.service.shared.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import ru.yandex.practicum.service.shared.dto.ApiError;
import ru.yandex.practicum.service.shared.exceptions.BadRequestException;
import ru.yandex.practicum.service.shared.exceptions.ConflictException;
import ru.yandex.practicum.service.shared.exceptions.ForbiddenException;
import ru.yandex.practicum.service.shared.exceptions.NotFoundException;
import ru.yandex.practicum.service.shared.model.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class ServiceExceptionHandler {
    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ApiError> handleException(Exception e) {
        log.info(e.getClass().getSimpleName() + " " + e.getMessage());
        HttpStatus httpStatus;

        if (e.getClass() == BadRequestException.class) {
            httpStatus = HttpStatus.BAD_REQUEST;
        } else if (e.getClass() == ForbiddenException.class) {
            httpStatus = HttpStatus.FORBIDDEN;
        } else if (e.getClass() == NotFoundException.class) {
            httpStatus = HttpStatus.NOT_FOUND;
        } else if (e.getClass() == ConflictException.class) {
            httpStatus = HttpStatus.CONFLICT;
        } else if (e.getClass() == HttpClientErrorException.Conflict.class){
            httpStatus = HttpStatus.CONFLICT;
        } else if (e.getClass() == MissingServletRequestParameterException.class) {
            httpStatus = HttpStatus.BAD_REQUEST;
        } else if (e.getClass() == HttpMessageNotReadableException.class) {
            httpStatus = HttpStatus.BAD_REQUEST;
        } else if (e.getClass() == DataIntegrityViolationException.class) {
            httpStatus = HttpStatus.CONFLICT;
        } else if (e.getClass() == MethodArgumentNotValidException.class) {
            httpStatus = HttpStatus.BAD_REQUEST;
        }
        else {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return new ResponseEntity<>(ApiError.builder()
                .errors(List.of(Arrays.toString(e.getStackTrace())))
                .message(e.getMessage())
                .reason(httpStatus.getReasonPhrase())
                .status(httpStatus.name())
                .timestamp(LocalDateTime.now().format(new DateTimeFormat().getFormatter()))
                .build(),
                httpStatus);
    }
}
