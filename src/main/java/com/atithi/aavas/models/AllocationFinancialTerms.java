package com.atithi.aavas.models;

import com.atithi.aavas.enums.BillingMode;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Stores the customer-specific financial agreement for one allocation.
 * Captures both the tariff snapshot and the negotiated amounts so that
 * future tariff changes do not affect existing residents.
 */
@Entity
@Table(
    name = "allocation_financial_terms",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_allocation_financial_terms_allocation",
            columnNames = "allocation_id"
        )
    },
    indexes = {
        @Index(name = "idx_financial_terms_room_tariff", columnList = "room_tariff_id"),
        @Index(name = "idx_financial_terms_hostel", columnList = "hostel_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllocationFinancialTerms extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hostel_id", nullable = false)
    private Hostel hostel;

    /** One allocation has exactly one set of financial terms. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "allocation_id", nullable = false, unique = true)
    private RoomAllocation allocation;

    /** The standard tariff that was the pricing source when terms were created. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_tariff_id", nullable = false)
    private RoomTariff roomTariff;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_mode", length = 30)
    @Builder.Default
    private BillingMode billingMode = BillingMode.MONTHLY;

    /** Negotiated/agreed monthly rent — may differ from tariff.monthlyRent. Null for DAILY stays. */
    @Column(name = "monthly_rent", precision = 12, scale = 2)
    private BigDecimal monthlyRent;

    /** Negotiated/agreed daily rate — null for MONTHLY stays. */
    @Column(name = "daily_rate", precision = 12, scale = 2)
    private BigDecimal dailyRate;

    /** Negotiated/agreed security deposit — may differ from tariff.securityDepositRequired. */
    @Column(name = "security_deposit_required", nullable = false, precision = 12, scale = 2)
    private BigDecimal securityDepositRequired;

    /** The join date used as the billing cycle anchor for future monthly charges. */
    @Column(name = "billing_anchor_date", nullable = false)
    private LocalDate billingAnchorDate;

    public Long getHostelId() {
        return hostel != null ? hostel.getId() : null;
    }

    public Long getAllocationId() {
        return allocation != null ? allocation.getId() : null;
    }

    public Long getRoomTariffId() {
        return roomTariff != null ? roomTariff.getId() : null;
    }
}
