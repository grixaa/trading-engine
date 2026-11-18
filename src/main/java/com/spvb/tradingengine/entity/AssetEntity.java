package com.spvb.tradingengine.entity;

import com.spvb.tradingengine.enums.AssetType;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import static jakarta.persistence.EnumType.STRING;
import static lombok.AccessLevel.PRIVATE;

@Entity
@Table(name = "asset")
@NoArgsConstructor
@Getter
@FieldDefaults(level = PRIVATE)
public class AssetEntity {
    @Id
    String code;

    @Enumerated(STRING)
    AssetType type;
}
