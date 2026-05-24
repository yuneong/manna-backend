package com.manna.place.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "places")
class Place(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(nullable = false)
    val meetingId: Long,

    @Column(nullable = false)
    val suggestedBy: Long,

    @Column(nullable = false)
    val name: String,

    @Column(length = 500)
    val url: String? = null,

    @Column(length = 120)
    val memo: String? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
