package com.test.backend.dto.request;

import com.test.backend.domain.entity.Post.PostStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdatePostStatusRequest {

    @NotNull(message = "상태 값은 필수입니다.")
    private PostStatus status;
}
