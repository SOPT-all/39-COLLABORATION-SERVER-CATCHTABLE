package org.sopt.catchtable.global.response;

public enum SuccessCode {
    SUCCESS("SUCCESS", "요청에 성공했습니다."),
    VOTE_CREATED("SUCCESS", "투표가 생성되었습니다."),
    VOTE_CLOSED_SUCCESS("SUCCESS", "투표가 종료되었습니다.");

    private final String code;
    private final String message;

    SuccessCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
