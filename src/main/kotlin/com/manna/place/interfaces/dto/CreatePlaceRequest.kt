package com.manna.place.interfaces.dto

import com.manna.place.application.command.CreatePlaceCommand
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreatePlaceRequest(
    @field:NotBlank(message = "장소 이름은 필수입니다")
    val name: String,

    val url: String?,

    @field:Size(max = 120, message = "메모는 120자 이하이어야 합니다")
    val memo: String?,
) {
    fun toCommand(meetingId: Long, userId: Long) = CreatePlaceCommand(
        meetingId = meetingId,
        userId = userId,
        name = name,
        url = url,
        memo = memo,
    )
}
