package org.example.trusttrade.order.repository;

import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.example.trusttrade.order.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    select p
    from Payment p
    where p.idempotencyKey = :idempotencyKey
""")
    Optional<Payment> findByIdempotencyKeyForUpdate(
            @Param("idempotencyKey") String idempotencyKey
    );

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);



    //결제 조회 스케줄러
    @Query("SELECT p FROM Payment p JOIN p.order o WHERE p.status = 'CONFIRMING' " +
            "AND p.requestAt <= :threshold")
    List<Payment> findByStatusWithLock(@Param("threshold") LocalDateTime threshold);


}
