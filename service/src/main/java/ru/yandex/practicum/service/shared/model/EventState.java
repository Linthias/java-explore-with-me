package ru.yandex.practicum.service.shared.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "event_states")
public class EventState {
    @Id
    private int id;

    @Column(name = "state_name", nullable = false, length = 20)
    private String name;
}
