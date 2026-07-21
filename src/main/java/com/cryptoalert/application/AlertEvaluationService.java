package com.cryptoalert.application;

import com.cryptoalert.domain.model.CryptoAlert;
import com.cryptoalert.domain.model.PriceRecord;
import com.cryptoalert.domain.repository.CryptoAlertRepository;
import com.cryptoalert.infrastructure.websocket.BinanceWebSocketPriceFeed;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@ApplicationScoped
public class AlertEvaluationService {

    private static final Logger LOG = LoggerFactory.getLogger(AlertEvaluationService.class);

    @Inject
    BinanceWebSocketPriceFeed priceFeed;

    @Inject
    CryptoAlertRepository repository;

    @Inject
    NotificationService notificationService;

    @PostConstruct
    void subscribeToPriceStream() {
        priceFeed.priceStream()
                .onItem().invoke(priceRecord -> processPriceRecord(priceRecord)
                        .subscribe().with(
                                ignored -> {
                                },
                                failure -> LOG.warn("Failed to process price record {}", priceRecord, failure)
                        ))
                .onFailure().invoke(failure -> LOG.error("Alert evaluation stream stopped unexpectedly", failure))
                .subscribe().with(
                        ignored -> {
                        },
                        failure -> LOG.error("Alert evaluation stream stopped unexpectedly", failure)
                );
    }

    Uni<Void> processPriceRecord(PriceRecord priceRecord) {
        String symbol = resolveSymbol(priceRecord);
        if (symbol == null || symbol.isBlank()) {
            return Uni.createFrom().voidItem();
        }

        return findActiveAlertsBySymbol(symbol)
                .flatMap(alerts -> evaluateAlerts(alerts, priceRecord));
    }

    private Uni<Void> evaluateAlerts(List<CryptoAlert> alerts, PriceRecord priceRecord) {
        if (alerts == null || alerts.isEmpty()) {
            LOG.info("No active alerts found for symbol {}", resolveSymbol(priceRecord));
            return Uni.createFrom().voidItem();
        }

        return Multi.createFrom().iterable(alerts)
                .onItem().transformToUniAndMerge(alert -> evaluateAlert(alert, priceRecord)
                        .onFailure().invoke(failure -> LOG.warn(
                                "Failed to process alert {} for price record {}",
                                alert.getId(),
                                priceRecord,
                                failure
                        ))
                        .onFailure().recoverWithNull())
                .collect().asList()
                .replaceWithVoid();
    }

    private Uni<Void> evaluateAlert(CryptoAlert alert, PriceRecord priceRecord) {
        return Uni.createFrom().voidItem()
                .onItem().transformToUni(ignored -> {
                    try {
                        if (alert.checkTriggerCondition(priceRecord)) {
                            alert.trigger(Instant.now());
                            return repository.update(alert)
                                    .onFailure().invoke(failure -> LOG.warn(
                                            "Failed to persist triggered alert {}",
                                            alert.getId(),
                                            failure
                                    ))
                                    .onItem().invoke(() -> notificationService.notifyTriggeredAlert(alert, priceRecord));
                        }
                    } catch (Exception ex) {
                        LOG.warn("Failed to evaluate alert {} for symbol {}", alert, resolveSymbol(priceRecord), ex);
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    private Uni<List<CryptoAlert>> findActiveAlertsBySymbol(String symbol) {
        return repository.findActiveBySymbol(symbol)
                .collect().asList()
                .onItem().transform(this::normalizeAlerts);
    }

    private List<CryptoAlert> normalizeAlerts(Object value) {
        if (value instanceof Collection<?> collection) {
            List<CryptoAlert> alerts = new ArrayList<>();
            for (Object item : collection) {
                if (item instanceof CryptoAlert alert) {
                    alerts.add(alert);
                }
            }
            return alerts;
        }

        if (value instanceof Iterable<?> iterable) {
            List<CryptoAlert> alerts = new ArrayList<>();
            for (Object item : iterable) {
                if (item instanceof CryptoAlert alert) {
                    alerts.add(alert);
                }
            }
            return alerts;
        }

        return List.of();
    }

    private String resolveSymbol(Object target) {
        if (target instanceof PriceRecord priceRecord) {
            return priceRecord.symbol();
        }
        if (target instanceof CryptoAlert alert) {
            return alert.getSymbol();
        }
        return null;
    }
}
