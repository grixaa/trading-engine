CREATE TABLE asset (
    code varchar(32) PRIMARY KEY CHECK (code ~ '^[A-Z0-9][A-Z0-9._-]{0,31}$'),
    type text NOT NULL CHECK (type IN ('CURRENCY', 'SECURITY'))
);

CREATE TABLE instrument (
    seccode varchar(32) PRIMARY KEY CHECK (seccode ~ '^[A-Z0-9][A-Z0-9._-]{0,31}$'),
    type text NOT NULL CHECK (type IN ('STOCK', 'BOND', 'FX')),
    base_asset varchar(32) NOT NULL REFERENCES asset (code),
    quote_currency varchar(32) NOT NULL REFERENCES asset (code),
    lot_size integer NOT NULL CHECK (lot_size > 0),
    min_step numeric(20, 8) NOT NULL CHECK (min_step > 0 AND min_step <> 'NaN'::numeric),
    CHECK (base_asset <> quote_currency)
);

CREATE TABLE trading_account (
    user_id bigint PRIMARY KEY CHECK (user_id > 0)
);

CREATE TABLE asset_balance (
    user_id bigint NOT NULL REFERENCES trading_account (user_id),
    asset varchar(32) NOT NULL REFERENCES asset (code),
    available numeric NOT NULL CHECK (available >= 0 AND available < 'Infinity'::numeric),
    reserved numeric NOT NULL CHECK (reserved >= 0 AND reserved < 'Infinity'::numeric),
    PRIMARY KEY (user_id, asset)
);

CREATE SEQUENCE order_priority_seq AS bigint;

CREATE TABLE trade_order (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES trading_account (user_id),
    seccode varchar(32) NOT NULL REFERENCES instrument (seccode),
    side text NOT NULL CHECK (side IN ('BUY', 'SELL')),
    price numeric(20, 8) NOT NULL CHECK (price > 0 AND price <> 'NaN'::numeric),
    quantity integer NOT NULL CHECK (quantity > 0),
    filled integer NOT NULL DEFAULT 0 CHECK (filled BETWEEN 0 AND quantity),
    status text NOT NULL CHECK (
        (status = 'OPEN' AND filled = 0)
        OR (status = 'PARTIALLY_FILLED' AND filled > 0 AND filled < quantity)
        OR (status = 'FILLED' AND filled = quantity)
        OR (status = 'CANCELLED' AND filled < quantity)
    ),
    priority bigint NOT NULL DEFAULT nextval('order_priority_seq'),
    created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    updated_at timestamptz NOT NULL DEFAULT clock_timestamp()
);

ALTER SEQUENCE order_priority_seq OWNED BY trade_order.priority;

CREATE INDEX trade_order_book_asks ON trade_order (seccode, price ASC, priority ASC)
    WHERE side = 'SELL' AND status IN ('OPEN', 'PARTIALLY_FILLED');
CREATE INDEX trade_order_book_bids ON trade_order (seccode, price DESC, priority ASC)
    WHERE side = 'BUY' AND status IN ('OPEN', 'PARTIALLY_FILLED');
CREATE INDEX trade_order_user_registry ON trade_order (user_id, id DESC);

CREATE TABLE trade (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    seccode varchar(32) NOT NULL REFERENCES instrument (seccode),
    buy_order_id bigint NOT NULL REFERENCES trade_order (id),
    sell_order_id bigint NOT NULL REFERENCES trade_order (id),
    price numeric(20, 8) NOT NULL CHECK (price > 0 AND price <> 'NaN'::numeric),
    quantity integer NOT NULL CHECK (quantity > 0),
    executed_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    CHECK (buy_order_id <> sell_order_id)
);

CREATE INDEX trade_instrument_registry ON trade (seccode, id DESC);
CREATE INDEX trade_buy_order_registry ON trade (buy_order_id, id DESC);
CREATE INDEX trade_sell_order_registry ON trade (sell_order_id, id DESC);
