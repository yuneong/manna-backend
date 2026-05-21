package com.manna.meeting.interfaces.controller

import com.manna.meeting.application.command.JoinMeetingCommand
import com.manna.meeting.application.facade.MeetingFacade
import com.manna.meeting.interfaces.dto.ConfirmDateRequest
import com.manna.meeting.interfaces.dto.CreateMeetingRequest
import com.manna.meeting.interfaces.dto.HeatmapResponse
import com.manna.meeting.interfaces.dto.MeetingResponse
import com.manna.meeting.interfaces.dto.MyScheduleResponse
import com.manna.meeting.interfaces.dto.UpdateScheduleRequest
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
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val SECURITY = arrayOf(SecurityRequirement(name = "Bearer Authentication"))

@Tag(name = "Meeting", description = "약속방 API")
@RestController
@RequestMapping("/api/v1/meetings")
class MeetingController(private val meetingFacade: MeetingFacade) {

    @Operation(summary = "약속방 생성", security = [SecurityRequirement(name = "Bearer Authentication")])
    @ApiResponse(responseCode = "201", description = "약속방 생성 성공 — 방장이 자동으로 참여자에 추가됨")
    @PostMapping
    fun createMeeting(
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: CreateMeetingRequest,
    ): ResponseEntity<MeetingResponse> {
        val info = meetingFacade.createMeeting(request.toCommand(userId))
        return ResponseEntity.status(HttpStatus.CREATED).body(MeetingResponse.from(info))
    }

    @Operation(summary = "약속방 상세 조회", security = [SecurityRequirement(name = "Bearer Authentication")])
    @GetMapping("/{meetingId}")
    fun getMeeting(
        @AuthenticationPrincipal userId: Long,
        @PathVariable meetingId: Long,
    ): ResponseEntity<MeetingResponse> {
        val info = meetingFacade.getMeeting(meetingId, userId)
        return ResponseEntity.ok(MeetingResponse.from(info))
    }

    @Operation(summary = "내 약속방 목록", security = [SecurityRequirement(name = "Bearer Authentication")])
    @GetMapping("/my")
    fun getMyMeetings(
        @AuthenticationPrincipal userId: Long,
    ): ResponseEntity<List<MeetingResponse>> {
        val infos = meetingFacade.getMyMeetings(userId)
        return ResponseEntity.ok(infos.map { MeetingResponse.from(it) })
    }

    @Operation(summary = "약속방 참여", security = [SecurityRequirement(name = "Bearer Authentication")])
    @ApiResponse(responseCode = "200", description = "참여 성공")
    @PostMapping("/{meetingId}/join")
    fun joinMeeting(
        @AuthenticationPrincipal userId: Long,
        @PathVariable meetingId: Long,
    ): ResponseEntity<Unit> {
        meetingFacade.joinMeeting(JoinMeetingCommand(meetingId, userId))
        return ResponseEntity.ok().build()
    }

    @Operation(summary = "약속 날짜 등록", security = [SecurityRequirement(name = "Bearer Authentication")])
    @ApiResponse(responseCode = "200", description = "기존 약속 날짜를 모두 교체(replace)")
    @PutMapping("/{meetingId}/schedules")
    fun updateSchedule(
        @AuthenticationPrincipal userId: Long,
        @PathVariable meetingId: Long,
        @Valid @RequestBody request: UpdateScheduleRequest,
    ): ResponseEntity<Unit> {
        meetingFacade.updateSchedule(request.toCommand(meetingId, userId))
        return ResponseEntity.ok().build()
    }

    @Operation(summary = "내 약속 날짜 조회", security = [SecurityRequirement(name = "Bearer Authentication")])
    @ApiResponse(responseCode = "200", description = "내가 선택한 약속 날짜 목록 반환")
    @GetMapping("/{meetingId}/schedules/me")
    fun getMySchedules(
        @AuthenticationPrincipal userId: Long,
        @PathVariable meetingId: Long,
    ): ResponseEntity<MyScheduleResponse> {
        val dates = meetingFacade.getMySchedules(meetingId, userId)
        return ResponseEntity.ok(MyScheduleResponse(meetingId = meetingId, scheduledDates = dates))
    }

    @Operation(summary = "가용 날짜 히트맵 조회", security = [SecurityRequirement(name = "Bearer Authentication")])
    @ApiResponse(responseCode = "200", description = "날짜별 참여 가능 인원 수 반환")
    @GetMapping("/{meetingId}/heatmap")
    fun getHeatmap(
        @PathVariable meetingId: Long,
    ): ResponseEntity<HeatmapResponse> {
        val info = meetingFacade.getHeatmap(meetingId)
        return ResponseEntity.ok(HeatmapResponse.from(info))
    }

    @Operation(summary = "날짜 확정 (방장 전용)", security = [SecurityRequirement(name = "Bearer Authentication")])
    @ApiResponse(responseCode = "200", description = "날짜 확정 성공 — status CONFIRMED로 변경")
    @PostMapping("/{meetingId}/confirm")
    fun confirmDate(
        @AuthenticationPrincipal userId: Long,
        @PathVariable meetingId: Long,
        @Valid @RequestBody request: ConfirmDateRequest,
    ): ResponseEntity<MeetingResponse> {
        val info = meetingFacade.confirmDate(request.toCommand(meetingId, userId))
        return ResponseEntity.ok(MeetingResponse.from(info))
    }
}
