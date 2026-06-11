package com.manna.settlement.interfaces.controller

import com.manna.settlement.application.facade.SettlementFacade
import com.manna.settlement.interfaces.dto.CreateSettlementRequest
import com.manna.settlement.interfaces.dto.SettlementListResponse
import com.manna.settlement.interfaces.dto.SettlementResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Settlement", description = "정산 API")
@RestController
@RequestMapping("/api/v1/meetings/{meetingId}/settlements")
class SettlementController(private val settlementFacade: SettlementFacade) {

    @Operation(summary = "정산 목록 조회", security = [SecurityRequirement(name = "Bearer Authentication")])
    @ApiResponse(responseCode = "200", description = "약속의 정산 전체 목록 반환")
    @GetMapping
    fun getSettlements(
        @AuthenticationPrincipal userId: Long,
        @PathVariable meetingId: Long,
    ): ResponseEntity<SettlementListResponse> {
        val infos = settlementFacade.getSettlements(meetingId, userId)
        return ResponseEntity.ok(SettlementListResponse.from(infos))
    }

    @Operation(summary = "정산 단건 조회", security = [SecurityRequirement(name = "Bearer Authentication")])
    @ApiResponse(responseCode = "200", description = "정산 상세 반환")
    @GetMapping("/{settlementId}")
    fun getSettlement(
        @AuthenticationPrincipal userId: Long,
        @PathVariable meetingId: Long,
        @PathVariable settlementId: Long,
    ): ResponseEntity<SettlementResponse> {
        val info = settlementFacade.getSettlement(meetingId, settlementId, userId)
        return ResponseEntity.ok(SettlementResponse.from(info))
    }

    @Operation(summary = "정산 생성", security = [SecurityRequirement(name = "Bearer Authentication")])
    @ApiResponse(responseCode = "201", description = "정산 생성 성공 — 최초 생성 시 meeting.status SETTLING 전이")
    @PostMapping
    fun createSettlement(
        @AuthenticationPrincipal userId: Long,
        @PathVariable meetingId: Long,
        @Valid @RequestBody request: CreateSettlementRequest,
    ): ResponseEntity<SettlementResponse> {
        val info = settlementFacade.createSettlement(request.toCommand(meetingId, userId))
        return ResponseEntity.status(HttpStatus.CREATED).body(SettlementResponse.from(info))
    }

    @Operation(summary = "납부 완료 처리", security = [SecurityRequirement(name = "Bearer Authentication")])
    @ApiResponse(responseCode = "200", description = "본인 납부 완료 처리")
    @PatchMapping("/{settlementId}/pay")
    fun markPaid(
        @AuthenticationPrincipal userId: Long,
        @PathVariable meetingId: Long,
        @PathVariable settlementId: Long,
    ): ResponseEntity<Unit> {
        settlementFacade.markPaid(settlementId, userId)
        return ResponseEntity.ok().build()
    }

    @Operation(summary = "정산 완료 처리", security = [SecurityRequirement(name = "Bearer Authentication")])
    @ApiResponse(responseCode = "200", description = "수금자가 전원 납부 확인 후 정산 완료")
    @PatchMapping("/{settlementId}/complete")
    fun complete(
        @AuthenticationPrincipal userId: Long,
        @PathVariable meetingId: Long,
        @PathVariable settlementId: Long,
    ): ResponseEntity<Unit> {
        settlementFacade.complete(settlementId, userId)
        return ResponseEntity.ok().build()
    }
}
