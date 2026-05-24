package com.manna.place.domain.service

import com.manna.common.exception.ErrorCode
import com.manna.common.exception.MannaException
import com.manna.meeting.domain.entity.MeetingStatus
import com.manna.meeting.domain.repository.MeetingRepository
import com.manna.place.application.command.CreatePlaceCommand
import com.manna.place.domain.entity.Place
import com.manna.place.domain.entity.PlaceVote
import com.manna.place.domain.repository.PlaceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PlaceService(
    private val placeRepository: PlaceRepository,
    private val meetingRepository: MeetingRepository,
) {

    @Transactional
    fun propose(command: CreatePlaceCommand): Place {
        val meeting = meetingRepository.findById(command.meetingId)
            ?: throw MannaException(ErrorCode.MEETING_NOT_FOUND)
        if (meeting.status != MeetingStatus.CONFIRMED) throw MannaException(ErrorCode.MEETING_NOT_CONFIRMED)
        if (meetingRepository.findParticipantByMeetingIdAndUserId(command.meetingId, command.userId) == null) {
            throw MannaException(ErrorCode.NOT_MEETING_PARTICIPANT)
        }
        return placeRepository.save(
            Place(
                meetingId = command.meetingId,
                suggestedBy = command.userId,
                name = command.name,
                url = command.url,
                memo = command.memo,
            ),
        )
    }

    @Transactional
    fun toggleVote(meetingId: Long, placeId: Long, userId: Long) {
        if (meetingRepository.findParticipantByMeetingIdAndUserId(meetingId, userId) == null) {
            throw MannaException(ErrorCode.NOT_MEETING_PARTICIPANT)
        }
        val place = placeRepository.findById(placeId) ?: throw MannaException(ErrorCode.PLACE_NOT_FOUND)
        if (place.meetingId != meetingId) throw MannaException(ErrorCode.PLACE_NOT_FOUND)

        val existing = placeRepository.findVoteByPlaceIdAndUserId(placeId, userId)
        if (existing != null) {
            placeRepository.deleteVote(existing)
        } else {
            placeRepository.saveVote(PlaceVote(placeId = placeId, userId = userId))
        }
    }

    fun getPlaces(meetingId: Long): List<Place> =
        placeRepository.findByMeetingId(meetingId)

    fun getVotesByPlaceIds(placeIds: List<Long>): List<PlaceVote> {
        if (placeIds.isEmpty()) return emptyList()
        return placeRepository.findVotesByPlaceIds(placeIds)
    }

    @Transactional
    fun deleteAllByMeetingId(meetingId: Long) {
        val places = placeRepository.findByMeetingId(meetingId)
        places.forEach { place -> placeRepository.deleteVotesByPlaceId(place.id) }
        placeRepository.deleteByMeetingId(meetingId)
    }
}
