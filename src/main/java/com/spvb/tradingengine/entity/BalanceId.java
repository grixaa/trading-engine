package com.spvb.tradingengine.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;

import static lombok.AccessLevel.PRIVATE;

@Embeddable
@Getter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = PRIVATE)
public class BalanceId implements Serializable {
    long userId;
    String asset;
}
