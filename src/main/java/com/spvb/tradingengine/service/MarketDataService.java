package com.spvb.tradingengine.service;

import java.util.Comparator;
import java.util.List;
import java.util.function.ToLongFunction;

import com.spvb.tradingengine.domain.BookLevel;
import com.spvb.tradingengine.domain.CursorPage;
import com.spvb.tradingengine.domain.Order;
import com.spvb.tradingengine.domain.OrderBook;
import com.spvb.tradingengine.domain.OrderFilter;
import com.spvb.tradingengine.domain.QuoteLevel;
import com.spvb.tradingengine.enums.Side;
import com.spvb.tradingengine.domain.Trade;
import com.spvb.tradingengine.domain.TradeFilter;
import com.spvb.tradingengine.exception.OrderNotFoundException;
import com.spvb.tradingengine.repository.OrderRepository;
import com.spvb.tradingengine.repository.TradeRepository;
import com.spvb.tradingengine.repository.mapper.PersistenceMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import static com.spvb.tradingengine.enums.Side.BUY;
import static com.spvb.tradingengine.enums.Side.SELL;
import static lombok.AccessLevel.PRIVATE;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class MarketDataService {
    CatalogService catalogService;
    OrderRepository orderRepository;
    TradeRepository tradeRepository;

    public Order getOrderById(final long userId, final long id) {
        return orderRepository.findByUserIdAndId(userId, id)
                .map(PersistenceMapper::toOrder)
                .orElseThrow(() -> new OrderNotFoundException("Заявка %d не найдена".formatted(id)));
    }

    public CursorPage<Order> listUserOrders(final long userId, @NonNull final OrderFilter filter) {
        val rows = orderRepository.findUserOrdersByFilters(
                        userId,
                        filter.seccode(),
                        filter.side(),
                        filter.status(),
                        filter.beforeId(),
                        Pageable.ofSize(filter.limit() + 1)
                ).stream()
                .map(PersistenceMapper::toOrder)
                .toList();

        return buildCursorPage(rows, filter.limit(), Order::id);
    }

    public CursorPage<Trade> listTrades(@NonNull final TradeFilter filter) {
        val rows = tradeRepository.findTradesByFilters(
                        filter.seccode(),
                        filter.orderId(),
                        filter.beforeId(),
                        Pageable.ofSize(filter.limit() + 1)
                ).stream()
                .map(PersistenceMapper::toTrade)
                .toList();

        return buildCursorPage(rows, filter.limit(), Trade::id);
    }

    public OrderBook getOrderBook(@NonNull final String seccode, final int depth) {
        catalogService.getInstrumentByCode(seccode);

        val levels = orderRepository.findOrderBookLevels(seccode, depth).stream()
                .map(row -> new BookLevel(
                        Side.valueOf(row.getSide()),
                        row.getPrice(),
                        row.getQuantity()
                )).toList();

        val byPrice = Comparator.comparing(BookLevel::price);

        return new OrderBook(
                seccode,
                buildQuoteLevels(levels, BUY, byPrice.reversed()),
                buildQuoteLevels(levels, SELL, byPrice)
        );
    }

    private List<QuoteLevel> buildQuoteLevels(
            @NonNull final List<BookLevel> levels,
            @NonNull final Side side,
            @NonNull final Comparator<BookLevel> order
    ) {
        return levels.stream()
                .filter(level -> level.side() == side)
                .sorted(order)
                .map(level -> new QuoteLevel(level.price(), level.quantity()))
                .toList();
    }

    private <T> CursorPage<T> buildCursorPage(
            @NonNull final List<T> rows,
            final int limit,
            @NonNull final ToLongFunction<T> id
    ) {
        if (rows.size() <= limit) {
            return new CursorPage<>(List.copyOf(rows), null);
        }
        return new CursorPage<>(List.copyOf(rows.subList(0, limit)), id.applyAsLong(rows.get(limit - 1)));
    }
}
