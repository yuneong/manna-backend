package com.manna.place.interfaces.controller

import com.manna.place.application.facade.PlaceFacade
import com.manna.place.interfaces.dto.CreatePlaceRequest
import com.manna.place.interfaces.dto.PlaceResponse
import com.manna.place.interfaces.dto.PlacesResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
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

@Tag(name = "Place", description = "장소 API")
@RestController
@RequestMapping("/api/v1/meetings/{meetingId}/places")
class PlaceController(private val placeFacade: PlaceFacade) {

    @Operation(summary = "장소 목록 조회", security = [SecurityRequirement(name = "Bearer Authentication")])
    @ApiResponse(responseCode = "200", description = "득표 수 내림차순 정렬")
    @GetMapping
    fun getPlaces(
        @AuthenticationPrincipal userId: Long,
        @PathVariable meetingId: Long,
    ): ResponseEntity<PlacesResponse> {
        val info = placeFacade.getPlaces(meetingId, userId)
        return ResponseEntity.ok(PlacesResponse.from(info))
    }

    @Operation(summary = "장소 제안 (참여자 전용)", security = [SecurityRequirement(name = "Bearer Authentication")])
    @ApiResponse(responseCode = "201", description = "장소 제안 성공 — meeting.status = CONFIRMED 필요")
    @PostMapping
    fun propose(
        @AuthenticationPrincipal userId: Long,
        @PathVariable meetingId: Long,
        @Valid @RequestBody request: CreatePlaceRequest,
    ): ResponseEntity<PlaceResponse> {
        val info = placeFacade.propose(request.toCommand(meetingId, userId))
        return ResponseEntity.status(HttpStatus.CREATED).body(PlaceResponse.from(info))
    }

    @Operation(summary = "장소 투표/취소 토글 (참여자 전용)", security = [SecurityRequirement(name = "Bearer Authentication")])
    @ApiResponse(responseCode = "200", description = "이미 투표했으면 취소, 없으면 추가")
    @PostMapping("/{placeId}/vote")
    fun toggleVote(
        @AuthenticationPrincipal userId: Long,
        @PathVariable meetingId: Long,
        @PathVariable placeId: Long,
    ): ResponseEntity<Unit> {
        placeFacade.toggleVote(meetingId, placeId, userId)
        return ResponseEntity.ok().build()
    }
}
