package com.magpie;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;

public class TokensDataProvider {

    private static JSONArray getTokens(String network) {
        String url = "https://api.magpiefi.xyz/token-manager/tokens";
        Response response = RestAssured.given()
                .header("accept", "application/json")
                .header("Content-Type", "application/json")
                .body("{\n" +
                        "  \"networkNames\": [\"" + network + "\"],\n" +
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

    public static JSONObject getTokenByIndex(String network, int index) {
        JSONArray tokens = getTokens(network);
        try {
            return tokens.getJSONObject(index);
        } catch (JSONException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to get token by index", e);
        }
    }

    // Method to get the token address by index
    public static String getTokenAddressByIndex(String network, int index) {
        JSONObject token = getTokenByIndex(network, index);
        try {
            return token.getString("address");
        } catch (JSONException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to get address from token", e);
        }
    }

    // Method to get the number of decimals for a token
    private static int getDecimalsForToken(JSONObject token) {
        try {
            return token.getInt("decimals");
        } catch (JSONException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to get decimals from token", e);
        }
    }

    // Method to calculate the sell amount with the appropriate number of decimals
    public static BigDecimal getSellAmountByDecimals(String network, int index, double amount) {
        JSONObject token = getTokenByIndex(network, index);
        int decimals = getDecimalsForToken(token);

        BigDecimal tokenAmount = BigDecimal.valueOf(amount);
        BigDecimal scalingFactor = BigDecimal.TEN.pow(decimals);

        return tokenAmount.multiply(scalingFactor);
    }

}