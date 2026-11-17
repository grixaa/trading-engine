package com.spvb.tradingengine.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

@Entity
@Table(name = "trading_account")
@Getter
@NoArgsConstructor
@FieldDefaults(level = PRIVATE)
public class AccountEntity {
    @Id
    Long userId;
}
