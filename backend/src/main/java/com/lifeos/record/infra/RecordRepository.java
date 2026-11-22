package com.lifeos.record.infra;

import com.lifeos.record.domain.RecordEntity;
import com.lifeos.record.domain.RecordStatus;
import com.lifeos.record.domain.RecordType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecordRepository extends JpaRepository<RecordEntity, UUID> {
    Optional<RecordEntity> findByIdAndHouseholdId(UUID id, UUID householdId);

    @Query("""
            select r from RecordEntity r
            where r.householdId = :householdId
              and (:status is null or r.status = :status)
              and (:type is null or r.type = :type)
              and (:q is null or lower(r.title) like lower(concat('%', cast(:q as string), '%'))
                   or lower(coalesce(r.description, '')) like lower(concat('%', cast(:q as string), '%'))
                   or lower(coalesce(r.category, '')) like lower(concat('%', cast(:q as string), '%')))
            order by r.updatedAt desc
            """)
    List<RecordEntity> search(@Param("householdId") UUID householdId,
                              @Param("status") RecordStatus status,
                              @Param("type") RecordType type,
                              @Param("q") String q);
}
