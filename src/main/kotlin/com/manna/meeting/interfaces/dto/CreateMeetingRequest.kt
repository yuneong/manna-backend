package com.manna.meeting.interfaces.dto

import com.manna.meeting.application.command.CreateMeetingCommand
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class CreateMeetingRequest(
    @field:NotBlank(message = "약속방 제목은 필수입니다")
    @field:Size(max = 50, message = "제목은 50자 이하이어야 합니다")
    val title: String,

    val description: String?,

    @field:NotNull(message = "시작 날짜는 필수입니다")
    val dateRangeStart: LocalDate,

    @field:NotNull(message = "종료 날짜는 필수입니다")
    val dateRangeEnd: LocalDate,
) {
    fun toCommand(hostId: Long) = CreateMeetingCommand(
        hostId = hostId,
        title = title,
        description = description,
        dateRangeStart = dateRangeStart,
        dateRangeEnd = dateRangeEnd,
    )
}
