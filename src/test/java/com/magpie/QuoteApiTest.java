package com.magpie;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class QuoteApiTest {

    private static final String BASE_URL = "https://api.magpiefi.xyz"; // API base URL
    private static final String QUOTE_ENDPOINT = "/aggregator/quote"; // The /quote endpoint

    // Утилітний метод для відправки запиту
    private Response sendQuoteRequest(String network, String fromTokenAddress, String toTokenAddress, String sellAmount, double slippage, boolean gasless) {
        return RestAssured.given()
                .queryParam("network", network)
                .queryParam("fromTokenAddress", fromTokenAddress)
                .queryParam("toTokenAddress", toTokenAddress)
                .queryParam("sellAmount", sellAmount)
                .queryParam("slippage", slippage)
                .queryParam("gasless", gasless)
                .when()
                .get(BASE_URL + QUOTE_ENDPOINT)
                .then()
                .extract().response();
    }

    // Valid Request for /quote
    @ParameterizedTest
    @CsvSource({
            "bsc, 0, 1, 0.5, false",
            "bsc, 1, 2, 0.05, false"
    })
    public void testQuote (String network, int tokenIndex, double amount, double slippage, boolean gasless) {
        String fromTokenAddress = String.valueOf(TokenDataProvider.getTokenAddressByIndex(tokenIndex));
        String toTokenAddress = String.valueOf(TokenDataProvider.getTokenAddressByIndex(tokenIndex + 1));
        BigDecimal sellAmountBigDecimal = TokenDataProvider.getSellAmountByDecimals(tokenIndex, amount);
        String sellAmount = sellAmountBigDecimal.stripTrailingZeros().toPlainString();

        // Виклик утилітного методу
        Response response = sendQuoteRequest(network, fromTokenAddress, toTokenAddress, sellAmount, slippage, gasless);

        // Validate the response status
        assertEquals(200, response.getStatusCode(), "Expected status code 200 OK");

        // Validate the response content type (it should be JSON)
        assertTrue(response.getContentType().contains("application/json"), "Expected content type to contain application/json");

        // Print the response body for debugging purposes
        System.out.println("Response Body: " + response.getBody().asString());

        // Check if the response contains the 'amountOut' key (a valid response key)
        boolean containsAmountOut = response.jsonPath().get("amountOut") != null;
        assertTrue(containsAmountOut, "Response should contain 'amountOut' key");

        // Check if the 'targetAddress' key exists in the response (another valid key)
        boolean containsTargetAddress = response.jsonPath().get("targetAddress") != null;
        assertTrue(containsTargetAddress, "Response should contain 'targetAddress' key");

        // Сheck for the 'fees' array or other relevant data
        boolean containsFees = response.jsonPath().get("fees") != null;
        assertTrue(containsFees, "Response should contain 'fees' array");
    }

    // Missing Amount Test for /quote
    @ParameterizedTest
    @CsvSource({
            "bsc, 0, 0, 0.5, false",  // Значення 0 для sellAmount
            "bsc, 1, 0, 0.05, false"  // Значення 0 для sellAmount
    })
    public void testQuoteWithMissingAmount(String network, int tokenIndex, int amount, double slippage, boolean gasless) {
        String fromTokenAddress = String.valueOf(TokenDataProvider.getTokenAddressByIndex(tokenIndex));
        String toTokenAddress = String.valueOf(TokenDataProvider.getTokenAddressByIndex(tokenIndex + 1));

        // Виклик утилітного методу з нульовим значенням sellAmount
        Response response = sendQuoteRequest(network, fromTokenAddress, toTokenAddress, String.valueOf(amount), slippage, gasless);

        // Перевірка, що статус відповіді є 2069 або інший код помилки, що вказує на відсутність маршруту
        assertEquals(2069, response.jsonPath().getInt("code"), "Expected error code 2069 indicating no routes found");

        // Перевірка, що контент відповіді є JSON
        assertTrue(response.getContentType().contains("application/json"), "Expected content type to contain application/json");

        // Виведення тіла відповіді для відлагодження
        System.out.println("Response Body: " + response.getBody().asString());

        // Перевірка наявності повідомлення, яке вказує на неможливість знайти маршрути для обміну
        String responseBody = response.getBody().asString();
        assertTrue(responseBody.contains("We are unable to find routes to execute your swap based on your amountIn"),
                "Response should contain an error message indicating that routes cannot be found due to amountIn.");
    }

    // Missing Amount Test for /quote
    @ParameterizedTest
    @CsvSource({
            "unknownNetwork, 0, 1, 0.5, false",  //
            "unknownNetwork, 1, 2, 0.05, false"  //
    })
    public void testQuoteWithInvalidNetwork(String network, int tokenIndex, double amount, double slippage, boolean gasless) {
        String fromTokenAddress = String.valueOf(TokenDataProvider.getTokenAddressByIndex(tokenIndex));
        String toTokenAddress = String.valueOf(TokenDataProvider.getTokenAddressByIndex(tokenIndex + 1));
        BigDecimal sellAmountBigDecimal = TokenDataProvider.getSellAmountByDecimals(tokenIndex, amount);
        String sellAmount = sellAmountBigDecimal.stripTrailingZeros().toPlainString();

        // Виклик утилітного методу
        Response response = sendQuoteRequest(network, fromTokenAddress, toTokenAddress, sellAmount, slippage, gasless);

        assertEquals(400, response.getStatusCode(), "Expected status code 400 Bad Request");
        assertTrue(response.getBody().asString().contains("network is not valid"), "Response should contain an error message indicating invalid network.");
    }





}
