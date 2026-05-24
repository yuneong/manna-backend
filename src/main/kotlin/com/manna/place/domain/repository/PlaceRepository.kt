package com.manna.place.domain.repository

import com.manna.place.domain.entity.Place
import com.manna.place.domain.entity.PlaceVote

interface PlaceRepository {
    fun save(place: Place): Place
    fun findById(id: Long): Place?
    fun findByMeetingId(meetingId: Long): List<Place>
    fun deleteByMeetingId(meetingId: Long)

    fun saveVote(vote: PlaceVote): PlaceVote
    fun findVoteByPlaceIdAndUserId(placeId: Long, userId: Long): PlaceVote?
    fun findVotesByPlaceIds(placeIds: List<Long>): List<PlaceVote>
    fun deleteVote(vote: PlaceVote)
    fun deleteVotesByPlaceId(placeId: Long)
}
