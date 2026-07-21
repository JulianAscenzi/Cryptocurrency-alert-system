package com.cryptoalert.infrastructure.rest;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
class CryptoAlertResourceTest {

    @Test
    void createAlertReturnsCreatedAlert() {
        given()
                .contentType("application/json")
                .body("""
                        {
                          "symbol": " btcusdt ",
                          "targetPrice": 50000,
                          "condition": "ABOVE"
                        }
                        """)
                .when()
                .post("/alerts")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("symbol", equalTo("BTCUSDT"))
                .body("status", equalTo("ACTIVE"));
    }

    @Test
    void createAlertRejectsBlankSymbol() {
        given()
                .contentType("application/json")
                .body("""
                        {
                          "symbol": " ",
                          "targetPrice": 50000,
                          "condition": "ABOVE"
                        }
                        """)
                .when()
                .post("/alerts")
                .then()
                .statusCode(400);
    }

    @Test
    void createAlertRejectsZeroPrice() {
        given()
                .contentType("application/json")
                .body("""
                        {
                          "symbol": "BTCUSDT",
                          "targetPrice": 0,
                          "condition": "ABOVE"
                        }
                        """)
                .when()
                .post("/alerts")
                .then()
                .statusCode(400);
    }

    @Test
    void createAlertRejectsNegativePrice() {
        given()
                .contentType("application/json")
                .body("""
                        {
                          "symbol": "BTCUSDT",
                          "targetPrice": -1,
                          "condition": "ABOVE"
                        }
                        """)
                .when()
                .post("/alerts")
                .then()
                .statusCode(400);
    }

    @Test
    void getAlertRejectsInvalidUuid() {
        given()
                .when()
                .get("/alerts/not-a-uuid")
                .then()
                .statusCode(400);
    }

    @Test
    void getAlertReturnsNotFoundForMissingAlert() {
        given()
                .when()
                .get("/alerts/123e4567-e89b-12d3-a456-426614174000")
                .then()
                .statusCode(404);
    }
}
