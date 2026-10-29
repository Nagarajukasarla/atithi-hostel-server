package com.atithi.aavas.models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Standard pricing for a hostel + room type over a period.
 * Not customer-specific. Customer-specific agreed amounts live in AllocationFinancialTerms.
 */
@Entity
@Table(
    name = "room_tariffs",
    indexes = {
        @Index(name = "idx_room_tariff_hostel_room_type", columnList = "hostel_id, room_type_id"),
        @Index(name = "idx_room_tariff_effective_from", columnList = "effective_from")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomTariff extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hostel_id", nullable = false)
    private Hostel hostel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    @Column(name = "monthly_rent", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyRent;

    @Column(name = "daily_rate", precision = 12, scale = 2)
    private BigDecimal dailyRate;

    @Column(name = "security_deposit_required", nullable = false, precision = 12, scale = 2)
    private BigDecimal securityDepositRequired;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    /** Null means this tariff is currently open/active. */
    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    public Long getHostelId() {
        return hostel != null ? hostel.getId() : null;
    }

    public Long getRoomTypeId() {
        return roomType != null ? roomType.getId() : null;
    }
}
