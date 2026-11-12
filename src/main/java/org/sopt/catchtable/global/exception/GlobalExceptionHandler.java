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
import org.springframework.context.MessageSourceResolvable;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
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

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        BindException.class,
        ConstraintViolationException.class,
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class
    })
    public ResponseEntity<BaseResponse<List<ValidationError>>> handleInvalidRequest(
        Exception exception,
        HttpServletRequest request
    ) {
        log.warn("Client request validation failed: {} {}", request.getMethod(), request.getRequestURI());
        return failure(ErrorCode.INVALID_REQUEST, extractValidationErrors(exception));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<?> handleMethodValidationException(
        HandlerMethodValidationException exception,
        HttpServletRequest request
    ) {
        if (exception.isForReturnValue()) {
            log.error("Unexpected server error: {} {}", request.getMethod(), request.getRequestURI());
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
        log.error("Unexpected server error: {} {}", request.getMethod(), request.getRequestURI());
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

    private ValidationError toValidationError(FieldError fieldError) {
        return new ValidationError(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private ResponseEntity<BaseResponse<Void>> internalServerError() {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(BaseResponse.failure(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
