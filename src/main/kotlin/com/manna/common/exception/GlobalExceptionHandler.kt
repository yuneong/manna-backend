package com.manna.common.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MannaException::class)
    fun handleMannaException(e: MannaException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(e.errorCode.status)
            .body(ErrorResponse.from(e.errorCode))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message = e.bindingResult.fieldErrors.firstOrNull()?.defaultMessage ?: "잘못된 요청입니다"
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(status = 400, message = message))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse(status = 500, message = "서버 내부 오류가 발생했습니다"))
}

data class ErrorResponse(
    val status: Int,
    val message: String,
) {
    companion object {
        fun from(errorCode: ErrorCode) = ErrorResponse(
            status = errorCode.status.value(),
            message = errorCode.message,
        )
    }
}
