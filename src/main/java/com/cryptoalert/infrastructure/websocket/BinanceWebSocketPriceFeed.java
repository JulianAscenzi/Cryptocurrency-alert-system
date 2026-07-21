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
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Reactive Binance WebSocket price feed.
 *
 * <p>Connects to Binance's public trade stream and emits every incoming trade price
 * as a {@link PriceRecord} through a Mutiny {@link Multi}.</p>
 */
@ApplicationScoped
public class BinanceWebSocketPriceFeed {

    private static final Logger LOG = Logger.getLogger(BinanceWebSocketPriceFeed.class);
    private static final long RECONNECT_DELAY_SECONDS = 5;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MultiEmitterProcessor<PriceRecord> priceProcessor = MultiEmitterProcessor.create();
    private final ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean reconnectScheduled = new AtomicBoolean();
    private WebSocketClientConnection connection;

    @ConfigProperty(name = "crypto.binance.websocket.url", defaultValue = "wss://stream.binance.com:9443")
    String websocketUrl;

    @ConfigProperty(name = "crypto.binance.symbols", defaultValue = "BTCUSDT")
    String configuredSymbols;

    @ConfigProperty(name = "crypto.binance.enabled", defaultValue = "true")
    boolean enabled;

    public Multi<PriceRecord> priceStream() {
        return priceProcessor.toMulti();
    }

    @PostConstruct
    void start() {
        if (!enabled) {
            LOG.info("Binance websocket price feed is disabled");
            return;
        }
        connect();
    }

    @PreDestroy
    void stop() {
        reconnectExecutor.shutdownNow();
    }

    void connect() {
        URI baseUri = resolveBaseUri();
        String streamPath = buildCombinedStreamPath();
        LOG.infov("Connecting to Binance trade stream: {0}{1}", baseUri, streamPath);

        BasicWebSocketConnector.create()
                .baseUri(baseUri)
                .path(streamPath)
                .onOpen(this::handleOpen)
                .onTextMessage((connection, message) -> handleTextMessage(message))
                .onError((connection, throwable) -> handleError(throwable))
                .onClose((connection, closeReason) -> handleClose(closeReason))
                .connect()
                .subscribe().with(this::handleConnected, this::handleStartupFailure);
    }

    URI resolveBaseUri() {
        URI configuredUri = URI.create(websocketUrl);
        return URI.create(configuredUri.getScheme() + "://" + configuredUri.getAuthority());
    }

    String buildCombinedStreamPath() {
        return "/stream?streams=" + String.join("/", configuredSymbols().stream()
                .map(symbol -> symbol.toLowerCase(Locale.ROOT) + "@trade")
                .toList());
    }

    List<String> configuredSymbols() {
        List<String> symbols = Arrays.stream(configuredSymbols.split(","))
                .map(String::trim)
                .filter(symbol -> !symbol.isBlank())
                .map(symbol -> symbol.toUpperCase(Locale.ROOT))
                .distinct()
                .toList();

        if (symbols.isEmpty()) {
            throw new IllegalStateException("At least one Binance symbol must be configured");
        }
        return symbols;
    }

    private void handleConnected(WebSocketClientConnection connection) {
        this.connection = connection;
        reconnectScheduled.set(false);
        LOG.infov("Connected to Binance trade stream with connection id {0}", connection.id());
    }

    private void handleOpen(WebSocketClientConnection connection) {
        LOG.infov("Binance trade stream opened: clientId={0}", connection.clientId());
    }

    void handleTextMessage(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            PriceRecord record = parseTradeMessage(root);
            priceProcessor.emit(record);
        } catch (Exception exception) {
            LOG.warn("Failed to parse Binance websocket message", exception);
        }
    }

    private void handleError(Throwable throwable) {
        LOG.error("WebSocket error while consuming Binance prices", throwable);
        scheduleReconnect();
    }

    private void handleClose(CloseReason closeReason) {
        LOG.infov("Binance trade stream closed: {0}", closeReason);
        scheduleReconnect();
    }

    private void handleStartupFailure(Throwable throwable) {
        LOG.error("Failed to connect to Binance trade stream", throwable);
        scheduleReconnect();
    }

    PriceRecord parseTradeMessage(String message) throws Exception {
        return parseTradeMessage(objectMapper.readTree(message));
    }

    private PriceRecord parseTradeMessage(JsonNode root) {
        JsonNode trade = root.hasNonNull("data") ? root.path("data") : root;
        String symbol = trade.path("s").asText().trim().toUpperCase(Locale.ROOT);
        BigDecimal price = new BigDecimal(trade.path("p").asText());
        long timestamp = trade.path("E").asLong(trade.path("T").asLong(Instant.now().toEpochMilli()));
        return new PriceRecord(symbol, price, Instant.ofEpochMilli(timestamp));
    }

    private void scheduleReconnect() {
        connection = null;
        if (!reconnectScheduled.compareAndSet(false, true)) {
            return;
        }

        LOG.infov("Scheduling Binance reconnect attempt in {0} seconds", RECONNECT_DELAY_SECONDS);
        reconnectExecutor.schedule(() -> {
            reconnectScheduled.set(false);
            connect();
        }, RECONNECT_DELAY_SECONDS, TimeUnit.SECONDS);
    }
}
