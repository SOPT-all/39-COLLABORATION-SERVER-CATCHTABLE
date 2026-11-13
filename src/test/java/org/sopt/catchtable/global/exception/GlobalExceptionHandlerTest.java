package org.sopt.catchtable.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolationException;
import java.lang.reflect.Method;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.catchtable.global.response.BaseResponse;
import org.sopt.catchtable.global.response.ValidationError;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
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

    @DisplayName("사용자 식별 헤더가 없으면 사용자를 확인할 수 없다는 401 응답으로 바뀐다")
    @Test
    void missingUserIdHeaderIsConvertedToUnauthorized() {
        // given
        MissingRequestHeaderException exception = new MissingRequestHeaderException("X-User-Id", headerParameter());

        // when
        ResponseEntity<?> response = handler.handleMissingRequestHeader(exception, request());

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo(BaseResponse.failure(ErrorCode.UNAUTHORIZED));
    }

    @DisplayName("사용자 식별과 무관한 필수 헤더가 없으면, 빠진 헤더 이름을 담은 잘못된 요청(400)으로 바뀐다")
    @Test
    void missingOtherHeaderIsConvertedToBadRequestWithHeaderName() {
        // given
        MissingRequestHeaderException exception = new MissingRequestHeaderException("Idempotency-Key", headerParameter());

        // when
        ResponseEntity<?> response = handler.handleMissingRequestHeader(exception, request());

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(BaseResponse.failure(
            ErrorCode.INVALID_REQUEST,
            java.util.List.of(new ValidationError("Idempotency-Key", "필수 요청 헤더입니다."))
        ));
    }

    @DisplayName("사용자 식별 헤더의 형식이 틀리면, 헤더가 없을 때와 같이 401 응답으로 바뀐다")
    @Test
    void nonNumericUserIdHeaderIsConvertedToUnauthorized() {
        // given
        MethodArgumentTypeMismatchException exception = typeMismatch("X-User-Id", "userIdHeader");

        // when
        ResponseEntity<?> response = handler.handleTypeMismatch(exception, request());

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo(BaseResponse.failure(ErrorCode.UNAUTHORIZED));
    }

    @DisplayName("경로 변수처럼 헤더가 아닌 값의 형식이 틀리면, 잘못된 요청(400)으로 바뀐다")
    @Test
    void otherTypeMismatchIsConvertedToBadRequest() {
        // given
        MethodArgumentTypeMismatchException exception = typeMismatch("voteId", "voteIdPath");

        // when
        ResponseEntity<?> response = handler.handleTypeMismatch(exception, request());

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(BaseResponse.failure(
            ErrorCode.INVALID_REQUEST,
            java.util.List.of(new ValidationError("voteId", "요청 값의 형식이 올바르지 않습니다."))
        ));
    }

    @DisplayName("경로 변수가 공백이라 값이 비게 되면, 서버 오류가 아니라 어느 값이 잘못됐는지 담은 잘못된 요청(400)으로 바뀐다")
    @Test
    void blankPathVariableIsConvertedToBadRequest() {
        // given
        MissingPathVariableException exception =
            new MissingPathVariableException("voteId", voteIdPathParameter(), true);

        // when
        ResponseEntity<?> response = handler.handleMissingPathVariable(exception, request());

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(BaseResponse.failure(
            ErrorCode.INVALID_REQUEST,
            java.util.List.of(new ValidationError("voteId", "요청 값의 형식이 올바르지 않습니다."))
        ));
    }

    @DisplayName("경로 변수 이름이 URL과 맞지 않아 처음부터 값이 없으면, 서버 코드 문제이므로 500으로 바뀐다")
    @Test
    void pathVariableMissingBeforeConversionIsConvertedToServerError() {
        // given
        MissingPathVariableException exception =
            new MissingPathVariableException("voteId", voteIdPathParameter(), false);

        // when
        ResponseEntity<?> response = handler.handleMissingPathVariable(exception, request());

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo(BaseResponse.failure(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    @DisplayName("존재하지 않는 경로로 요청하면 서버 오류가 아니라 경로를 찾을 수 없다는 404로 바뀐다")
    @Test
    void noResourceFoundIsConvertedToNotFound() {
        // given
        NoResourceFoundException exception =
            new NoResourceFoundException(HttpMethod.POST, "/api/v1/votez", "api/v1/votez");

        // when
        ResponseEntity<BaseResponse<Void>> response = handler.handleNoResourceFound(exception, request());

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo(BaseResponse.failure(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @DisplayName("지원하지 않는 HTTP 메서드로 요청하면 서버 오류가 아니라 405로 바뀐다")
    @Test
    void methodNotSupportedIsConvertedToMethodNotAllowed() {
        // given
        HttpRequestMethodNotSupportedException exception = new HttpRequestMethodNotSupportedException("GET");

        // when
        ResponseEntity<BaseResponse<Void>> response = handler.handleMethodNotSupported(exception, request());

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).isEqualTo(BaseResponse.failure(ErrorCode.METHOD_NOT_ALLOWED));
    }

    @DisplayName("지원하지 않는 Content-Type으로 요청하면 서버 오류가 아니라 415로 바뀐다")
    @Test
    void mediaTypeNotSupportedIsConvertedToUnsupportedMediaType() {
        // given
        HttpMediaTypeNotSupportedException exception = new HttpMediaTypeNotSupportedException(
            MediaType.TEXT_PLAIN, java.util.List.of(MediaType.APPLICATION_JSON)
        );

        // when
        ResponseEntity<BaseResponse<Void>> response = handler.handleMediaTypeNotSupported(exception, request());

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(response.getBody()).isEqualTo(BaseResponse.failure(ErrorCode.UNSUPPORTED_MEDIA_TYPE));
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

    private MethodParameter voteIdPathParameter() {
        try {
            Method method = HeaderFixture.class.getDeclaredMethod("voteIdPath", Long.class);
            return new MethodParameter(method, 0);
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private MethodArgumentTypeMismatchException typeMismatch(String name, String fixtureMethod) {
        try {
            Method method = HeaderFixture.class.getDeclaredMethod(fixtureMethod, Long.class);
            return new MethodArgumentTypeMismatchException(
                "abc", Long.class, name, new MethodParameter(method, 0), new NumberFormatException()
            );
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @SuppressWarnings("unused")
    private static class HeaderFixture {
        void handle(String header) {
        }

        void userIdHeader(@RequestHeader("X-User-Id") Long userId) {
        }

        void voteIdPath(@PathVariable Long voteId) {
        }
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/votes");
        return request;
    }
}
