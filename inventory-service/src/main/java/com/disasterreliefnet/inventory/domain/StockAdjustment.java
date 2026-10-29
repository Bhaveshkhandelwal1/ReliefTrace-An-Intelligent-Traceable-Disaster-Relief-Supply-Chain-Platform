package com.disasterreliefnet.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity logging immutable stock adjustments made to items.
 */
@Entity
@Table(name = "stock_adjustments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustment {

    @Id
    @Column(name = "adjustment_id", updatable = false, nullable = false)
    private UUID adjustmentId;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(name = "adjustment", nullable = false, precision = 12, scale = 3)
    private BigDecimal adjustment;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "adjusted_by", nullable = false, length = 255)
    private String adjustedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StockAdjustment that)) return false;
        return adjustmentId != null && Objects.equals(adjustmentId, that.adjustmentId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
