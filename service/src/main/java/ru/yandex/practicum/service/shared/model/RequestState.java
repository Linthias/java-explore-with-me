package ru.yandex.practicum.service.shared.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "request_states")
public class RequestState {
    @Id
    private int id;

    @Column(name = "state_name", nullable = false, length = 20)
    private String name;
}
