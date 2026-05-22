package com.manna.meeting.interfaces.dto

import com.manna.meeting.application.command.CreateRevoteCommand
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class CreateRevoteRequest(
    @field:NotEmpty(message = "후보 날짜는 필수입니다")
    @field:Size(min = 2, message = "후보 날짜는 2개 이상이어야 합니다")
    val candidateDates: List<LocalDate>,
) {
    fun toCommand(meetingId: Long, userId: Long) = CreateRevoteCommand(
        meetingId = meetingId,
        userId = userId,
        candidateDates = candidateDates,
    )
}
