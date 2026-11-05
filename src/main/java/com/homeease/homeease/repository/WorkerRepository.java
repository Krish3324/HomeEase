package com.homeease.homeease.repository;

import com.homeease.homeease.model.Worker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WorkerRepository extends JpaRepository<Worker, Long> {

    @Query("SELECT w.category, COUNT(w) FROM Worker w GROUP BY w.category")
    List<Object[]> countWorkersByCategory();

    @Query("SELECT w.city, COUNT(w) FROM Worker w GROUP BY w.city")
    List<Object[]> countWorkersByCity();

    @Query("SELECT w FROM Worker w WHERE " +
           "(LOWER(w.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(w.city) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(w.state) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(w.category) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Worker> searchWorkers(@Param("search") String search);

    @Query("SELECT w FROM Worker w WHERE w.approved = true AND " +
           "(LOWER(w.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(w.city) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(w.state) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(w.category) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Worker> searchActiveWorkers(@Param("search") String search);

    @Query("SELECT w FROM Worker w WHERE w.approved = false AND " +
           "(LOWER(w.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(w.city) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(w.state) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(w.category) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Worker> searchPendingWorkers(@Param("search") String search);

    Optional<Worker> findByEmail(String email);

    Optional<Worker> findByVerificationToken(String token);

    List<Worker> findByCategoryAndApprovedTrueAndEnabledTrue(String category);

    @Query("SELECT COUNT(w) FROM Worker w WHERE LOWER(w.category) = LOWER(:category) AND w.approved = true AND w.enabled = true")
    long countAvailableByCategory(@Param("category") String category);

    List<Worker> findByApprovedTrueAndEnabledTrue();

    List<Worker> findByApprovedFalse();

    List<Worker> findByApprovedTrue();

    long countByApprovedTrue();

    long countByApprovedFalse();

    boolean existsByPublicUserId(String publicUserId);

    long countByEnabled(boolean enabled);

    // ================= NEARBY WORKERS (online only) =================
    @Query(value = """
                SELECT *,
                (
                    6371 * acos(
                        cos(radians(:lat)) *
                        cos(radians(latitude)) *
                        cos(radians(longitude) - radians(:lng)) +
                        sin(radians(:lat)) *
                        sin(radians(latitude))
                    )
                ) AS distance
                FROM workers
                WHERE approved = 1
                AND enabled = 1
                AND online = 1
                AND LOWER(category) = LOWER(:category)
                HAVING distance <= :radius
                ORDER BY distance
            """, nativeQuery = true)
    List<Worker> findNearbyWorkers(
            @Param("category") String category,
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radius") double radius);
}