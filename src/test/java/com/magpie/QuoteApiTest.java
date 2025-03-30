package com.magpie;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    // Create logger
    private static final Logger logger = LoggerFactory.getLogger(QuoteApiTest.class);

    // Utility method for sending a request
    private Response sendQuoteRequest(String network, String fromTokenAddress, String toTokenAddress, String sellAmount, double slippage, boolean gasless) {
        logger.info("Sending request with parameters: network={}, fromTokenAddress={}, toTokenAddress={}, sellAmount={}, slippage={}, gasless={}",
                network, fromTokenAddress, toTokenAddress, sellAmount, slippage, gasless);
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

        // Call the utility method
        Response response = sendQuoteRequest(network, fromTokenAddress, toTokenAddress, sellAmount, slippage, gasless);

        logger.info("Response Status Code: {}", response.getStatusCode());

        // Validate the response status
        assertEquals(200, response.getStatusCode(), "Expected status code 200 OK");

        // Validate the response content type (it should be JSON)
        assertTrue(response.getContentType().contains("application/json"), "Expected content type to contain application/json");

        logger.info("Response Body: {}", response.getBody().asString());

        // Check if the response contains the 'amountOut' key (a valid response key)
        boolean containsAmountOut = response.jsonPath().get("amountOut") != null;
        assertTrue(containsAmountOut, "Response should contain 'amountOut' key");

        // Check if the 'targetAddress' key exists in the response (another valid key)
        boolean containsTargetAddress = response.jsonPath().get("targetAddress") != null;
        assertTrue(containsTargetAddress, "Response should contain 'targetAddress' key");

        // Check for the 'fees' array or other relevant data
        boolean containsFees = response.jsonPath().get("fees") != null;
        assertTrue(containsFees, "Response should contain 'fees' array");
    }

    // Missing Amount Test for /quote
    @ParameterizedTest
    @CsvSource({
            "bsc, 0, 0, 0.5, false",  // Value 0 for sellAmount
            "bsc, 1, 0, 0.05, false"  // Value 0 for sellAmount
    })
    public void testQuoteWithMissingAmount(String network, int tokenIndex, int amount, double slippage, boolean gasless) {
        String fromTokenAddress = String.valueOf(TokenDataProvider.getTokenAddressByIndex(tokenIndex));
        String toTokenAddress = String.valueOf(TokenDataProvider.getTokenAddressByIndex(tokenIndex + 1));

        // Call the utility method with a zero sellAmount value
        Response response = sendQuoteRequest(network, fromTokenAddress, toTokenAddress, String.valueOf(amount), slippage, gasless);

        logger.info("Response Status Code: {}", response.getStatusCode());
        logger.info("Response Body: {}", response.getBody().asString());

        // Verify that the response status is 2069 or another error code indicating no route found
        assertEquals(2069, response.jsonPath().getInt("code"), "Expected error code 2069 indicating no routes found");

        // Verify that the response content type is JSON
        assertTrue(response.getContentType().contains("application/json"), "Expected content type to contain application/json");

        // Verify the presence of a message indicating that no routes can be found for the exchange
        String responseBody = response.getBody().asString();
        assertTrue(responseBody.contains("We are unable to find routes to execute your swap based on your amountIn"),
                "Response should contain an error message indicating that routes cannot be found due to amountIn.");
    }

    // Invalid Network Test for /quote
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

        // Call the utility method
        Response response = sendQuoteRequest(network, fromTokenAddress, toTokenAddress, sellAmount, slippage, gasless);

        logger.info("Response Status Code: {}", response.getStatusCode());
        logger.info("Response Body: {}", response.getBody().asString());

        assertEquals(500, response.getStatusCode(), "Expected status code 500 Internal Server Error");
        String responseBody = response.getBody().asString();
        assertTrue(responseBody.contains("\"Something went wrong\""),
                "Expected response body to contain 'message: Something went wrong'. Actual response: " + responseBody);
    }
}
