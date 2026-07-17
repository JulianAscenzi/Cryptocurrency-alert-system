package com.cryptoalert.infrastructure.websocket;

import com.cryptoalert.domain.model.PriceRecord;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.websockets.next.BasicWebSocketConnector;
import io.quarkus.websockets.next.CloseReason;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.MultiEmitterProcessor;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;

/**
 * Reactive Binance WebSocket price feed.
 *
 * <p>Connects to Binance's public trade stream and emits every incoming trade price
 * as a {@link PriceRecord} through a Mutiny {@link Multi}.</p>
 */
@ApplicationScoped
public class BinanceWebSocketPriceFeed {

    private static final Logger LOG = Logger.getLogger(BinanceWebSocketPriceFeed.class);
    private static final URI BINANCE_BASE_URI = URI.create("wss://stream.binance.com:9443");
    private static final String BTCUSDT_TRADE_PATH = "/ws/btcusdt@trade";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MultiEmitterProcessor<PriceRecord> priceProcessor = MultiEmitterProcessor.create();
    private WebSocketClientConnection connection;


    public Multi<PriceRecord> priceStream() {
        return priceProcessor.toMulti();
    }

    @PostConstruct
    void start() {
        BasicWebSocketConnector.create()
                .baseUri(BINANCE_BASE_URI)
                .path(BTCUSDT_TRADE_PATH)
                .onOpen(this::handleOpen)
                .onTextMessage((connection, message) -> handleTextMessage(message))
                .onError((connection, throwable) -> handleError(throwable))
                .onClose((connection, closeReason) -> handleClose(closeReason))
                .connect()
                .subscribe().with(this::handleConnected, this::handleStartupFailure);
    }

    private void handleConnected(WebSocketClientConnection connection) {
        this.connection = connection;
        LOG.infov("Connected to Binance trade stream with connection id {0}", connection.id());
    }

    private void handleOpen(WebSocketClientConnection connection) {
        LOG.infov("Binance trade stream opened: clientId={0}", connection.clientId());
    }

    private void handleTextMessage(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String symbol = root.path("s").asText();
            BigDecimal price = new BigDecimal(root.path("p").asText("0"));
            long timestamp = root.path("E").asLong(root.path("T").asLong(Instant.now().toEpochMilli()));
            PriceRecord record = new PriceRecord(symbol, price, Instant.ofEpochMilli(timestamp));
            priceProcessor.emit(record);
        } catch (Exception exception) {
            LOG.error("Failed to parse Binance websocket message", exception);
            priceProcessor.fail(exception);
        }
    }

    private void handleError(Throwable throwable) {
        LOG.error("WebSocket error while consuming Binance prices", throwable);
        priceProcessor.fail(throwable);
    }

    private void handleClose(CloseReason closeReason) {
        LOG.infov("Binance trade stream closed: {0}", closeReason);
        priceProcessor.complete();
    }

    private void handleStartupFailure(Throwable throwable) {
        LOG.error("Failed to connect to Binance trade stream", throwable);
        priceProcessor.fail(throwable);
    }
}
