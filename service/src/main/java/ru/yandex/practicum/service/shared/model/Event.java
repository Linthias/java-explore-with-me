package ru.yandex.practicum.service.shared.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false, length = 2000)
    private String annotation;

    @Column(name = "category_id", nullable = false)
    private long categoryId;

    @Column(name = "confirmed_requests", nullable = false)
    private long confirmedRequests;

    @Column(name = "created_on", nullable = false)
    private LocalDateTime createdOn;

    @Column(nullable = false, length = 7000)
    private String description;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Column(name = "initiator_id", nullable = false)
    private long initiatorId;

    @Column(nullable = false)
    private float latitude;

    @Column(nullable = false)
    private float longtitude;

    @Column(name = "is_paid", nullable = false)
    private boolean isPaid;

    @Column(name = "participant_limit", nullable = false)
    private int participantLimit;

    @Column(name = "published_on")
    private LocalDateTime publishedOn;

    @Column(name = "is_moderation_required", nullable = false)
    private boolean isModerationRequired;

    @Column(name = "event_state_id", nullable = false)
    private int eventStateId;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false)
    private long views;
}
