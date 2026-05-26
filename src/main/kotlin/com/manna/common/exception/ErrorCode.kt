package com.manna.common.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val status: HttpStatus,
    val message: String,
) {
    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),
    SOCIAL_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "소셜 로그인에 실패했습니다"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다"),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다"),

    // Meeting
    MEETING_NOT_FOUND(HttpStatus.NOT_FOUND, "약속방을 찾을 수 없습니다"),
    NOT_MEETING_HOST(HttpStatus.FORBIDDEN, "약속방 방장만 가능한 작업입니다"),
    NOT_MEETING_PARTICIPANT(HttpStatus.FORBIDDEN, "약속방 참여자만 가능한 작업입니다"),
    ALREADY_JOINED(HttpStatus.CONFLICT, "이미 참여한 약속방입니다"),
    MEETING_NOT_OPEN(HttpStatus.BAD_REQUEST, "진행 중인 약속방이 아닙니다"),
    MEETING_NOT_CONFIRMED(HttpStatus.BAD_REQUEST, "확정된 약속방이 아닙니다"),
    MEETING_ALREADY_CONFIRMED(HttpStatus.BAD_REQUEST, "이미 확정된 약속방입니다"),
    DATE_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "약속방 날짜 범위를 벗어난 날짜입니다"),

    // Place
    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "장소를 찾을 수 없습니다"),

    // Revote
    REVOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "진행 중인 재투표가 없습니다"),
    REVOTE_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "이미 진행 중인 재투표가 있습니다"),
    REVOTE_ALREADY_VOTED(HttpStatus.CONFLICT, "이미 투표하였습니다"),
    REVOTE_INVALID_CANDIDATE_DATE(HttpStatus.BAD_REQUEST, "유효하지 않은 후보 날짜입니다"),
    REVOTE_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "아직 전원 투표가 완료되지 않았습니다"),
    REVOTE_IN_PROGRESS(HttpStatus.BAD_REQUEST, "재투표가 진행 중입니다"),
}
