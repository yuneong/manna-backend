package com.manna.settlement.domain.entity

import com.manna.meeting.domain.entity.Meeting
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "settlements")
class Settlement(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    val meeting: Meeting,

    @Column(nullable = false)
    val creatorId: Long,

    @Column(nullable = false)
    val title: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: SettlementType,

    val totalAmount: Int? = null,

    @Column(nullable = false)
    val bankName: String,

    @Column(nullable = false)
    val accountNumber: String,

    @Column(nullable = false)
    val accountHolder: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: SettlementStatus = SettlementStatus.IN_PROGRESS,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
