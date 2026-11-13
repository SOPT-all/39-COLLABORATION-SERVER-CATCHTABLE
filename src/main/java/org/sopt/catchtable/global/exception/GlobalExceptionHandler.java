package org.sopt.catchtable.global.exception;

import jakarta.validation.ConstraintViolationException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sopt.catchtable.global.response.BaseResponse;
import org.sopt.catchtable.global.response.ValidationError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String USER_ID_HEADER = "X-User-Id";

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<BaseResponse<Void>> handleBusinessException(BusinessException exception) {
        return failure(exception.getErrorCode());
    }

    // 사용자 식별 헤더(X-User-Id)가 없으면 401, 그 외 필수 헤더가 없으면 400
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<?> handleMissingRequestHeader(
        MissingRequestHeaderException exception,
        HttpServletRequest request
    ) {
        log.warn("Missing request header '{}': {} {}",
            exception.getHeaderName(), request.getMethod(), request.getRequestURI());

        if (USER_ID_HEADER.equalsIgnoreCase(exception.getHeaderName())) {
            return failure(ErrorCode.UNAUTHORIZED);
        }

        return failure(ErrorCode.INVALID_REQUEST, List.of(
            new ValidationError(exception.getHeaderName(), "필수 요청 헤더입니다.")
        ));
    }

    // X-User-Id가 숫자가 아니면 헤더가 없을 때와 같이 사용자를 식별할 수 없으므로 401
    // 그 외 경로 변수·쿼리 파라미터 형식 오류는 400
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> handleTypeMismatch(
        MethodArgumentTypeMismatchException exception,
        HttpServletRequest request
    ) {
        if (isUserIdHeader(exception)) {
            log.warn("Invalid user id header: {} {}", request.getMethod(), request.getRequestURI());
            return failure(ErrorCode.UNAUTHORIZED);
        }

        return handleInvalidRequest(exception, request);
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        BindException.class,
        ConstraintViolationException.class,
        HttpMessageNotReadableException.class,
        MissingServletRequestParameterException.class
    })
    public ResponseEntity<BaseResponse<List<ValidationError>>> handleInvalidRequest(
        Exception exception,
        HttpServletRequest request
    ) {
        log.warn("Client request validation failed: {} {}", request.getMethod(), request.getRequestURI());
        return failure(ErrorCode.INVALID_REQUEST, extractValidationErrors(exception));
    }

    // 클라이언트의 잘못된 경로·메서드·Content-Type 요청
    // 서버 오류가 아니므로 500 대신 알맞은 4xx로 응답하고, 스택트레이스 없이 warn으로만 남긴다
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<BaseResponse<Void>> handleNoResourceFound(
        NoResourceFoundException exception,
        HttpServletRequest request
    ) {
        log.warn("Resource not found: {} {}", request.getMethod(), request.getRequestURI());
        return failure(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<BaseResponse<Void>> handleMethodNotSupported(
        HttpRequestMethodNotSupportedException exception,
        HttpServletRequest request
    ) {
        log.warn("Method not allowed: {} {}", request.getMethod(), request.getRequestURI());
        return failure(ErrorCode.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<BaseResponse<Void>> handleMediaTypeNotSupported(
        HttpMediaTypeNotSupportedException exception,
        HttpServletRequest request
    ) {
        log.warn("Unsupported media type '{}': {} {}",
            exception.getContentType(), request.getMethod(), request.getRequestURI());
        return failure(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<?> handleMethodValidationException(
        HandlerMethodValidationException exception,
        HttpServletRequest request
    ) {
        if (exception.isForReturnValue()) {
            log.error("Unexpected server error: {} {}", request.getMethod(), request.getRequestURI(), exception);
            return internalServerError();
        }

        log.warn("Client request validation failed: {} {}", request.getMethod(), request.getRequestURI());
        BaseResponse<?> response = BaseResponse.failure(
            ErrorCode.INVALID_REQUEST,
            extractValidationErrors(exception)
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleUnexpectedException(
        Exception exception,
        HttpServletRequest request
    ) {
        // 원인 추적을 위해 스택트레이스는 서버 로그에만 남기고, 응답에는 공통 메시지만 내린다
        log.error("Unexpected server error: {} {}", request.getMethod(), request.getRequestURI(), exception);
        return internalServerError();
    }

    private ResponseEntity<BaseResponse<Void>> failure(ErrorCode errorCode) {
        return ResponseEntity
            .status(errorCode.getHttpStatus())
            .body(BaseResponse.failure(errorCode));
    }

    private <T> ResponseEntity<BaseResponse<T>> failure(ErrorCode errorCode, T data) {
        return ResponseEntity
            .status(errorCode.getHttpStatus())
            .body(BaseResponse.failure(errorCode, data));
    }

    private List<ValidationError> extractValidationErrors(Exception exception) {
        if (exception instanceof BindException bindException) {
            return bindException.getBindingResult().getFieldErrors().stream()
                .map(this::toValidationError)
                .toList();
        }

        if (exception instanceof ConstraintViolationException constraintViolationException) {
            return constraintViolationException.getConstraintViolations().stream()
                .map(violation -> new ValidationError(
                    violation.getPropertyPath().toString(),
                    violation.getMessage()
                ))
                .toList();
        }

        if (exception instanceof MethodArgumentTypeMismatchException typeMismatchException) {
            return List.of(new ValidationError(
                typeMismatchException.getName(),
                "요청 값의 형식이 올바르지 않습니다."
            ));
        }

        if (exception instanceof MissingServletRequestParameterException missingParameterException) {
            return List.of(new ValidationError(
                missingParameterException.getParameterName(),
                "필수 요청 파라미터입니다."
            ));
        }

        if (exception instanceof HandlerMethodValidationException methodValidationException) {
            return methodValidationException.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                    .map(error -> new ValidationError(
                        result.getMethodParameter().getParameterName(),
                        error.getDefaultMessage()
                    )))
                .toList();
        }

        return List.of();
    }

    // 같은 이름의 경로 변수·쿼리 파라미터와 구분하기 위해 @RequestHeader로 받은 값인지까지 확인
    private boolean isUserIdHeader(MethodArgumentTypeMismatchException exception) {
        return USER_ID_HEADER.equalsIgnoreCase(exception.getName())
            && exception.getParameter().hasParameterAnnotation(RequestHeader.class);
    }

    private ValidationError toValidationError(FieldError fieldError) {
        return new ValidationError(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private ResponseEntity<BaseResponse<Void>> internalServerError() {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(BaseResponse.failure(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
