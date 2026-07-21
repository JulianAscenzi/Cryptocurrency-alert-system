package com.cryptoalert.infrastructure.websocket;

import com.cryptoalert.domain.model.PriceRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BinanceWebSocketPriceFeedTest {

    @Test
    void configuredSymbolsProduceCombinedStreamPath() {
        BinanceWebSocketPriceFeed feed = new BinanceWebSocketPriceFeed();
        feed.websocketUrl = "wss://stream.binance.com:9443/ws";
        feed.configuredSymbols = " BTCUSDT, ethusdt, BTCUSDT ";

        assertEquals("wss://stream.binance.com:9443", feed.resolveBaseUri().toString());
        assertEquals("/stream?streams=btcusdt@trade/ethusdt@trade", feed.buildCombinedStreamPath());
    }

    @Test
    void parsedTradeMessageEmitsNormalizedPriceRecord() throws Exception {
        BinanceWebSocketPriceFeed feed = new BinanceWebSocketPriceFeed();

        PriceRecord record = feed.parseTradeMessage("""
                {
                  "stream": "btcusdt@trade",
                  "data": {
                    "s": "btcusdt",
                    "p": "42100.50",
                    "E": 1704067200000
                  }
                }
                """);

        assertEquals("BTCUSDT", record.symbol());
        assertEquals(new BigDecimal("42100.50"), record.price());
        assertEquals(Instant.parse("2024-01-01T00:00:00Z"), record.timestamp());
    }

    @Test
    void malformedMessageDoesNotKillFutureProcessing() {
        BinanceWebSocketPriceFeed feed = new BinanceWebSocketPriceFeed();
        List<PriceRecord> records = new ArrayList<>();
        feed.priceStream().subscribe().with(records::add);

        feed.handleTextMessage("{\"data\":{\"s\":\"BTCUSDT\"}}");
        feed.handleTextMessage("""
                {
                  "data": {
                    "s": "ETHUSDT",
                    "p": "3000.00",
                    "E": 1704067200000
                  }
                }
                """);

        assertEquals(1, records.size());
        assertEquals("ETHUSDT", records.get(0).symbol());
    }
}
