package com.yourcourier.service; // Or your chosen package name

import java.text.DecimalFormat;

public class DigiPinService {

    private static final char[][] DIGIPIN_GRID = {
            {'F', 'C', '9', '8'},
            {'J', '3', '2', '7'},
            {'K', '4', '5', '6'},
            {'L', 'M', 'P', 'T'}
    };

    private static final double MIN_LAT = 2.5;
    private static final double MAX_LAT = 38.5;
    private static final double MIN_LON = 63.5;
    private static final double MAX_LON = 99.5;

    private static final DecimalFormat COORD_FORMAT = new DecimalFormat("0.000000");

    public static class Coordinates {
        public double latitude;
        public double longitude;

        public Coordinates(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public String getFormattedLatitude() {
            return COORD_FORMAT.format(this.latitude);
        }

        public String getFormattedLongitude() {
            return COORD_FORMAT.format(this.longitude);
        }

        @Override
        public String toString() {
            return "Coordinates{" +
                    "latitude=" + getFormattedLatitude() +
                    ", longitude=" + getFormattedLongitude() +
                    '}';
        }
    }

    public String generateDigiPin(double lat, double lon) {
        if (lat < MIN_LAT || lat > MAX_LAT) {
            throw new IllegalArgumentException("Latitude out of range. Must be between " + MIN_LAT + " and " + MAX_LAT + ". Got: " + lat);
        }
        if (lon < MIN_LON || lon > MAX_LON) {
            throw new IllegalArgumentException("Longitude out of range. Must be between " + MIN_LON + " and " + MAX_LON + ". Got: " + lon);
        }

        double currentMinLat = MIN_LAT;
        double currentMaxLat = MAX_LAT;
        double currentMinLon = MIN_LON;
        double currentMaxLon = MAX_LON;

        StringBuilder digiPinBuilder = new StringBuilder();

        for (int level = 1; level <= 10; level++) {
            double latDiv = (currentMaxLat - currentMinLat) / 4.0;
            double lonDiv = (currentMaxLon - currentMinLon) / 4.0;

            // Ensure lat/lon is strictly within current cell for floor calculation
            double relativeLat = Math.max(0, Math.min(lat - currentMinLat, currentMaxLat - currentMinLat - 1e-9));
            int row = 3 - (int) Math.floor(relativeLat / latDiv);

            double relativeLon = Math.max(0, Math.min(lon - currentMinLon, currentMaxLon - currentMinLon - 1e-9));
            int col = (int) Math.floor(relativeLon / lonDiv);

            // Clamp row and col to be within grid dimensions [0-3]
            row = Math.max(0, Math.min(row, 3));
            col = Math.max(0, Math.min(col, 3));

            digiPinBuilder.append(DIGIPIN_GRID[row][col]);

            if (level == 3 || level == 6) {
                digiPinBuilder.append('-');
            }

            // Update bounds for the next level - THIS IS THE CRITICAL SECTION
            // The following block correctly calculates the new cell based on old bounds
            double chosenCellMinLat = currentMinLat + latDiv * (3 - row);
            double chosenCellMaxLat = currentMinLat + latDiv * (4 - row);
            double chosenCellMinLon = currentMinLon + lonDiv * col;
            double chosenCellMaxLon = currentMinLon + lonDiv * (col + 1);

            currentMinLat = chosenCellMinLat;
            currentMaxLat = chosenCellMaxLat;
            currentMinLon = chosenCellMinLon;
            currentMaxLon = chosenCellMaxLon;
        }
        return digiPinBuilder.toString();
    }

    public Coordinates getCoordinatesFromDigiPin(String digiPin) {
        if (digiPin == null) {
            throw new IllegalArgumentException("Invalid DIGIPIN: Cannot be null.");
        }
        String pin = digiPin.replace("-", "");
        if (pin.length() != 10) {
            throw new IllegalArgumentException("Invalid DIGIPIN length after removing hyphens. Expected 10 characters, got " + pin.length());
        }

        double currentMinLat = MIN_LAT;
        double currentMaxLat = MAX_LAT;
        double currentMinLon = MIN_LON;
        double currentMaxLon = MAX_LON;

        for (int i = 0; i < 10; i++) {
            char currentChar = pin.charAt(i);
            int foundRow = -1, foundCol = -1;
            boolean charFound = false;

            for (int r = 0; r < 4; r++) {
                for (int c = 0; c < 4; c++) {
                    if (DIGIPIN_GRID[r][c] == currentChar) {
                        foundRow = r;
                        foundCol = c;
                        charFound = true;
                        break;
                    }
                }
                if (charFound) break;
            }

            if (!charFound) {
                throw new IllegalArgumentException("Invalid character '" + currentChar + "' in DIGIPIN at position " + (digiPin.indexOf(currentChar) + 1));
            }

            double latDiv = (currentMaxLat - currentMinLat) / 4.0;
            double lonDiv = (currentMaxLon - currentMinLon) / 4.0;

            // This bound update logic for decoding also needs to be correct
            // The logic from your provided code:
            currentMinLat = currentMaxLat - latDiv * (foundRow + 1);
            currentMaxLat = currentMinLat + latDiv; // Uses newly updated currentMinLat - THIS IS A BUG PATTERN
            // Should be: currentMaxLat_old - latDiv * foundRow;

            currentMinLon = currentMinLon + lonDiv * foundCol;
            currentMaxLon = currentMinLon + lonDiv; // Uses newly updated currentMinLon - THIS IS A BUG PATTERN
            // Should be: currentMinLon_old + lonDiv * (foundCol + 1);

        }

        double centerLat = (currentMinLat + currentMaxLat) / 2.0;
        double centerLon = (currentMinLon + currentMaxLon) / 2.0;

        return new Coordinates(centerLat, centerLon);
    }

    // Main method for quick testing
    public static void main(String[] args) {
        DigiPinService service = new DigiPinService();
        double testLat1 = 12.9716; // Bangalore example
        double testLon1 = 77.5946;
        try {
            System.out.println("Testing with Latitude: " + testLat1 + ", Longitude: " + testLon1);
            String digipin1 = service.generateDigiPin(testLat1, testLon1);
            System.out.println("Generated DIGIPIN 1: " + digipin1);

            Coordinates coords1 = service.getCoordinatesFromDigiPin(digipin1);
            System.out.println("Decoded DIGIPIN 1: Lat=" + coords1.getFormattedLatitude() + ", Lon=" + coords1.getFormattedLongitude());
            System.out.println("Original Coords 1: Lat=" + COORD_FORMAT.format(testLat1) + ", Lon=" + COORD_FORMAT.format(testLon1));
            System.out.println("---");

        } catch (IllegalArgumentException e) {
            System.err.println("Error with test case 1: " + e.getMessage());
        }

        double testLat2 = 28.622788; // Dak Bhawan example
        double testLon2 = 77.213033;
        try {
            System.out.println("Testing with Latitude: " + testLat2 + ", Longitude: " + testLon2);
            String digipin2 = service.generateDigiPin(testLat2, testLon2);
            System.out.println("Generated DIGIPIN 2: " + digipin2);

            Coordinates coords2 = service.getCoordinatesFromDigiPin(digipin2);
            System.out.println("Decoded DIGIPIN 2: Lat=" + coords2.getFormattedLatitude() + ", Lon=" + coords2.getFormattedLongitude());
            System.out.println("Original Coords 2: Lat=" + COORD_FORMAT.format(testLat2) + ", Lon=" + COORD_FORMAT.format(testLon2));
            System.out.println("---");
        } catch (IllegalArgumentException e) {
            System.err.println("Error with test case 2: " + e.getMessage());
        }
    }
}