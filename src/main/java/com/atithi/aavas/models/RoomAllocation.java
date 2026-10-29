package com.atithi.aavas.models;

import com.atithi.aavas.enums.AllocationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "room_allocations", indexes = {
        @Index(name = "idx_allocations_hostel_id", columnList = "hostel_id"),
        @Index(name = "idx_allocations_customer_id", columnList = "customer_id"),
        @Index(name = "idx_allocations_room_id", columnList = "room_id"),
        @Index(name = "idx_allocations_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomAllocation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hostel_id", nullable = false)
    private Hostel hostel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "bed_number", length = 50)
    private String bedNumber;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "check_out_date")
    private LocalDate checkOutDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private AllocationStatus status = AllocationStatus.ACTIVE;

    public Long getHostelId() {
        return hostel != null ? hostel.getId() : null;
    }

    public Long getCustomerId() {
        return customer != null ? customer.getId() : null;
    }

    public Long getRoomId() {
        return room != null ? room.getId() : null;
    }
}
