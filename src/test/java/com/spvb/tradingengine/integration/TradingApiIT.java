package com.spvb.tradingengine.integration;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spvb.tradingengine.api.dto.AmendOrderRequest;
import com.spvb.tradingengine.api.dto.CreateAssetRequest;
import com.spvb.tradingengine.api.dto.CreateInstrumentRequest;
import com.spvb.tradingengine.api.dto.DepositRequest;
import com.spvb.tradingengine.api.dto.PlaceOrderRequest;
import com.spvb.tradingengine.enums.AssetType;
import com.spvb.tradingengine.enums.Side;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.spvb.tradingengine.enums.AssetType.CURRENCY;
import static com.spvb.tradingengine.enums.AssetType.SECURITY;
import static com.spvb.tradingengine.enums.InstrumentType.STOCK;
import static com.spvb.tradingengine.enums.Side.BUY;
import static com.spvb.tradingengine.enums.Side.SELL;
import static com.spvb.tradingengine.util.ApiConstants.USER_ID_HEADER;
import static java.net.http.HttpRequest.BodyPublishers.noBody;
import static java.util.concurrent.TimeUnit.*;
import static lombok.AccessLevel.PRIVATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT, properties = "server.address=127.0.0.1")
@ActiveProfiles("integration")
@Testcontainers
@Execution(SAME_THREAD)
@FieldDefaults(level = PRIVATE)
class TradingApiIT {
    private static final long BUYER = 1;
    private static final long SELLER = 2;
    private static final String ORDERS = "/api/orders";
    private static final String BOOK = "/api/instruments/DEMO/order-book";
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @LocalServerPort int port;
    @Autowired DataSource dataSource;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;

    @DynamicPropertySource
    static void configurePostgres(@NonNull final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @AfterAll
    static void closeHttpClient() {
        HTTP.close();
    }

    @BeforeEach
    void prepareOrderBook() throws IOException, InterruptedException {
        jdbc.execute("""
                TRUNCATE trade, trade_order, asset_balance, trading_account, instrument, asset
                RESTART IDENTITY
                """);

        createAsset("RUB", CURRENCY);
        createAsset("DEMO", SECURITY);
        createInstrument("DEMO");
        openAccount(BUYER);
        openAccount(SELLER);
        depositAsset(BUYER, "RUB", 10_000);
        depositAsset(SELLER, "DEMO", 100);
    }

    @Test
    void settlesPartialFillAtRestingPriceAndReleasesReserveOnCancel() throws Exception {
        val ask = placeOrder(SELLER, "DEMO", SELL, 100, 2);
        val bid = placeOrder(BUYER, "DEMO", BUY, 105, 3);
        val buyOrderId = bid.path("id").asLong();

        assertThat(ask.path("status").asText())
                .isEqualTo("OPEN");
        assertThat(bid.path("status").asText())
                .isEqualTo("PARTIALLY_FILLED");
        assertThat(bid.path("filled").asInt())
                .isEqualTo(2);
        assertThat(bid.path("remaining").asInt())
                .isEqualTo(1);
        assertBalance(BUYER, "RUB", "6950", "1050");
        assertBalance(BUYER, "DEMO", "20", "0");
        assertBalance(SELLER, "RUB", "2000", "0");
        assertBalance(SELLER, "DEMO", "80", "0");

        val book = get(BOOK);
        assertThat(book.path("bids"))
                .hasSize(1);
        assertThat(book.path("bids").get(0).path("price").decimalValue())
                .isEqualByComparingTo("105");
        assertThat(book.path("bids").get(0).path("quantity").asInt())
                .isEqualTo(1);
        assertThat(book.path("asks"))
                .isEmpty();

        val cancelled = exchange(cancelOrderRequest(BUYER, buyOrderId), 200);
        assertThat(cancelled.path("status").asText())
                .isEqualTo("CANCELLED");
        assertThat(cancelled.path("filled").asInt())
                .isEqualTo(2);
        assertThat(cancelled.path("remaining").asInt())
                .isZero();
        assertBalance(BUYER, "RUB", "8000", "0");
        assertThat(get(BOOK).path("bids"))
                .isEmpty();

        val trades = get("/api/trades?orderId=%s".formatted(buyOrderId)).path("items");
        assertThat(trades)
                .hasSize(1);
        assertThat(trades.get(0).path("price").decimalValue())
                .isEqualByComparingTo("100");
        assertThat(trades.get(0).path("quantity").asInt())
                .isEqualTo(2);
        assertThat(trades.get(0).path("sellOrderId").asLong())
                .isEqualTo(ask.path("id").asLong());
        assertThat(trades.get(0).path("buyOrderId").asLong())
                .isEqualTo(buyOrderId);
    }

    @Test
    void repricesOrderAndMatchesBestPriceThenFifo() throws Exception {
        val expensive = placeOrder(SELLER, "DEMO", SELL, 103, 1);
        val first = placeOrder(SELLER, "DEMO", SELL, 100, 1);
        val second = placeOrder(SELLER, "DEMO", SELL, 100, 1);
        val bid = placeOrder(BUYER, "DEMO", BUY, 95, 2);

        val amendment = new AmendOrderRequest(BigDecimal.valueOf(105), null);
        val request = request(orderPath(bid.path("id").asLong()), BUYER)
                .header("Content-Type", "application/json")
                .method("PATCH", jsonBody(amendment))
                .build();
        val amended = exchange(request, 200);

        assertThat(amended.path("status").asText())
                .isEqualTo("FILLED");
        val trades = get("/api/trades?seccode=DEMO").path("items");
        assertThat(trades)
                .hasSize(2);
        assertThat(trades.get(0).path("sellOrderId").asLong())
                .isEqualTo(second.path("id").asLong());
        assertThat(trades.get(1).path("sellOrderId").asLong())
                .isEqualTo(first.path("id").asLong());

        val remaining = get("/api/orders?status=OPEN", SELLER).path("items");
        assertThat(remaining)
                .hasSize(1);
        assertThat(remaining.get(0).path("id").asLong())
                .isEqualTo(expensive.path("id").asLong());
        assertBalance(BUYER, "RUB", "8000", "0");
    }

    @Test
    void rollsBackOrderAndBalancesIfPostgresRejectsTrade() throws Exception {
        val ask = placeOrder(SELLER, "DEMO", SELL, 100, 1);
        jdbc.execute("ALTER TABLE trade ADD CONSTRAINT reject_trade_for_test CHECK (quantity = 0)");

        try {
            exchange(placeOrderRequest(BUYER, "DEMO", BUY, 105, 1), 500);
        } finally {
            jdbc.execute("ALTER TABLE trade DROP CONSTRAINT reject_trade_for_test");
        }

        val askAfterRollback = get(orderPath(ask.path("id").asLong()), SELLER);
        assertThat(askAfterRollback.path("status").asText())
                .isEqualTo("OPEN");
        assertThat(askAfterRollback.path("filled").asInt())
                .isZero();
        assertThat(get(ORDERS, BUYER).path("items"))
                .isEmpty();
        assertThat(get("/api/trades").path("items"))
                .isEmpty();
        assertBalance(BUYER, "RUB", "10000", "0");
        assertBalance(SELLER, "DEMO", "90", "10");
    }

    @Test
    void reservesSharedCashOnlyOnceAcrossTwoInstruments() throws Exception {
        createInstrument("DEMO2");

        val responses = competeAtDatabaseLock(
                "SELECT 1 FROM trading_account WHERE user_id = ? FOR UPDATE",
                BUYER,
                "%trading_account%",
                placeOrderRequest(BUYER, "DEMO", BUY, 750, 1),
                placeOrderRequest(BUYER, "DEMO2", BUY, 750, 1)
        );

        assertThat(responses)
                .extracting(HttpResponse::statusCode)
                .containsExactlyInAnyOrder(201, 409);
        val rejected = responses.stream()
                .filter(response -> response.statusCode() == 409)
                .findFirst()
                .orElseThrow();
        assertThat(json.readTree(rejected.body()).path("code").asText())
                .isEqualTo("INSUFFICIENT_BALANCE");
        assertBalance(BUYER, "RUB", "2500", "7500");
        assertThat(get(ORDERS, BUYER).path("items"))
                .hasSize(1);
    }

    @Test
    void cancellationRacingExecutionLeavesConsistentOrderTradeAndReserves() throws Exception {
        val ask = placeOrder(SELLER, "DEMO", SELL, 100, 1);
        val askId = ask.path("id").asLong();

        val responses = competeAtDatabaseLock(
                "SELECT 1 FROM instrument WHERE seccode = ? FOR UPDATE",
                "DEMO",
                "%instrument%",
                cancelOrderRequest(SELLER, askId),
                placeOrderRequest(BUYER, "DEMO", BUY, 100, 1)
        );

        val cancel = responses.get(0);
        val buy = readResponse(responses.get(1), 201);
        val finalAsk = get(orderPath(askId), SELLER);
        val trades = get("/api/trades?orderId=%s".formatted(askId)).path("items");

        if (cancel.statusCode() == 200) {
            assertThat(finalAsk.path("status").asText())
                    .isEqualTo("CANCELLED");
            assertThat(finalAsk.path("filled").asInt())
                    .isZero();
            assertThat(buy.path("status").asText())
                    .isEqualTo("OPEN");
            assertThat(trades)
                    .isEmpty();
            assertBalance(BUYER, "RUB", "9000", "1000");
            assertBalance(SELLER, "DEMO", "100", "0");
        } else {
            assertThat(readResponse(cancel, 409).path("code").asText())
                    .isEqualTo("ORDER_CLOSED");
            assertThat(finalAsk.path("status").asText())
                    .isEqualTo("FILLED");
            assertThat(buy.path("status").asText())
                    .isEqualTo("FILLED");
            assertThat(trades)
                    .hasSize(1);
            assertThat(trades.get(0).path("buyOrderId").asLong())
                    .isEqualTo(buy.path("id").asLong());
            assertThat(trades.get(0).path("quantity").asInt())
                    .isEqualTo(1);
            assertBalance(BUYER, "RUB", "9000", "0");
            assertBalance(BUYER, "DEMO", "10", "0");
            assertBalance(SELLER, "DEMO", "90", "0");
            assertBalance(SELLER, "RUB", "1000", "0");
        }
    }

    private List<HttpResponse<String>> competeAtDatabaseLock(
            @NonNull final String lockSql,
            @NonNull final Object rowId,
            @NonNull final String waitingQuery,
            @NonNull final HttpRequest firstRequest,
            @NonNull final HttpRequest secondRequest
    ) throws Exception {
        try (val connection = dataSource.getConnection();
             val lock = connection.prepareStatement(lockSql)
        ) {
            connection.setAutoCommit(false);
            lock.setObject(1, rowId);

            try (val rows = lock.executeQuery()) {
                assertThat(rows.next())
                        .isTrue();
            }

            val first = HTTP.sendAsync(firstRequest, HttpResponse.BodyHandlers.ofString());
            val second = HTTP.sendAsync(secondRequest, HttpResponse.BodyHandlers.ofString());

            await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                    assertThat(jdbc.queryForObject("""
                            SELECT COUNT(*) FROM pg_stat_activity
                            WHERE datname = current_database() AND wait_event_type = 'Lock' AND query LIKE ?
                            """, Long.class, waitingQuery)).isEqualTo(2L));
            connection.commit();

            return List.of(first.get(15, SECONDS), second.get(15, SECONDS));
        }
    }

    private void createAsset(
            @NonNull final String code,
            @NonNull final AssetType type
    ) throws IOException, InterruptedException {
        exchange(postJson("/api/assets", new CreateAssetRequest(code, type)), 201);
    }

    private void createInstrument(@NonNull final String seccode) throws IOException, InterruptedException {
        val instrument = new CreateInstrumentRequest(
                seccode,
                STOCK,
                "DEMO",
                "RUB",
                BigDecimal.TEN,
                new BigDecimal("0.01")
        );

        exchange(postJson("/api/instruments", instrument), 201);
    }

    private void openAccount(final long userId) throws IOException, InterruptedException {
        val request = request(accountPath(userId))
                .PUT(noBody())
                .build();

        exchange(request, 204);
    }

    private void depositAsset(final long userId, @NonNull final String asset, final long amount)
            throws IOException, InterruptedException {
        val deposit = new DepositRequest(asset, BigDecimal.valueOf(amount));
        exchange(postJson("%s/deposits".formatted(accountPath(userId)), deposit), 200);
    }

    private JsonNode placeOrder(
            final long userId,
            @NonNull final String seccode,
            @NonNull final Side side,
            final int price,
            final int quantity
    ) throws IOException, InterruptedException {
        return exchange(placeOrderRequest(userId, seccode, side, price, quantity), 201);
    }

    private HttpRequest placeOrderRequest(
            final long userId,
            @NonNull final String seccode,
            @NonNull final Side side,
            final int price,
            final int quantity
    ) throws JsonProcessingException {
        val order = new PlaceOrderRequest(
                seccode,
                side,
                BigDecimal.valueOf(price),
                BigDecimal.valueOf(quantity)
        );

        return request(ORDERS, userId)
                .header("Content-Type", "application/json")
                .POST(jsonBody(order))
                .build();
    }

    private HttpRequest cancelOrderRequest(final long userId, final long orderId) {
        return request(orderPath(orderId), userId)
                .DELETE()
                .build();
    }

    private static String orderPath(final long id) {
        return "%s/%s".formatted(ORDERS, id);
    }

    private static String accountPath(final long userId) {
        return "/api/accounts/%s".formatted(userId);
    }

    private void assertBalance(
            final long userId,
            @NonNull final String asset,
            @NonNull final String available,
            @NonNull final String reserved
    ) throws IOException, InterruptedException {
        for (val balance : get("/api/balances", userId)) {
            if (asset.equals(balance.path("asset").asText())) {
                assertThat(balance.path("available").decimalValue())
                        .isEqualByComparingTo(available);
                assertThat(balance.path("reserved").decimalValue())
                        .isEqualByComparingTo(reserved);
                return;
            }
        }
        throw new AssertionError("У участника %s отсутствует баланс актива %s".formatted(userId, asset));
    }

    private JsonNode get(@NonNull final String path) throws IOException, InterruptedException {
        return exchange(request(path).GET().build(), 200);
    }

    private JsonNode get(@NonNull final String path, final long userId) throws IOException, InterruptedException {
        return exchange(request(path, userId).GET().build(), 200);
    }

    private JsonNode exchange(
            @NonNull final HttpRequest request,
            final int expectedStatus
    ) throws IOException, InterruptedException {
        val response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        return readResponse(response, expectedStatus);
    }

    private JsonNode readResponse(
            @NonNull final HttpResponse<String> response,
            final int expectedStatus
    ) throws IOException {
        assertThat(response.statusCode())
                .as("%s %s: %s", response.request().method(), response.uri(), response.body())
                .isEqualTo(expectedStatus);

        return response.body().isEmpty()
                ? json.missingNode()
                : json.readTree(response.body());
    }

    private HttpRequest postJson(
            @NonNull final String path,
            @NonNull final Object body
    ) throws JsonProcessingException {
        return request(path)
                .header("Content-Type", "application/json")
                .POST(jsonBody(body))
                .build();
    }

    private HttpRequest.BodyPublisher jsonBody(@NonNull final Object body) throws JsonProcessingException {
        return HttpRequest.BodyPublishers
                .ofString(json.writeValueAsString(body));
    }

    private HttpRequest.Builder request(@NonNull final String path, final long userId) {
        return request(path)
                .header(USER_ID_HEADER, Long.toString(userId));
    }

    private HttpRequest.Builder request(@NonNull final String path) {
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:%s%s".formatted(port, path)))
                .timeout(Duration.ofSeconds(20));
    }
}
