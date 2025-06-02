package com.yourcourier.controller; // Or the package you created

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourcourier.service.DigiPinService; // Assuming DigiPinService is in this package

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// This annotation maps this servlet to the URL your JavaScript is trying to reach
@WebServlet("/api/location/digipin")
public class LocationServlet extends HttpServlet {

    private DigiPinService digiPinService;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        // This method is called once when the servlet is first loaded.
        // It's a good place to initialize services.
        super.init();
        this.digiPinService = new DigiPinService();
        this.objectMapper = new ObjectMapper(); // For converting Java objects to JSON
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // This method handles GET requests to /api/location/digipin

        try {
            String latParam = request.getParameter("latitude");
            String lonParam = request.getParameter("longitude");

            if (latParam == null || lonParam == null || latParam.isEmpty() || lonParam.isEmpty()) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing latitude or longitude parameters.");
                return;
            }

            double latitude = Double.parseDouble(latParam);
            double longitude = Double.parseDouble(lonParam);

            // Use your DigiPinService to generate the DigiPin
            String digipin = digiPinService.generateDigiPin(latitude, longitude);

            // Prepare the data for the JSON response
            Map<String, String> jsonResponseData = new HashMap<>();
            jsonResponseData.put("digipin", digipin);
            // You could add more data to the response if needed, e.g.,
            // jsonResponseData.put("receivedLatitude", String.valueOf(latitude));
            // jsonResponseData.put("receivedLongitude", String.valueOf(longitude));

            // Set the content type of the response to application/json
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            // Write the JSON response to the client
            objectMapper.writeValue(response.getWriter(), jsonResponseData);

        } catch (NumberFormatException e) {
            // This error occurs if latitude or longitude are not valid numbers
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid latitude or longitude format.");
        } catch (IllegalArgumentException e) {
            // This catches errors from your DigiPinService (e.g., lat/lon out of range)
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            // For any other unexpected errors
            e.printStackTrace(); // Log the full error to the server console (Tomcat logs)
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An unexpected error occurred on the server.");
        }
    }
}