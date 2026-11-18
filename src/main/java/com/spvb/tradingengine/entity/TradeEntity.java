package com.spvb.tradingengine.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PRIVATE;

@Entity
@Table(name = "trade")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@FieldDefaults(level = PRIVATE)
public class TradeEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    Long id;

    String seccode;
    long buyOrderId;
    long sellOrderId;
    BigDecimal price;
    int quantity;
    OffsetDateTime executedAt;
}
