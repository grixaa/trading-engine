package com.spvb.tradingengine.repository.mapper;

import com.spvb.tradingengine.domain.Asset;
import com.spvb.tradingengine.domain.Balance;
import com.spvb.tradingengine.domain.Instrument;
import com.spvb.tradingengine.domain.Order;
import com.spvb.tradingengine.domain.Trade;
import com.spvb.tradingengine.entity.AssetEntity;
import com.spvb.tradingengine.entity.BalanceEntity;
import com.spvb.tradingengine.entity.InstrumentEntity;
import com.spvb.tradingengine.entity.OrderEntity;
import com.spvb.tradingengine.entity.TradeEntity;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PersistenceMapper {
    public Asset toAsset(AssetEntity entity) {
        return Asset.builder()
                .code(entity.getCode())
                .type(entity.getType())
                .build();
    }

    public Balance toBalance(BalanceEntity entity) {
        return Balance.builder()
                .asset(entity.getId().getAsset())
                .available(entity.getAvailable())
                .reserved(entity.getReserved())
                .build();
    }

    public Instrument toInstrument(InstrumentEntity entity) {
        return Instrument.builder()
                .seccode(entity.getSeccode())
                .type(entity.getType())
                .baseAsset(entity.getBaseAsset())
                .quoteCurrency(entity.getQuoteCurrency())
                .lotSize(entity.getLotSize())
                .minStep(entity.getMinStep())
                .build();
    }

    public Order toOrder(OrderEntity entity) {
        return Order.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .seccode(entity.getSeccode())
                .side(entity.getSide())
                .price(entity.getPrice())
                .quantity(entity.getQuantity())
                .filled(entity.getFilled())
                .status(entity.getStatus())
                .priority(entity.getPriority())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public Trade toTrade(TradeEntity entity) {
        return Trade.builder()
                .id(entity.getId())
                .seccode(entity.getSeccode())
                .buyOrderId(entity.getBuyOrderId())
                .sellOrderId(entity.getSellOrderId())
                .price(entity.getPrice())
                .quantity(entity.getQuantity())
                .executedAt(entity.getExecutedAt())
                .build();
    }
}
