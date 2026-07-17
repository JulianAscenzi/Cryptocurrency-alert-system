package com.cryptoalert.application;

import com.cryptoalert.domain.model.AlertCondition;
import com.cryptoalert.domain.model.AlertStatus;
import com.cryptoalert.domain.model.CryptoAlert;
import com.cryptoalert.domain.model.PriceRecord;
import com.cryptoalert.domain.repository.CryptoAlertRepository;
import com.cryptoalert.infrastructure.websocket.BinanceWebSocketPriceFeed;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertEvaluationServiceTest {

    @Test
    void shouldTriggerAndPersistMatchingAlertWhenPriceArrives() throws Exception {
        InMemoryCryptoAlertRepository repository = new InMemoryCryptoAlertRepository();
        TrackingNotificationService notificationService = new TrackingNotificationService();
        CryptoAlert alert = CryptoAlert.createNew("BTCUSDT", new BigDecimal("1000"), AlertCondition.ABOVE);
        repository.save(alert).await().indefinitely();

        AlertEvaluationService service = new AlertEvaluationService();
        service.priceFeed = new TestPriceFeed(new PriceRecord("BTCUSDT", new BigDecimal("1001"), Instant.now()));
        service.repository = repository;
        service.notificationService = notificationService;

        service.subscribeToPriceStream();

        assertTrue(notificationService.await(2, TimeUnit.SECONDS));
        assertEquals(AlertStatus.TRIGGERED, alert.getStatus());
        assertEquals(1, repository.updatedAlerts.size());
        assertEquals("BTCUSDT", notificationService.lastSymbol);
    }

    @Test
    void shouldContinueProcessingOtherAlertsWhenOneEvaluationFails() throws Exception {
        InMemoryCryptoAlertRepository repository = new InMemoryCryptoAlertRepository();
        TrackingNotificationService notificationService = new TrackingNotificationService();
        CryptoAlert workingAlert = CryptoAlert.createNew("BTCUSDT", new BigDecimal("1000"), AlertCondition.ABOVE);
        CryptoAlert failingAlert = new FailingCryptoAlert(UUID.randomUUID(), "BTCUSDT", new BigDecimal("1000"), AlertCondition.ABOVE, AlertStatus.ACTIVE, Instant.now(), null);
        repository.save(workingAlert).await().indefinitely();
        repository.save(failingAlert).await().indefinitely();

        AlertEvaluationService service = new AlertEvaluationService();
        service.priceFeed = new TestPriceFeed(new PriceRecord("BTCUSDT", new BigDecimal("1001"), Instant.now()));
        service.repository = repository;
        service.notificationService = notificationService;

        service.processPriceRecord(new PriceRecord("BTCUSDT", new BigDecimal("1001"), Instant.now())).await().indefinitely();

        assertEquals(AlertStatus.TRIGGERED, workingAlert.getStatus());
        assertEquals(1, repository.updatedAlerts.size());
        assertEquals(1, notificationService.callCount.get());
    }

    private static final class TestPriceFeed extends BinanceWebSocketPriceFeed {
        private final Multi<PriceRecord> prices;

        private TestPriceFeed(PriceRecord... prices) {
            this.prices = Multi.createFrom().iterable(List.of(prices));
        }

        @Override
        public Multi<PriceRecord> priceStream() {
            return prices;
        }
    }

    private static final class InMemoryCryptoAlertRepository implements CryptoAlertRepository {
        private final List<CryptoAlert> alerts = new ArrayList<>();
        private final List<CryptoAlert> updatedAlerts = new ArrayList<>();

        @Override
        public Uni<CryptoAlert> save(CryptoAlert alert) {
            alerts.add(alert);
            return Uni.createFrom().item(alert);
        }

        @Override
        public Uni<CryptoAlert> findById(UUID id) {
            return Uni.createFrom().item(alerts.stream().filter(alert -> alert.getId().equals(id)).findFirst().orElse(null));
        }

        @Override
        public Multi<CryptoAlert> findAllActive() {
            return Multi.createFrom().iterable(alerts.stream().filter(alert -> alert.getStatus() == AlertStatus.ACTIVE).toList());
        }

        @Override
        public Multi<CryptoAlert> findActiveBySymbol(String symbol) {
            List<CryptoAlert> matches = alerts.stream()
                    .filter(alert -> alert.getStatus() == AlertStatus.ACTIVE && alert.getSymbol().equalsIgnoreCase(symbol))
                    .toList();
            return Multi.createFrom().iterable(matches);
        }

        @Override
        public Uni<Void> update(CryptoAlert alert) {
            updatedAlerts.add(alert);
            return Uni.createFrom().voidItem();
        }
    }

    private static final class TrackingNotificationService extends NotificationService {
        private final CountDownLatch latch = new CountDownLatch(1);
        private final AtomicInteger callCount = new AtomicInteger();
        private volatile String lastSymbol;

        @Override
        public void notifyTriggeredAlert(CryptoAlert alert, PriceRecord priceRecord) {
            super.notifyTriggeredAlert(alert, priceRecord);
            callCount.incrementAndGet();
            lastSymbol = alert != null ? alert.getSymbol() : priceRecord != null ? priceRecord.symbol() : null;
            latch.countDown();
        }

        private boolean await(long timeout, TimeUnit unit) throws InterruptedException {
            return latch.await(timeout, unit);
        }
    }

    private static final class FailingCryptoAlert extends CryptoAlert {
        private FailingCryptoAlert(UUID id, String symbol, BigDecimal targetPrice, AlertCondition condition, AlertStatus status, Instant createdAt, Instant triggeredAt) {
            super(id, symbol, targetPrice, condition, status, createdAt, triggeredAt);
        }

        @Override
        public boolean checkTriggerCondition(PriceRecord priceRecord) {
            throw new IllegalStateException("boom");
        }
    }
}
