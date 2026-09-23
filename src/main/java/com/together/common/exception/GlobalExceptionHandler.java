package com.together.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 응답 본문은 RFC 7807 {@link ProblemDetail} 형식을 쓴다.
 * 별도 응답 래퍼는 Notion API 명세서에 정의된 바 없으므로 임의로 만들지 않는다.
 *
 * <p>{@link ResponseEntityExceptionHandler} 를 상속해 Spring MVC 표준 예외(404, 405, 415 등)가
 * 아래 catch-all 에 잡혀 500으로 뭉개지지 않게 한다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ProblemDetail> handleBusiness(BusinessException e) {
        return ResponseEntity.status(e.getStatus())
                .body(ProblemDetail.forStatusAndDetail(e.getStatus(), e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception e) {
        log.error("처리되지 않은 예외", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."));
    }

    /**
     * 검증 실패는 필드별로 쪼개서 내려준다.
     *
     * <p>프론트가 입력란을 강조해야 하는데, 한 줄로 이어 붙이면 어느 필드가 틀렸는지 다시 파싱해야 한다.
     * 그래서 {@code errors} 를 {@code {필드명: 메시지}} 맵으로 싣고, {@code detail} 에는 토스트용
     * 일반 문구만 둔다.
     *
     * <pre>
     * {"title":"Bad Request","status":400,"detail":"입력값이 올바르지 않습니다.",
     *  "instance":"/challenges",
     *  "errors":{"endDate":"종료일은 필수입니다.","nickname":"닉네임은 필수입니다."}}
     * </pre>
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            // 한 필드에 제약이 여러 개 걸리면 메시지를 이어 붙인다(키를 잃지 않도록)
            errors.merge(error.getField(), messageOf(error), (a, b) -> a + " " + b);
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "입력값이 올바르지 않습니다.");
        problem.setProperty("errors", errors);

        // 필드에 매이지 않는 객체 단위 오류는 강조할 입력란이 없으므로 detail 로 올린다
        String objectErrors = e.getBindingResult().getGlobalErrors().stream()
                .map(GlobalExceptionHandler::messageOf)
                .collect(Collectors.joining(" "));
        if (StringUtils.hasText(objectErrors)) {
            problem.setDetail(objectErrors);
        }

        return ResponseEntity.status(status).body(problem);
    }

    private static String messageOf(ObjectError error) {
        return error.getDefaultMessage() == null ? "올바르지 않은 값입니다." : error.getDefaultMessage();
    }
}
