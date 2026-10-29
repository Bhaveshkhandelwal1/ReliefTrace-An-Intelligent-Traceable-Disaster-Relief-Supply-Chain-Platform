package com.disasterreliefnet.receiver.repository;

import com.disasterreliefnet.receiver.domain.Receiver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Receiver} aggregate roots.
 */
public interface ReceiverRepository extends JpaRepository<Receiver, UUID> {

    @Query("SELECT r FROM Receiver r WHERE r.deletedAt IS NULL")
    Page<Receiver> findAllActive(Pageable pageable);

    @Query("SELECT r FROM Receiver r WHERE r.disasterId = :disasterId AND r.deletedAt IS NULL")
    Page<Receiver> findAllByDisasterId(@Param("disasterId") UUID disasterId, Pageable pageable);

    @Query("SELECT r FROM Receiver r WHERE r.receiverId = :receiverId AND r.deletedAt IS NULL")
    Optional<Receiver> findActiveById(@Param("receiverId") UUID receiverId);

    boolean existsByEmailAndDeletedAtIsNull(String email);
}
