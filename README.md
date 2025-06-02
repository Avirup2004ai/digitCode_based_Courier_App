# DigiPin Courier App

## Overview

The DigiPin Courier App is a web application designed to demonstrate a location-based addressing system using "DigiPins". Users can select pickup and delivery locations on an interactive map, generate unique DigiPins for these locations, get an estimated courier price based on the locations and parcel weight, and simulate a booking process.

## Features

* **Interactive Map Selection:** Users can click on a Leaflet.js map to choose precise pickup and delivery locations.
* **DigiPin Generation:** Generates a unique 10-character DigiPin based on geographic coordinates.
* **Address Geocoding:** Allows users to search for addresses, which are then geocoded (using Nominatim/OpenStreetMap) and displayed on the map to help set locations.
* **Courier Service Estimation:**
    * Calculates an estimated shipping price based on pickup and delivery DigiPins (implicitly using coordinates), and parcel weight.
    * Displays a conceptual route on a separate map.
* **Parcel Booking Simulation:** Allows users to simulate booking a parcel based on the estimated details.

## Tech Stack

* **Backend:**
    * Java
    * Java Servlets (for handling API requests)
    * Jackson (for JSON processing)
* **Frontend:**
    * HTML5
    * CSS3
    * JavaScript (ES6+)
    * Leaflet.js (for interactive maps)
* **Build & Dependency Management:**
    * Apache Maven
* **Server:**
    * Apache Tomcat (embedded via `tomcat7-maven-plugin`)

## Setup and Running the Project

### Prerequisites

* Java Development Kit (JDK) - Version 1.8 or higher (as specified in `pom.xml`).
* Apache Maven installed and configured.
* A modern web browser.

### Instructions

1.  **Clone the repository (or download the source code):**
    ```bash
    git clone <your-repository-url>
    cd DigiPinCourierApp 
    ```
    (If you haven't uploaded to GitHub yet, you'll do this after setting up the project locally).

2.  **Build the project using Maven:**
    Open a terminal or command prompt in the project's root directory (where `pom.xml` is located) and run:
    ```bash
    mvn clean package
    ```
    This will compile the code and create a WAR file in the `target` directory.

3.  **Run the application:**
    Use the Tomcat 7 Maven plugin to run the application:
    ```bash
    mvn tomcat7:run
    ```

4.  **Access the application:**
    Open your web browser and navigate to:
    [http://localhost:8080/DigiPinCourierApp/](http://localhost:8080/DigiPinCourierApp/)
    (The port and context path are configured in the `pom.xml` for the `tomcat7-maven-plugin`).

## API Endpoints

The application uses the following backend API endpoints:

* **`GET /api/location/digipin`**
    * Generates a DigiPin for given coordinates.
    * Query Parameters: `latitude`, `longitude`
    * Returns: JSON with the generated `digipin`.
* **`POST /api/courier/estimate`**
    * Provides a price and route estimation.
    * Request Body: JSON containing `pickupDigiPin`, `deliveryDigiPin`, `weight`, `pickupLat`, `pickupLng`, `deliveryLat`, `deliveryLng`.
    * Returns: JSON with `price`, `distanceKm`, `notes`, and `routePath`.
* **`POST /api/courier/book`**
    * Simulates booking a parcel.
    * Request Body: JSON with booking details like DigiPins, coordinates, weight, and estimated price.
    * Returns: JSON with `bookingId` and `status`.

## Usage

1.  **Set Locations:**
    * Click the "Set Pickup Location" or "Set Delivery Location" button to activate the respective mode.
    * Click on the main map to select a location. The latitude, longitude, and generated DigiPin will be populated.
    * Alternatively, type an address in the "Pickup Address" or "Delivery Address" field and click the "Find ... on Map" button to geocode it.
2.  **Enter Parcel Weight:** Input the weight of the parcel in kilograms.
3.  **Get Estimation:** Click the "Get Estimation" button. The estimated price and route information (including a visual on the smaller map) will be displayed.
4.  **Book Parcel:** After getting an estimation, click the "Book Parcel" button to simulate the booking. A status message will be shown.

---