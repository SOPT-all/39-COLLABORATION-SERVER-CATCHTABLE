package org.sopt.catchtable.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "잘못된 요청입니다."),
    INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "INVALID_NICKNAME", "닉네임은 공백만으로 구성할 수 없으며, 1~15자여야 합니다."),
    INVALID_VOTE_CANDIDATES(HttpStatus.BAD_REQUEST, "INVALID_VOTE_CANDIDATES", "서로 다른 매장 2개를 후보로 선택해야 합니다."),
    INVALID_VOTE_CANDIDATE(HttpStatus.BAD_REQUEST, "INVALID_VOTE_CANDIDATE", "해당 투표의 후보가 아닙니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "사용자를 확인할 수 없습니다."),
    VOTE_CLOSE_FORBIDDEN(HttpStatus.FORBIDDEN, "VOTE_CLOSE_FORBIDDEN", "투표 생성자만 투표를 종료할 수 있습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
    STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "STORE_NOT_FOUND", "매장을 찾을 수 없습니다."),
    VOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "VOTE_NOT_FOUND", "투표를 찾을 수 없습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "요청한 경로를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "지원하지 않는 HTTP 메서드입니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "지원하지 않는 Content-Type입니다."),
    ALREADY_VOTED(HttpStatus.CONFLICT, "ALREADY_VOTED", "이미 해당 투표에 참여했습니다."),
    VOTE_CLOSED(HttpStatus.CONFLICT, "VOTE_CLOSED", "종료된 투표에는 참여할 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
