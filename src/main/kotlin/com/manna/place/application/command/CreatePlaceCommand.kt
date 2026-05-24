package com.manna.place.application.command

data class CreatePlaceCommand(
    val meetingId: Long,
    val userId: Long,
    val name: String,
    val url: String?,
    val memo: String?,
)
