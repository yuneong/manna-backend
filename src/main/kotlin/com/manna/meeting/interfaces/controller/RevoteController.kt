package com.manna.meeting.interfaces.controller

import com.manna.meeting.application.facade.RevoteFacade
import com.manna.meeting.interfaces.dto.ConfirmRevoteRequest
import com.manna.meeting.interfaces.dto.CreateRevoteRequest
import com.manna.meeting.interfaces.dto.RevoteResponse
import com.manna.meeting.interfaces.dto.VoteRevoteRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Revote", description = "재투표 API")
@RestController
@RequestMapping("/api/v1/meetings/{meetingId}/revote")
class RevoteController(private val revoteFacade: RevoteFacade) {

    @Operation(summary = "재투표 생성 (방장 전용)", security = [SecurityRequirement(name = "Bearer Authentication")])
    @PostMapping
    fun createRevote(
        @AuthenticationPrincipal userId: Long,
        @PathVariable meetingId: Long,
        @Valid @RequestBody request: CreateRevoteRequest,
    ): ResponseEntity<RevoteResponse> {
        val info = revoteFacade.createRevote(request.toCommand(meetingId, userId))
        return ResponseEntity.status(HttpStatus.CREATED).body(RevoteResponse.from(info))
    }

    @Operation(summary = "재투표 참여", security = [SecurityRequirement(name = "Bearer Authentication")])
    @PostMapping("/vote")
    fun vote(
        @AuthenticationPrincipal userId: Long,
        @PathVariable meetingId: Long,
        @Valid @RequestBody request: VoteRevoteRequest,
    ): ResponseEntity<Unit> {
        revoteFacade.vote(request.toCommand(meetingId, userId))
        return ResponseEntity.ok().build()
    }

    @Operation(summary = "재투표 현황 조회", security = [SecurityRequirement(name = "Bearer Authentication")])
    @GetMapping
    fun getRevoteStatus(
        @AuthenticationPrincipal userId: Long,
        @PathVariable meetingId: Long,
    ): ResponseEntity<RevoteResponse> {
        val info = revoteFacade.getRevoteStatus(meetingId, userId)
        return ResponseEntity.ok(RevoteResponse.from(info))
    }

    @Operation(summary = "재동률 방장 확정 (방장 전용)", security = [SecurityRequirement(name = "Bearer Authentication")])
    @PostMapping("/confirm")
    fun confirmRevote(
        @AuthenticationPrincipal userId: Long,
        @PathVariable meetingId: Long,
        @Valid @RequestBody request: ConfirmRevoteRequest,
    ): ResponseEntity<Unit> {
        revoteFacade.confirmRevote(request.toCommand(meetingId, userId))
        return ResponseEntity.ok().build()
    }
}
