package com.together.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * {@code POST /challenges/{challengeId}/missions} (방장 전용).
 *
 * <p>보상은 고정값이 아니라 범위다. 실제 지급액은 오늘의 미션 배정 시점에 이 범위에서 한 번만
 * 뽑힌다 — 같은 날 같은 챌린지의 멤버는 모두 같은 금액을 받는다.
 *
 * <p>{@code rewardMin <= rewardMax} 검사는 값 두 개를 함께 봐야 해서 서비스에서 한다.
 */
public record MissionCreateRequest(
        @NotBlank(message = "미션 제목은 필수입니다.")
        @Size(max = 200, message = "미션 제목은 200자 이하여야 합니다.")
        String title,

        @NotBlank(message = "제출 형식은 필수입니다.")
        @Pattern(regexp = "TEXT|PHOTO", message = "제출 형식은 TEXT 또는 PHOTO 여야 합니다.")
        String submitType,

        @NotNull(message = "보상 하한은 필수입니다.")
        @Min(value = 100, message = "보상 하한은 100원 이상이어야 합니다.")
        @Max(value = 10000, message = "보상 하한은 10,000원 이하여야 합니다.")
        Integer rewardMin,

        @NotNull(message = "보상 상한은 필수입니다.")
        @Min(value = 100, message = "보상 상한은 100원 이상이어야 합니다.")
        @Max(value = 10000, message = "보상 상한은 10,000원 이하여야 합니다.")
        Integer rewardMax,

        /** PHOTO 미션이면 null 을 보내도 된다. */
        @Min(value = 1, message = "최소 글자수는 1자 이상이어야 합니다.")
        Integer minTextLength,

        /** AI 제안을 골라 등록하는 경로면 AI, 방장이 직접 쓴 것이면 생략(OWNER 로 저장). */
        @Pattern(regexp = "OWNER|AI", message = "생성 주체는 OWNER 또는 AI 여야 합니다.")
        String createdByType
) {
}
