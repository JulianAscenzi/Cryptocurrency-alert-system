package com.cryptoalert.infrastructure.rest;

import com.cryptoalert.application.CryptoAlertService;
import com.cryptoalert.application.dto.CreateCryptoAlertRequest;
import com.cryptoalert.domain.model.CryptoAlert;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/alerts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CryptoAlertResource {

    @Inject
    CryptoAlertService service;

    @POST
    public Uni<Response> createAlert(CreateCryptoAlertRequest request) {
        if (request == null || request.symbol() == null || request.targetPrice() == null || request.condition() == null) {
            return Uni.createFrom().failure(new BadRequestException("symbol, targetPrice and condition are required"));
        }

        return service.createAlert(request.symbol(), request.targetPrice(), request.condition())
                .map(alert -> Response.status(Response.Status.CREATED).entity(alert).build());
    }

    @GET
    public Uni<List<CryptoAlert>> listActiveAlerts() {
        return service.listActiveAlerts();
    }

    @GET
    @Path("{id}")
    public Uni<CryptoAlert> getAlert(@PathParam("id") String id) {
        UUID alertId = parseUuid(id);
        return service.getAlert(alertId);
    }

    @DELETE
    @Path("{id}")
    public Uni<Response> cancelAlert(@PathParam("id") String id) {
        UUID alertId = parseUuid(id);
        return service.cancelAlert(alertId)
                .map(alert -> Response.ok(alert).build());
    }

    private UUID parseUuid(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid alert id");
        }
    }
}
