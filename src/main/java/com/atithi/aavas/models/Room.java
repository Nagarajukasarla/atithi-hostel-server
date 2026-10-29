package com.atithi.aavas.models;

import com.atithi.aavas.enums.RoomState;
import com.atithi.aavas.enums.RoomStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "rooms", uniqueConstraints = {
        @UniqueConstraint(name = "uk_hostel_room_number", columnNames = {"hostel_id", "room_number"})
}, indexes = {
        @Index(name = "idx_rooms_room_type_id", columnList = "room_type_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_number", nullable = false, length = 50)
    private String roomNumber;

    @Column(nullable = false, length = 50)
    private String floor;

    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity;

    @Column(nullable = false)
    @Builder.Default
    private Integer occupied = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_state", nullable = false, length = 20)
    @Builder.Default
    private RoomState roomState = RoomState.CLEAN;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_status", nullable = false, length = 30)
    @Builder.Default
    private RoomStatus roomStatus = RoomStatus.VACANT;

    @Column(name = "last_cleaned")
    private Instant lastCleaned;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * The room type defines sharing count (e.g. "4 Sharing" → sharingCount = 4).
     * Intentionally nullable while existing rooms are being backfilled.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hostel_id", nullable = false)
    private Hostel hostel;

    public Long getHostelId() {
        return hostel != null ? hostel.getId() : null;
    }

    public Long getRoomTypeId() {
        return roomType != null ? roomType.getId() : null;
    }

    /** Convenience accessor — returns the sharing count from the linked room type, or null if not set. */
    public Integer getSharingCount() {
        return roomType != null ? roomType.getSharingCount() : null;
    }
}
