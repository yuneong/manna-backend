package com.manna.place.infrastructure.jpa

import com.manna.place.domain.entity.Place
import com.manna.place.domain.entity.PlaceVote
import org.springframework.data.jpa.repository.JpaRepository

interface PlaceJpaRepository : JpaRepository<Place, Long> {
    fun findByMeetingId(meetingId: Long): List<Place>
    fun deleteByMeetingId(meetingId: Long)
}

interface PlaceVoteJpaRepository : JpaRepository<PlaceVote, Long> {
    fun findByPlaceIdAndUserId(placeId: Long, userId: Long): PlaceVote?
    fun findByPlaceIdIn(placeIds: List<Long>): List<PlaceVote>
    fun deleteByPlaceId(placeId: Long)
}
