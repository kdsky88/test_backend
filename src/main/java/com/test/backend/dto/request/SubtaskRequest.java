package com.test.backend.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 생성/수정 요청의 하위 항목 하나. */
@Getter
@Setter
@NoArgsConstructor
public class SubtaskRequest {
    private String title;
    private boolean done;
}
