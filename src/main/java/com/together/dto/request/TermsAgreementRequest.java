package com.together.dto.request;

import jakarta.validation.constraints.NotBlank;

/** {@code POST /auth/terms-agreement} (FR-028) */
public record TermsAgreementRequest(
        @NotBlank(message = "약관 버전은 필수입니다.")
        String termsVersion
) {
}
