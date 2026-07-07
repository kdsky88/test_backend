package com.test.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 할 일의 하위 체크리스트 항목. Todo에 @ElementCollection으로 내장. */
@Embeddable
@Getter
@NoArgsConstructor
public class Subtask {

    @Column(name = "title", length = 100, nullable = false)
    private String title;

    @Column(name = "done", nullable = false)
    private boolean done;

    public Subtask(String title, boolean done) {
        this.title = title;
        this.done = done;
    }
}
