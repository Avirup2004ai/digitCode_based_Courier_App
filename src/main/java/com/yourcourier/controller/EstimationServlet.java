package com.yourcourier.controller; // Or your chosen package

import com.fasterxml.jackson.databind.JsonNode; // For reading JSON
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourcourier.service.DigiPinService; // You might need this for coordinate conversion or validation

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
// Import your Coordinates class if you use it here, e.g.:
// import com.yourcourier.service.DigiPinService.Coordinates;


@WebServlet("/api/courier/estimate")
public class EstimationServlet extends HttpServlet {

    private ObjectMapper objectMapper;
    private DigiPinService digiPinService; // If you need to decode digipins or use its coordinate logic

    @Override
    public void init() throws ServletException {
        super.init();
        this.objectMapper = new ObjectMapper();
        this.digiPinService = new DigiPinService(); // Initialize if needed
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            // Read the JSON payload from the request
            JsonNode rootNode = objectMapper.readTree(request.getReader());

            String pickupDigiPin = rootNode.path("pickupDigiPin").asText();
            String deliveryDigiPin = rootNode.path("deliveryDigiPin").asText();
            double weight = rootNode.path("weight").asDouble();
            double pickupLat = rootNode.path("pickupLat").asDouble();
            double pickupLng = rootNode.path("pickupLng").asDouble();
            double deliveryLat = rootNode.path("deliveryLat").asDouble();
            double deliveryLng = rootNode.path("deliveryLng").asDouble();

            // --- Placeholder for your actual estimation logic ---
            // You would typically:
            // 1. Potentially use digiPinService.getCoordinatesFromDigiPin() if you only relied on DigiPins,
            //    but the JS is already sending lat/lng.
            // 2. Calculate distance between (pickupLat, pickupLng) and (deliveryLat, deliveryLng).
            //    This might involve a Haversine formula or calling a mapping service.
            // 3. Calculate price based on distance and weight.

            // Example: Simple distance (very rough, not geographically accurate for large distances)
            double distanceKm = Math.sqrt(Math.pow(deliveryLat - pickupLat, 2) + Math.pow(deliveryLng - pickupLng, 2)) * 111; // Very rough approx

            // Example: Simple pricing
            double price = 50.0 + (distanceKm * 2.5) + (weight * 10.0);
            String notes = "Estimation based on approximate distance and weight.";
            // --- End of Placeholder Logic ---

            Map<String, Object> jsonResponseData = new HashMap<>();
            jsonResponseData.put("price", price);
            jsonResponseData.put("distanceKm", distanceKm);
            jsonResponseData.put("notes", notes);
            // As per your script.js, it also expects a 'routePath' for drawing on the map.
            // For now, let's send back the pickup and delivery points as a simple route.
            // In a real app, this would be a list of points from a routing service.
            Map<String, Double> pickupPoint = new HashMap<>();
            pickupPoint.put("lat", pickupLat);
            pickupPoint.put("lng", pickupLng);

            Map<String, Double> deliveryPoint = new HashMap<>();
            deliveryPoint.put("lat", deliveryLat);
            deliveryPoint.put("lng", deliveryLng);

            jsonResponseData.put("routePath", new Object[]{pickupPoint, deliveryPoint});


            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(), jsonResponseData);

        } catch (Exception e) {
            e.printStackTrace();
            // Send a more structured error response if possible
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Error calculating estimation: " + e.getMessage());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 500 error
            objectMapper.writeValue(response.getWriter(), errorResponse);
        }
    }
}