package org.sopt.catchtable.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolationException;
import java.lang.reflect.Method;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.sopt.catchtable.global.response.BaseResponse;
import org.sopt.catchtable.global.response.ValidationError;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void businessExceptionIsConvertedToItsErrorCodeResponse() {
        ResponseEntity<BaseResponse<Void>> response = handler.handleBusinessException(
            new BusinessException(ErrorCode.VOTE_CLOSED)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo(BaseResponse.failure(ErrorCode.VOTE_CLOSED));
    }

    @Test
    void invalidRequestIsConvertedToCommonBadRequestResponseWithValidationData() {
        ResponseEntity<BaseResponse<java.util.List<ValidationError>>> response = handler.handleInvalidRequest(
            new ConstraintViolationException(Set.of()),
            request()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(BaseResponse.failure(ErrorCode.INVALID_REQUEST, java.util.List.of()));
    }

    @Test
    void missingRequiredParameterIsConvertedToBadRequestWithFieldError() {
        ResponseEntity<BaseResponse<java.util.List<ValidationError>>> response = handler.handleInvalidRequest(
            new MissingServletRequestParameterException("nickname", "String"),
            request()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(BaseResponse.failure(
            ErrorCode.INVALID_REQUEST,
            java.util.List.of(new ValidationError("nickname", "필수 요청 파라미터입니다."))
        ));
    }

    @Test
    void missingUserIdHeaderIsConvertedToUnauthorized() {
        ResponseEntity<?> response = handler.handleMissingRequestHeader(
            new MissingRequestHeaderException("X-User-Id", headerParameter()),
            request()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo(BaseResponse.failure(ErrorCode.UNAUTHORIZED));
    }

    @Test
    void missingOtherHeaderIsConvertedToBadRequestWithHeaderName() {
        ResponseEntity<?> response = handler.handleMissingRequestHeader(
            new MissingRequestHeaderException("Idempotency-Key", headerParameter()),
            request()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(BaseResponse.failure(
            ErrorCode.INVALID_REQUEST,
            java.util.List.of(new ValidationError("Idempotency-Key", "필수 요청 헤더입니다."))
        ));
    }

    @Test
    void unexpectedExceptionDoesNotExposeItsMessage() {
        ResponseEntity<BaseResponse<Void>> response = handler.handleUnexpectedException(
            new RuntimeException("sensitive detail"),
            request()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo(BaseResponse.failure(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    private MethodParameter headerParameter() {
        try {
            Method method = HeaderFixture.class.getDeclaredMethod("handle", String.class);
            return new MethodParameter(method, 0);
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static class HeaderFixture {
        @SuppressWarnings("unused")
        void handle(String header) {
        }
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/votes");
        return request;
    }
}
