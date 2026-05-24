package com.manna.place.infrastructure.repository

import com.manna.place.domain.entity.Place
import com.manna.place.domain.entity.PlaceVote
import com.manna.place.domain.repository.PlaceRepository
import com.manna.place.infrastructure.jpa.PlaceJpaRepository
import com.manna.place.infrastructure.jpa.PlaceVoteJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class PlaceRepositoryImpl(
    private val placeJpaRepository: PlaceJpaRepository,
    private val placeVoteJpaRepository: PlaceVoteJpaRepository,
) : PlaceRepository {

    override fun save(place: Place): Place =
        placeJpaRepository.save(place)

    override fun findById(id: Long): Place? =
        placeJpaRepository.findById(id).orElse(null)

    override fun findByMeetingId(meetingId: Long): List<Place> =
        placeJpaRepository.findByMeetingId(meetingId)

    @Transactional
    override fun deleteByMeetingId(meetingId: Long) =
        placeJpaRepository.deleteByMeetingId(meetingId)

    override fun saveVote(vote: PlaceVote): PlaceVote =
        placeVoteJpaRepository.save(vote)

    override fun findVoteByPlaceIdAndUserId(placeId: Long, userId: Long): PlaceVote? =
        placeVoteJpaRepository.findByPlaceIdAndUserId(placeId, userId)

    override fun findVotesByPlaceIds(placeIds: List<Long>): List<PlaceVote> =
        placeVoteJpaRepository.findByPlaceIdIn(placeIds)

    override fun deleteVote(vote: PlaceVote) =
        placeVoteJpaRepository.delete(vote)

    @Transactional
    override fun deleteVotesByPlaceId(placeId: Long) =
        placeVoteJpaRepository.deleteByPlaceId(placeId)
}
