package com.disasterreliefnet.common.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Retrieves unpublished outbox events using PostgreSQL FOR UPDATE SKIP LOCKED concurrency semantics.
     * Prevents lock contention across multiple microservice instances.
     *
     * @param limit maximum number of records to retrieve per polling cycle
     * @return list of locked unpublished outbox events
     */
    @Query(value = "SELECT * FROM outbox_events WHERE published = false ORDER BY created_at ASC LIMIT :limit FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<OutboxEvent> findUnpublishedForUpdateSkipLocked(@Param("limit") int limit);

    /**
     * Fallback standard finder for environments without SKIP LOCKED support (e.g. standard H2 testing).
     */
    List<OutboxEvent> findTop100ByPublishedFalseOrderByCreatedAtAsc();
}
