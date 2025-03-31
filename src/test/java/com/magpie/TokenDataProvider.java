package com.magpie;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;

public class TokenDataProvider {
    private static JSONArray getTokens() {
        String url = "https://api.magpiefi.xyz/token-manager/tokens";
        Response response = RestAssured.given()
                .header("accept", "application/json")
                .header("Content-Type", "application/json")
                .body("{\n" +
                        "  \"networkNames\": [\n" +
                        "    \"bsc\"\n" +
                        "  ],\n" +
                        "  \"searchValue\": [\"\"],\n" +
                        "  \"exact\": false,\n" +
                        "  \"offset\": 0,\n" +
                        "  \"exclude\": []\n" +
                        "}")
                .post(url);

        if (response.statusCode() == 200) {
            try {
                return new JSONArray(response.getBody().asString());
            } catch (JSONException e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to parse response JSON", e);
            }
        } else {
            throw new RuntimeException("Request failed with status code: " + response.statusCode());
        }
    }

    public static JSONObject getTokenByIndex(int index) {
        JSONArray tokens = getTokens();
        try {
            return tokens.getJSONObject(index);
        } catch (JSONException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to get token by index", e);
        }
    }

    // Method to get the token address by index
    public static String getTokenAddressByIndex(int index) {
        JSONArray tokens = getTokens();
        try {
            JSONObject token = tokens.getJSONObject(index);
            return token.getString("address"); // Returns only the token address
        } catch (JSONException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to get address from token", e);
        }
    }

    // Method to get the number of decimal places for a token
    private static int getDecimalsForToken(JSONObject token) {
        try {
            return token.getInt("decimals");
        } catch (JSONException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to get decimals from token", e);
        }
    }

    // Method to calculate the sell amount with the appropriate number of decimal places
    public static BigDecimal getSellAmountByDecimals(int index, double amount) {
        JSONObject token = getTokenByIndex(index);
        int decimals = getDecimalsForToken(token);

        // Creating token amount considering the number of decimal places
        BigDecimal tokenAmount = BigDecimal.valueOf(amount);
        BigDecimal scalingFactor = BigDecimal.TEN.pow(decimals);

        return tokenAmount.multiply(scalingFactor);
    }

}