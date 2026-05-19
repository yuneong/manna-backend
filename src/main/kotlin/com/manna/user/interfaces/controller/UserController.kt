package com.manna.user.interfaces.controller

import com.manna.user.application.facade.UserFacade
import com.manna.user.interfaces.dto.LoginRequest
import com.manna.user.interfaces.dto.SignUpRequest
import com.manna.user.interfaces.dto.TokenResponse
import com.manna.user.interfaces.dto.UserResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "User", description = "사용자 API")
@RestController
@RequestMapping("/api/v1/users")
class UserController(private val userFacade: UserFacade) {

    @Operation(summary = "회원가입")
    @ApiResponse(responseCode = "201", description = "회원가입 성공")
    @PostMapping("/sign-up")
    fun signUp(
        @Valid @RequestBody request: SignUpRequest,
    ): ResponseEntity<UserResponse> {
        val info = userFacade.signUp(request.toCommand())
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(info))
    }

    @Operation(summary = "로그인")
    @ApiResponse(responseCode = "200", description = "로그인 성공 — accessToken 반환")
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
    ): ResponseEntity<TokenResponse> {
        val info = userFacade.login(request.toCommand())
        return ResponseEntity.ok(TokenResponse.from(info))
    }

    @Operation(summary = "내 정보 조회", security = [SecurityRequirement(name = "Bearer Authentication")])
    @ApiResponse(responseCode = "200", description = "내 정보 반환")
    @GetMapping("/me")
    fun getMyInfo(
        @AuthenticationPrincipal userId: Long,
    ): ResponseEntity<UserResponse> {
        val info = userFacade.getMyInfo(userId)
        return ResponseEntity.ok(UserResponse.from(info))
    }

    @Operation(summary = "회원 탈퇴", security = [SecurityRequirement(name = "Bearer Authentication")])
    @ApiResponse(responseCode = "204", description = "탈퇴 성공 (소프트 딜리트)")
    @DeleteMapping("/me")
    fun withdraw(
        @AuthenticationPrincipal userId: Long,
    ): ResponseEntity<Unit> {
        userFacade.withdraw(userId)
        return ResponseEntity.noContent().build()
    }
}
