package com.atithi.aavas.models;

import jakarta.persistence.*;
import lombok.*;

/**
 * Reusable classification for rooms in a hostel, such as "4 Sharing".
 * Capacity remains a property of an individual Room.
 */
@Entity
@Table(name = "room_types", uniqueConstraints = {
        @UniqueConstraint(name = "uk_room_type_hostel_name", columnNames = {"hostel_id", "name"})
}, indexes = {
        @Index(name = "idx_room_types_hostel_active", columnList = "hostel_id,active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomType extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hostel_id", nullable = false)
    private Hostel hostel;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "sharing_count", nullable = false)
    private Integer sharingCount;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    public Long getHostelId() {
        return hostel != null ? hostel.getId() : null;
    }
}
