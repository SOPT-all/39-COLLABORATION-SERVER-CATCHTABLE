package org.sopt.catchtable.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.sopt.catchtable.global.response.BaseResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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
    void invalidRequestIsConvertedToCommonBadRequestResponse() {
        ResponseEntity<BaseResponse<Void>> response = handler.handleInvalidRequest(
            new ConstraintViolationException(Set.of())
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(BaseResponse.failure(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void unexpectedExceptionDoesNotExposeItsMessage() {
        ResponseEntity<BaseResponse<Void>> response = handler.handleUnexpectedException(
            new RuntimeException("sensitive detail")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo(BaseResponse.failure(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
