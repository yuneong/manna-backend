package com.manna.place.application.facade

import com.manna.meeting.domain.service.MeetingDomainService
import com.manna.place.application.command.CreatePlaceCommand
import com.manna.place.application.info.PlaceInfo
import com.manna.place.application.info.PlacesInfo
import com.manna.place.domain.service.PlaceService
import com.manna.user.domain.service.UserDomainService
import org.springframework.stereotype.Component

@Component
class PlaceFacade(
    private val placeService: PlaceService,
    private val meetingDomainService: MeetingDomainService,
    private val userDomainService: UserDomainService,
) {

    fun getPlaces(meetingId: Long, userId: Long): PlacesInfo {
        val places = placeService.getPlaces(meetingId)
        val votes = placeService.getVotesByPlaceIds(places.map { it.id })
        val votesByPlace = votes.groupBy { it.placeId }

        val allUserIds = (votes.map { it.userId } + places.map { it.suggestedBy }).distinct()
        val userMap = if (allUserIds.isEmpty()) emptyMap()
        else userDomainService.getUsersByIds(allUserIds).associateBy { it.id }

        val totalParticipants = meetingDomainService.getParticipantCount(meetingId)

        val placeInfos = places
            .map { place ->
                PlaceInfo.from(place, votesByPlace[place.id] ?: emptyList(), userId, userMap, userMap)
            }
            .sortedByDescending { it.voteCount }

        return PlacesInfo(places = placeInfos, totalParticipants = totalParticipants)
    }

    fun propose(command: CreatePlaceCommand): PlaceInfo {
        val place = placeService.propose(command)
        val userMap = userDomainService.getUsersByIds(listOf(place.suggestedBy)).associateBy { it.id }
        return PlaceInfo.from(place, emptyList(), command.userId, emptyMap(), userMap)
    }

    fun toggleVote(meetingId: Long, placeId: Long, userId: Long) {
        placeService.toggleVote(meetingId, placeId, userId)
    }
}
