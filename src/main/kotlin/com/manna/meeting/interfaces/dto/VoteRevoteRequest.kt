package com.manna.meeting.interfaces.dto

import com.manna.meeting.application.command.VoteRevoteCommand
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class VoteRevoteRequest(
    @field:NotNull(message = "투표 날짜는 필수입니다")
    val votedDate: LocalDate,
) {
    fun toCommand(meetingId: Long, userId: Long) = VoteRevoteCommand(
        meetingId = meetingId,
        userId = userId,
        votedDate = votedDate,
    )
}
