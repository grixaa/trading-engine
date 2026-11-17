package com.spvb.tradingengine.entity;

import com.spvb.tradingengine.domain.OrderStatus;
import com.spvb.tradingengine.domain.Side;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PRIVATE;

@Entity
@Table(name = "trade_order")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@FieldDefaults(level = PRIVATE)
public class OrderEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    Long id;

    long userId;
    String seccode;

    @Enumerated(EnumType.STRING)
    Side side;

    @Setter
    BigDecimal price;

    @Setter
    int quantity;

    @Setter
    int filled;

    @Enumerated(EnumType.STRING)
    @Setter
    OrderStatus status;

    @Setter
    long priority;
    OffsetDateTime createdAt;

    @Setter
    OffsetDateTime updatedAt;
}
