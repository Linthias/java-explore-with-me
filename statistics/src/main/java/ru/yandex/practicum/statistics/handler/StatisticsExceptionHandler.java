package ru.yandex.practicum.statistics.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.yandex.practicum.statistics.dto.ApiError;
import ru.yandex.practicum.statistics.model.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class StatisticsExceptionHandler {
    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ApiError> handleException(Exception e) {
        log.info(e.getClass().getSimpleName() + " " + e.getMessage());
        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;

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
