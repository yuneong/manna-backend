package com.manna.place.application.info

import com.manna.place.domain.entity.Place
import com.manna.place.domain.entity.PlaceVote
import com.manna.user.domain.entity.User

data class ProposerInfo(val id: Long, val nickname: String)

data class VoterInfo(val id: Long, val nickname: String, val profileImageUrl: String?)

data class PlaceInfo(
    val id: Long,
    val name: String,
    val url: String?,
    val memo: String?,
    val proposer: ProposerInfo,
    val voteCount: Int,
    val voters: List<VoterInfo>,
    val myVoted: Boolean,
) {
    companion object {
        fun from(
            place: Place,
            votes: List<PlaceVote>,
            requestUserId: Long,
            userMap: Map<Long, User>,
            proposerMap: Map<Long, User>,
        ): PlaceInfo {
            val proposer = proposerMap[place.suggestedBy]
            return PlaceInfo(
                id = place.id,
                name = place.name,
                url = place.url,
                memo = place.memo,
                proposer = ProposerInfo(
                    id = place.suggestedBy,
                    nickname = proposer?.nickname ?: "",
                ),
                voteCount = votes.size,
                voters = votes.mapNotNull { vote ->
                    userMap[vote.userId]?.let { VoterInfo(it.id, it.nickname, it.profileImageUrl) }
                },
                myVoted = votes.any { it.userId == requestUserId },
            )
        }
    }
}

data class PlacesInfo(
    val places: List<PlaceInfo>,
    val totalParticipants: Int,
)
