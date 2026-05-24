package com.manna.place.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "place_votes",
    uniqueConstraints = [UniqueConstraint(columnNames = ["place_id", "user_id"])],
)
class PlaceVote(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "place_id", nullable = false)
    val placeId: Long,

    @Column(name = "user_id", nullable = false)
    val userId: Long,
)
