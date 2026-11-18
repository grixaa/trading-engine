package com.spvb.tradingengine.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

import static lombok.AccessLevel.PRIVATE;

@Entity
@Table(name = "asset_balance")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@FieldDefaults(level = PRIVATE)
public class BalanceEntity {
    @EmbeddedId
    BalanceId id;

    @Setter
    BigDecimal available;

    @Setter
    BigDecimal reserved;
}
