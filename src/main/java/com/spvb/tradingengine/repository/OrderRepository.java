package com.spvb.tradingengine.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.spvb.tradingengine.enums.OrderStatus;
import com.spvb.tradingengine.enums.Side;
import com.spvb.tradingengine.entity.OrderEntity;
import jakarta.persistence.QueryHint;
import org.hibernate.jpa.HibernateHints;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    Optional<OrderEntity> findByUserIdAndId(long userId, long id);

    // Read an immutable scalar before locking so the persistence context does not cache a stale order.
    @Query("SELECT o.seccode FROM OrderEntity o WHERE o.userId = :userId AND o.id = :id")
    Optional<String> findInstrumentCodeForUserOrder(long userId, long id);

    @Query(value = "SELECT nextval('order_priority_seq')", nativeQuery = true)
    long nextOrderPriority();

    @Query("""
            SELECT o FROM OrderEntity o
            WHERE o.seccode = :seccode AND o.status IN (OPEN, PARTIALLY_FILLED)
              AND o.side = SELL AND o.price <= :price
            ORDER BY o.price ASC, o.priority
            """)
    @QueryHints(@QueryHint(name = HibernateHints.HINT_FETCH_SIZE, value = "128"))
    Stream<OrderEntity> streamMatchingSellOrders(String seccode, BigDecimal price);

    @Query("""
            SELECT o FROM OrderEntity o
            WHERE o.seccode = :seccode AND o.status IN (OPEN, PARTIALLY_FILLED)
              AND o.side = BUY AND o.price >= :price
            ORDER BY o.price DESC, o.priority
            """)
    @QueryHints(@QueryHint(name = HibernateHints.HINT_FETCH_SIZE, value = "128"))
    Stream<OrderEntity> streamMatchingBuyOrders(String seccode, BigDecimal price);

    @Query("""
            SELECT o FROM OrderEntity o WHERE o.userId = :userId
              AND (:seccode IS NULL OR o.seccode = :seccode)
              AND (:side IS NULL OR o.side = :side)
              AND (:status IS NULL OR o.status = :status)
              AND (:beforeId IS NULL OR o.id < :beforeId)
            ORDER BY o.id DESC
            """)
    List<OrderEntity> findUserOrdersByFilters(
            long userId, String seccode, Side side, OrderStatus status, Long beforeId, Pageable page
    );

    @Query(value = """
            (SELECT side, price, SUM(quantity - filled) AS quantity
             FROM trade_order
             WHERE seccode = :seccode AND side = 'BUY' AND status IN ('OPEN', 'PARTIALLY_FILLED')
             GROUP BY side, price ORDER BY price DESC LIMIT :depth)
            UNION ALL
            (SELECT side, price, SUM(quantity - filled) AS quantity
             FROM trade_order
             WHERE seccode = :seccode AND side = 'SELL' AND status IN ('OPEN', 'PARTIALLY_FILLED')
             GROUP BY side, price ORDER BY price ASC LIMIT :depth)
            """, nativeQuery = true)
    List<BookLevelProjection> findOrderBookLevels(String seccode, int depth);
}
