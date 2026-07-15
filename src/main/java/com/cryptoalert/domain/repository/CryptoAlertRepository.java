package com.cryptoalert.domain.repository;

import com.cryptoalert.domain.model.CryptoAlert;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import java.util.UUID;

/**
 * Domain Repository interface defining the reactive persistence contract for CryptoAlert entities.
 * Decoupled from any framework-specific persistence library.
 */
public interface CryptoAlertRepository {
    
    Uni<CryptoAlert> save(CryptoAlert alert);
    
    Uni<CryptoAlert> findById(UUID id);
    
    Multi<CryptoAlert> findAllActive();
    
    Multi<CryptoAlert> findActiveBySymbol(String symbol);
    
    Uni<Void> update(CryptoAlert alert);
}
