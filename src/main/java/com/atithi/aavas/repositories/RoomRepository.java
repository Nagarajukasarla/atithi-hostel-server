package com.atithi.aavas.repositories;

import com.atithi.aavas.models.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByHostel_IdOrderByRoomNumberAsc(Long hostelId);

    List<Room> findByHostel_IdAndFloorOrderByRoomNumberAsc(Long hostelId, String floor);

    Optional<Room> findByIdAndHostel_Id(Long id, Long hostelId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.id = :id AND r.hostel.id = :hostelId")
    Optional<Room> findByIdAndHostel_IdForUpdate(@Param("id") Long id, @Param("hostelId") Long hostelId);

    boolean existsByHostel_IdAndRoomNumber(Long hostelId, String roomNumber);

    boolean existsByHostel_IdAndRoomNumberAndIdNot(Long hostelId, String roomNumber, Long id);

    boolean existsByRoomType_Id(Long roomTypeId);

    @Query("""
            SELECT COALESCE(SUM(r.maxCapacity), 0), COALESCE(SUM(r.occupied), 0)
            FROM Room r
            WHERE r.hostel.id = :hostelId
            """)
    List<Object[]> sumOccupancyByHostelId(@Param("hostelId") Long hostelId);
}
