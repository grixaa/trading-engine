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
import com.spvb.tradingengine.domain.Side;
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

import static com.spvb.tradingengine.domain.Side.BUY;
import static com.spvb.tradingengine.domain.Side.SELL;
import static lombok.AccessLevel.PRIVATE;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class MarketDataService {
    CatalogService catalogService;
    OrderRepository orderRepository;
    TradeRepository tradeRepository;

    public Order order(final long userId, final long id) {
        return orderRepository.findByUserIdAndId(userId, id)
                .map(PersistenceMapper::toOrder)
                .orElseThrow(() -> new OrderNotFoundException("Заявка %d не найдена".formatted(id)));
    }

    public CursorPage<Order> orders(final long userId, @NonNull final OrderFilter filter) {
        val rows = orderRepository.search(
                        userId,
                        filter.seccode(),
                        filter.side(),
                        filter.status(),
                        filter.beforeId(),
                        Pageable.ofSize(filter.limit() + 1)
                ).stream()
                .map(PersistenceMapper::toOrder)
                .toList();

        return page(rows, filter.limit(), Order::id);
    }

    public CursorPage<Trade> trades(@NonNull final TradeFilter filter) {
        val rows = tradeRepository.search(
                        filter.seccode(),
                        filter.orderId(),
                        filter.beforeId(),
                        Pageable.ofSize(filter.limit() + 1)
                ).stream()
                .map(PersistenceMapper::toTrade)
                .toList();

        return page(rows, filter.limit(), Trade::id);
    }

    public OrderBook book(@NonNull final String seccode, final int depth) {
        catalogService.instrument(seccode);

        val levels = orderRepository.bookRows(seccode, depth).stream()
                .map(row -> new BookLevel(
                        Side.valueOf(row.getSide()),
                        row.getPrice(),
                        row.getQuantity()
                )).toList();

        val byPrice = Comparator.comparing(BookLevel::price);

        return new OrderBook(
                seccode,
                quotes(levels, BUY, byPrice.reversed()),
                quotes(levels, SELL, byPrice)
        );
    }

    private List<QuoteLevel> quotes(
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

    private <T> CursorPage<T> page(
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
