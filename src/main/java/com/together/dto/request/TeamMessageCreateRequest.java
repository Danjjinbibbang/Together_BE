package com.together.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * {@code POST /challenges/{challengeId}/messages} (FR-044)
 *
 * <p>200자 상한은 DB 컬럼과 맞춰 둔 값이다. 한 줄 응원이라는 성격에서 온 제한이기도 하다.
 */
public record TeamMessageCreateRequest(
        @NotBlank(message = "메시지를 입력해주세요.")
        @Size(max = 200, message = "메시지는 200자를 넘을 수 없습니다.")
        String content
) {
}
