package org.sopt.catchtable.global.response;

public record ValidationError(
    String field,
    String message
) {
}
