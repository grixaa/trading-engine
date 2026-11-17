package com.spvb.tradingengine.entity;

import com.spvb.tradingengine.domain.InstrumentType;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

import static jakarta.persistence.EnumType.STRING;
import static lombok.AccessLevel.PRIVATE;

@Entity
@Table(name = "instrument")
@NoArgsConstructor
@Getter
@FieldDefaults(level = PRIVATE)
public class InstrumentEntity {
    @Id
    String seccode;

    @Enumerated(STRING)
    InstrumentType type;

    String baseAsset;
    String quoteCurrency;
    int lotSize;
    BigDecimal minStep;
}
