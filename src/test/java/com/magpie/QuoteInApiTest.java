package com.magpie;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class QuoteInApiTest {
    private static final String BASE_URL = "https://api.magpiefi.xyz";
    private static final String QUOTE_ENDPOINT = "/aggregator/quote-in";

    // Logger
    private static final Logger logger = LoggerFactory.getLogger(QuoteInApiTest.class);

    private Response sendQuoteInRequest(String fromNetwork, String toNetwork,
                                        String fromTokenAddress, String toTokenAddress,
                                        String sellAmount, double slippageIn,
                                        double slippageOut, boolean gasless, int bridgeType) {
        logger.info("Sending request with parameters: fromNetwork={}, toNetwork={}, fromTokenAddress={}, toTokenAddress={}, sellAmount={}, slippageIn={}, slippageOut={}, gasless={}, bridgeTypes={}",
                fromNetwork, toNetwork, fromTokenAddress, toTokenAddress, sellAmount, slippageIn, slippageOut, gasless, bridgeType);

        return RestAssured.given()
                .queryParam("fromNetwork", fromNetwork)
                .queryParam("toNetwork", toNetwork)
                .queryParam("fromTokenAddress", fromTokenAddress)
                .queryParam("toTokenAddress", toTokenAddress)
                .queryParam("sellAmount", sellAmount)
                .queryParam("slippageIn", slippageIn)
                .queryParam("slippageOut", slippageOut)
                .queryParam("gasless", gasless)
                .queryParam("liquiditySources", "") // Added this parameter
                .queryParam("bridgeTypes", bridgeType) // Fixed parameter name
                .when()
                .get(BASE_URL + QUOTE_ENDPOINT)
                .then()
                .extract().response();
    }

    @ParameterizedTest
    @CsvSource({
            "bsc, ethereum, 0, 0, 1, 0.5, 0.05, false, 1"
    })
    public void testQuoteIn(String fromNetwork, String toNetwork, int tokenIndex1, int tokenIndex2,
                            double amount, double slippageIn, double slippageOut, boolean gasless, int bridgeType) {

        String fromTokenAddress = TokensDataProvider.getTokenAddressByIndex(fromNetwork, tokenIndex1);
        String toTokenAddress = TokensDataProvider.getTokenAddressByIndex(toNetwork, tokenIndex2);
        BigDecimal sellAmountBigDecimal = TokensDataProvider.getSellAmountByDecimals(fromNetwork,tokenIndex1, amount);
        String sellAmount = sellAmountBigDecimal.stripTrailingZeros().toPlainString();

        Response response = sendQuoteInRequest(fromNetwork, toNetwork, fromTokenAddress, toTokenAddress,
                sellAmount, slippageIn, slippageOut, gasless, bridgeType);

        logger.info("Response Status Code: {}", response.getStatusCode());

        assertEquals(200, response.getStatusCode(), "Expected status code 200 OK");
        assertTrue(response.getContentType().contains("application/json"), "Expected content type to contain application/json");

        logger.info("Response Body: {}", response.getBody().asString());

        assertTrue(response.jsonPath().getString("amountOut") != null, "Response should contain 'amountOut' key");
        assertTrue(response.jsonPath().getString("targetAddress") != null, "Response should contain 'targetAddress' key");
        assertTrue(response.jsonPath().getList("fees") != null, "Response should contain 'fees' array");
    }

    @ParameterizedTest
    @CsvSource({
            "bsc, ethereum, 0, 0, 0, 0.5, 0.05, false, 1" // Value 0 for sellAmount
    })
    public void testQuoteInWithMissingAmount(String fromNetwork, String toNetwork, int tokenIndex1, int tokenIndex2,
                                             double amount, double slippageIn, double slippageOut, boolean gasless, int bridgeType) {
        String fromTokenAddress = TokensDataProvider.getTokenAddressByIndex(fromNetwork, tokenIndex1);
        String toTokenAddress = TokensDataProvider.getTokenAddressByIndex(toNetwork, tokenIndex2);

        // Ensure `sellAmount` is passed as "0", not "0.0"
        String sellAmount = String.format("%.0f", amount);

        // Call API
        Response response = sendQuoteInRequest(fromNetwork, toNetwork, fromTokenAddress, toTokenAddress,
                sellAmount, slippageIn, slippageOut, gasless, bridgeType);

        logger.info("Response Status Code: {}", response.getStatusCode());
        logger.info("Response Body: {}", response.getBody().asString());

        // Check API status code (manually verify the actual returned code)
        int actualCode = response.jsonPath().getInt("code");
        assertEquals(2121, actualCode, "Expected error code 2121 indicating no routes found");

        // Ensure response is in JSON format
        assertTrue(response.getContentType().contains("application/json"), "Expected content type to contain application/json");

        // Check if correct error message is returned
        String responseBody = response.getBody().asString();
        assertTrue(responseBody.contains("The minimum amount has to be greater than ~$0.0000"),
                "Response should contain an error message indicating that routes cannot be found due to amountIn.");
    }

    @ParameterizedTest
    @CsvSource({
            "bsc, ethereum, 0, 0, 100000000000000000000000000000000000, 0.5, 0.05, true, 1"
    })
    public void testQuoteInWithMaxAmount(String fromNetwork, String toNetwork, int tokenIndex1, int tokenIndex2,
                                         BigDecimal amount, double slippageIn, double slippageOut, boolean gasless, int bridgeType) {
        String fromTokenAddress = TokensDataProvider.getTokenAddressByIndex(fromNetwork, tokenIndex1);
        String toTokenAddress = TokensDataProvider.getTokenAddressByIndex(toNetwork, tokenIndex2);

        // Convert sellAmount value to string format without exponential notation
        String sellAmount = amount.toPlainString();

        Response response = sendQuoteInRequest(fromNetwork, toNetwork, fromTokenAddress, toTokenAddress,
                sellAmount, slippageIn, slippageOut, gasless, bridgeType);

        logger.info("Response Status Code: {}", response.getStatusCode());
        logger.info("Response Body: {}", response.getBody().asString());

        // Check status code (expected 400 or another error code)
        int actualStatusCode = response.getStatusCode();
        assertEquals(400, actualStatusCode, "Expected status code 400 Bad Request due to exceeding amount limit");

        // Ensure response contains the correct error message for exceeding the limit
        String responseBody = response.getBody().asString();
        assertTrue(responseBody.contains("The maximum amount has to be less than"),
                "Response should contain an error message indicating amount exceeds the limit.");
    }
}
