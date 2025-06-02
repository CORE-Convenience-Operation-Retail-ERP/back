package com.core.erp.repository;

import com.core.erp.domain.AttendanceEntity;
import com.core.erp.domain.PartTimerEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<AttendanceEntity, Integer> {
    List<AttendanceEntity> findByEmployee_EmpId(int empId);

    @Query(value = """
    SELECT * FROM attendance 
    WHERE part_timer_id = :partTimerId 
      AND store_id = :storeId 
      AND in_time BETWEEN :start AND :end
""", nativeQuery = true)
    List<AttendanceEntity> findWorkLog(
            @Param("partTimerId") Integer partTimerId,
            @Param("storeId") Integer storeId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    /**
     * (1) 특정 직원(empId)의 특정 날짜 출근 기록 중 가장 최신 출근 (inTime 기준) 조회
     */
    Optional<AttendanceEntity> findTopByEmployee_EmpIdAndAttendDateOrderByInTimeDesc(int empId, LocalDate attendDate);

    /**
     * (2) 해당 아르바이트(partTimer)가 특정 날짜에 이미 출근했는지 여부 확인
     */
    boolean existsByPartTimerAndAttendDate(PartTimerEntity partTimer, LocalDate date);

    /**
     * (3) 해당 아르바이트(partTimer)의 특정 날짜 출근 기록 단건 조회
     */
    Optional<AttendanceEntity> findByPartTimerAndAttendDate(PartTimerEntity pt, LocalDate today);

    /**
     * (4) 출퇴근 목록 필터 검색 (storeId, partTimerId, partName, position, 날짜 범위 필터 포함)
     * - storeId, partTimerId, partName, position은 선택적이며, null일 경우 필터링 제외
     * - startDate, endDate는 inTime 기준으로 범위 조건
     * - 결과는 inTime 기준 내림차순 정렬
     */
    @Query("""
        SELECT a FROM AttendanceEntity a
        WHERE (:storeId IS NULL OR a.store.storeId = :storeId)
          AND (:partTimerId IS NULL OR a.partTimer.partTimerId = :partTimerId)
          AND (:partName IS NULL OR a.partTimer.partName LIKE %:partName%)
          AND (:position IS NULL OR a.partTimer.position = :position)
          AND (:startDate IS NULL OR a.inTime >= :startDate)
          AND (:endDate IS NULL OR a.inTime <= :endDate)
        ORDER BY a.inTime DESC
        """)
    Page<AttendanceEntity> findAttendancesWithFilter(
            @Param("storeId") Integer storeId,
            @Param("partTimerId") Integer partTimerId,
            @Param("partName") String partName,
            @Param("position") String position,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    @Query("""
    SELECT COUNT(a) > 0 FROM AttendanceEntity a
    WHERE a.partTimer.partTimerId = :partTimerId
      AND a.store.storeId = :storeId
      AND a.attendDate = :today
      AND a.outTime IS NULL
""")
    boolean isCurrentlyCheckedIn(
            @Param("partTimerId") Long partTimerId,
            @Param("storeId") Integer storeId,
            @Param("today") LocalDate today
    );

    Optional<AttendanceEntity> findTopByPartTimerAndAttendDateOrderByInTimeAsc(PartTimerEntity pt, LocalDate today);

    void deleteAllByPartTimer(PartTimerEntity entity);
}
