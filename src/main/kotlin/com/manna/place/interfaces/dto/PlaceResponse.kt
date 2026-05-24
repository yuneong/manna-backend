package com.manna.place.interfaces.dto

import com.manna.place.application.info.PlaceInfo
import com.manna.place.application.info.PlacesInfo

data class ProposerResponse(val id: Long, val nickname: String)

data class PlaceResponse(
    val id: Long,
    val name: String,
    val url: String?,
    val memo: String?,
    val proposer: ProposerResponse,
    val voteCount: Int,
    val voters: List<String>,
    val myVoted: Boolean,
) {
    companion object {
        fun from(info: PlaceInfo) = PlaceResponse(
            id = info.id,
            name = info.name,
            url = info.url,
            memo = info.memo,
            proposer = ProposerResponse(info.proposer.id, info.proposer.nickname),
            voteCount = info.voteCount,
            voters = info.voters,
            myVoted = info.myVoted,
        )
    }
}

data class PlacesResponse(
    val places: List<PlaceResponse>,
    val totalParticipants: Int,
) {
    companion object {
        fun from(info: PlacesInfo) = PlacesResponse(
            places = info.places.map { PlaceResponse.from(it) },
            totalParticipants = info.totalParticipants,
        )
    }
}
