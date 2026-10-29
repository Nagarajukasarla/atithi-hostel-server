package com.atithi.aavas.repositories;

import com.atithi.aavas.models.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {

    List<RoomType> findByHostel_IdOrderBySharingCountAscNameAsc(Long hostelId);

    Optional<RoomType> findByIdAndHostel_Id(Long id, Long hostelId);

    boolean existsByHostel_IdAndNameIgnoreCase(Long hostelId, String name);

    boolean existsByHostel_IdAndNameIgnoreCaseAndIdNot(Long hostelId, String name, Long id);

    boolean existsByIdAndHostel_Id(Long id, Long hostelId);

    @Query("""
            SELECT rt.id, rt.name, rt.sharingCount,
                   COALESCE(SUM(r.maxCapacity), 0),
                   COALESCE(SUM(r.occupied), 0)
            FROM RoomType rt
            LEFT JOIN Room r ON r.roomType = rt AND r.hostel.id = :hostelId
            WHERE rt.hostel.id = :hostelId
            GROUP BY rt.id, rt.name, rt.sharingCount
            ORDER BY rt.sharingCount ASC, rt.name ASC
            """)
    List<Object[]> findSharingTypeOccupancySummary(@Param("hostelId") Long hostelId);
}
