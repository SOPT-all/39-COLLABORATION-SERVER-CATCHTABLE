package org.sopt.catchtable.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.sopt.catchtable.global.response.BaseResponse;
import org.sopt.catchtable.global.response.ValidationError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
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
    void unexpectedExceptionDoesNotExposeItsMessage() {
        ResponseEntity<BaseResponse<Void>> response = handler.handleUnexpectedException(
            new RuntimeException("sensitive detail"),
            request()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo(BaseResponse.failure(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/votes");
        return request;
    }
}
