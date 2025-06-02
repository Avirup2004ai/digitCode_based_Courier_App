let map;
let pickupMarker, deliveryMarker;
let currentMapMode = 'pickup'; // 'pickup' or 'delivery'
let pickupCoords, deliveryCoords; // To store {lat, lng} objects

let routeMap;
let routePolyline; // For displaying the route on the routeMap

const CONTEXT_PATH = "/DigiPinCourierApp";

function initMap() {
    console.log("DEBUG: initMap called"); // DEBUG
    map = L.map('map').setView([22.5726, 88.3639], 10);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
    }).addTo(map);

    map.on('click', function(e) {
        console.log("DEBUG: Map clicked!", e.latlng); // DEBUG
        const lat = e.latlng.lat;
        const lng = e.latlng.lng;

        if (currentMapMode === 'pickup') {
            console.log("DEBUG: Map click in pickup mode"); // DEBUG
            if (pickupMarker) map.removeLayer(pickupMarker);
            pickupMarker = L.marker([lat, lng]).addTo(map).bindPopup('Pickup Location').openPopup();
            document.getElementById('pickupLat').value = lat.toFixed(6);
            document.getElementById('pickupLng').value = lng.toFixed(6);
            pickupCoords = { lat: lat, lng: lng };
            console.log("DEBUG: Calling fetchDigiPin for pickup", lat, lng); // DEBUG
            fetchDigiPin(lat, lng, 'pickup');
        } else if (currentMapMode === 'delivery') {
            console.log("DEBUG: Map click in delivery mode"); // DEBUG
            if (deliveryMarker) map.removeLayer(deliveryMarker);
            deliveryMarker = L.marker([lat, lng]).addTo(map).bindPopup('Delivery Location').openPopup();
            document.getElementById('deliveryLat').value = lat.toFixed(6);
            document.getElementById('deliveryLng').value = lng.toFixed(6);
            deliveryCoords = { lat: lat, lng: lng };
            console.log("DEBUG: Calling fetchDigiPin for delivery", lat, lng); // DEBUG
            fetchDigiPin(lat, lng, 'delivery');
        }
    });
}

function initRouteMap() {
    console.log("DEBUG: initRouteMap called"); // DEBUG
    routeMap = L.map('routeMap').setView([22.5726, 88.3639], 5);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
    }).addTo(routeMap);
    document.getElementById('routeMapLabel').style.display = 'none';
}

function setMapMode(mode) {
    console.log("DEBUG: setMapMode called with mode:", mode); // DEBUG
    currentMapMode = mode;
    const pickupBtn = document.getElementById('setPickupModeBtn');
    const deliveryBtn = document.getElementById('setDeliveryModeBtn');

    if (mode === 'pickup') {
        pickupBtn.classList.add('active-mode');
        deliveryBtn.classList.remove('active-mode');
        alert('Map mode: SET PICKUP. Click on the map to select the pickup location.');
    } else {
        deliveryBtn.classList.add('active-mode');
        pickupBtn.classList.remove('active-mode');
        alert('Map mode: SET DELIVERY. Click on the map to select the delivery location.');
    }
}

async function fetchDigiPin(lat, lng, type) {
    console.log(`DEBUG: fetchDigiPin called for type: ${type}, lat: ${lat}, lng: ${lng}`); // DEBUG
    const url = `${CONTEXT_PATH}/api/location/digipin?latitude=${lat.toFixed(6)}&longitude=${lng.toFixed(6)}`;
    console.log("DEBUG: Fetching URL:", url); // DEBUG
    try {
        const response = await fetch(url);
        console.log("DEBUG: fetchDigiPin response status:", response.status); // DEBUG
        if (!response.ok) {
            const errorText = await response.text(); // Get raw error text
            console.error(`DEBUG: fetchDigiPin response not OK. Status: ${response.status}, Text: ${errorText}`); // DEBUG
            // Try to parse as JSON, but fallback if not
            let errorData;
            try {
                errorData = JSON.parse(errorText);
            } catch (e) {
                errorData = { error: `HTTP error! status: ${response.status}. Response: ${errorText}` };
            }
            throw new Error(errorData.error || `Failed to fetch DigiPin. Status: ${response.status}`);
        }
        const data = await response.json();
        console.log("DEBUG: fetchDigiPin data received:", data); // DEBUG
        if (type === 'pickup') {
            document.getElementById('pickupDigiPin').value = data.digipin || "Error: No digipin in response";
        } else if (type === 'delivery') {
            document.getElementById('deliveryDigiPin').value = data.digipin || "Error: No digipin in response";
        }
    } catch (error) {
        console.error('DEBUG: Error in fetchDigiPin:', error); // DEBUG
        alert('Could not fetch DigiPin: ' + error.message);
    }
}

async function geocodeAddress(type) {
    console.log("DEBUG: geocodeAddress called for type:", type); // DEBUG
    const addressInputId = type === 'pickup' ? 'pickupAddress' : 'deliveryAddress';
    const address = document.getElementById(addressInputId).value;
    if (!address) {
        alert('Please enter an address to find on the map.');
        return;
    }
    try {
        const response = await fetch(`https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(address)}&limit=1`);
        if (!response.ok) {
            throw new Error(`Nominatim HTTP error! status: ${response.status}`);
        }
        const data = await response.json();
        if (data && data.length > 0) {
            const lat = parseFloat(data[0].lat);
            const lng = parseFloat(data[0].lon);
            map.setView([lat, lng], 15);
            map.fire('click', { latlng: L.latLng(lat, lng) });
        } else {
            alert('Address not found by geocoding service.');
        }
    } catch (error) {
        console.error('Error geocoding address:', error);
        alert('Error finding address on map: ' + error.message);
    }
}

async function getEstimation() {
    console.log("DEBUG: getEstimation called"); // DEBUG
    const pickupDigiPin = document.getElementById('pickupDigiPin').value;
    const deliveryDigiPin = document.getElementById('deliveryDigiPin').value;
    const parcelWeight = document.getElementById('parcelWeight').value;

    const pickupLat = document.getElementById('pickupLat').value;
    const pickupLng = document.getElementById('pickupLng').value;
    const deliveryLat = document.getElementById('deliveryLat').value;
    const deliveryLng = document.getElementById('deliveryLng').value;

    if (!pickupDigiPin || !deliveryDigiPin || !parcelWeight) {
        alert('Please ensure Pickup DigiPin, Delivery DigiPin, and Parcel Weight are filled.');
        return;
    }
    if (!pickupLat || !pickupLng || !deliveryLat || !deliveryLng) {
        alert('Please select pickup and delivery locations on the map.');
        return;
    }

    const url = `${CONTEXT_PATH}/api/courier/estimate`;
    console.log("DEBUG: getEstimation - Fetching URL:", url); // DEBUG
    try {
        const response = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                pickupDigiPin: pickupDigiPin,
                deliveryDigiPin: deliveryDigiPin,
                weight: parseFloat(parcelWeight),
                pickupLat: parseFloat(pickupLat),
                pickupLng: parseFloat(pickupLng),
                deliveryLat: parseFloat(deliveryLat),
                deliveryLng: parseFloat(deliveryLng)
            })
        });
        console.log("DEBUG: getEstimation response status:", response.status); // DEBUG
        if (!response.ok) {
            const errorText = await response.text();
            console.error(`DEBUG: getEstimation response not OK. Status: ${response.status}, Text: ${errorText}`); // DEBUG
            let errorData;
            try {
                errorData = JSON.parse(errorText);
            } catch (e) {
                errorData = { message: `HTTP error! status: ${response.status}. Response: ${errorText}` };
            }
            throw new Error(errorData.message || `Failed to get estimation. Status: ${response.status}`);
        }
        const data = await response.json();
        console.log("DEBUG: getEstimation data received:", data); // DEBUG
        document.getElementById('estimatedPrice').textContent = `₹${data.price.toFixed(2)}`;
        document.getElementById('routeInfo').textContent = `Distance: ${data.distanceKm ? data.distanceKm.toFixed(2) + ' km' : 'N/A'}. ${data.notes || ''}`;
        document.getElementById('bookingStatus').textContent = 'N/A';

        if (routePolyline) routeMap.removeLayer(routePolyline);
        routeMap.eachLayer(function (layer) {
            if (layer instanceof L.Marker) {
                routeMap.removeLayer(layer);
            }
        });

        if (data.routePath && data.routePath.length > 0) {
            const latLngs = data.routePath.map(point => [point.lat, point.lng]);
            routePolyline = L.polyline(latLngs, { color: 'blue' }).addTo(routeMap);
            L.marker([parseFloat(pickupLat), parseFloat(pickupLng)]).addTo(routeMap).bindPopup('Pickup');
            L.marker([parseFloat(deliveryLat), parseFloat(deliveryLng)]).addTo(routeMap).bindPopup('Delivery');
            routeMap.fitBounds(routePolyline.getBounds());
            document.getElementById('routeMapLabel').style.display = 'block';
        } else {
            if (pickupCoords && deliveryCoords) {
                L.marker([pickupCoords.lat, pickupCoords.lng]).addTo(routeMap).bindPopup('Pickup');
                L.marker([deliveryCoords.lat, deliveryCoords.lng]).addTo(routeMap).bindPopup('Delivery');
                routeMap.setView([(pickupCoords.lat + deliveryCoords.lat) / 2, (pickupCoords.lng + deliveryCoords.lng) / 2], 8);
                document.getElementById('routeMapLabel').style.display = 'block';
            } else {
                document.getElementById('routeMapLabel').style.display = 'none';
            }
            document.getElementById('routeInfo').textContent += " (Visual route path not available or only points shown)";
        }
    } catch (error) {
        console.error('DEBUG: Error in getEstimation:', error); // DEBUG
        document.getElementById('estimatedPrice').textContent = 'Error';
        document.getElementById('routeInfo').textContent = error.message;
    }
}

async function bookParcel() {
    console.log("DEBUG: bookParcel called"); // DEBUG
    const pickupDigiPin = document.getElementById('pickupDigiPin').value;
    const deliveryDigiPin = document.getElementById('deliveryDigiPin').value;
    const parcelWeight = document.getElementById('parcelWeight').value;
    const estimatedPriceText = document.getElementById('estimatedPrice').textContent;

    const pickupLat = document.getElementById('pickupLat').value;
    const pickupLng = document.getElementById('pickupLng').value;
    const deliveryLat = document.getElementById('deliveryLat').value;
    const deliveryLng = document.getElementById('deliveryLng').value;

    if (!pickupDigiPin || !deliveryDigiPin || !parcelWeight || estimatedPriceText === 'N/A' || estimatedPriceText === 'Error') {
        alert('Please get a valid estimation before booking. Ensure all location and weight details are present.');
        return;
    }
     if (!pickupLat || !pickupLng || !deliveryLat || !deliveryLng) {
        alert('Pickup and delivery coordinates are missing. Please select locations on the map again.');
        return;
    }

    const bookingDetails = {
        pickupDigiPin: pickupDigiPin,
        pickupLat: parseFloat(pickupLat),
        pickupLng: parseFloat(pickupLng),
        deliveryDigiPin: deliveryDigiPin,
        deliveryLat: parseFloat(deliveryLat),
        deliveryLng: parseFloat(deliveryLng),
        weight: parseFloat(parcelWeight),
        estimatedPrice: parseFloat(estimatedPriceText.replace('₹', ''))
    };
    const url = `${CONTEXT_PATH}/api/courier/book`;
    console.log("DEBUG: bookParcel - Fetching URL:", url); // DEBUG
    try {
        const response = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(bookingDetails)
        });
        console.log("DEBUG: bookParcel response status:", response.status); // DEBUG
        if (!response.ok) {
            const errorText = await response.text();
            console.error(`DEBUG: bookParcel response not OK. Status: ${response.status}, Text: ${errorText}`); // DEBUG
            let errorData;
            try {
                errorData = JSON.parse(errorText);
            } catch (e) {
                errorData = { message: `HTTP error! status: ${response.status}. Response: ${errorText}` };
            }
            throw new Error(errorData.message || `Booking failed. Status: ${response.status}`);
        }
        const data = await response.json();
        console.log("DEBUG: bookParcel data received:", data); // DEBUG
        document.getElementById('bookingStatus').textContent = `Booking successful! ID: ${data.bookingId}. Status: ${data.status}`;
        alert(`Booking successful! Booking ID: ${data.bookingId}`);
    } catch (error) {
        console.error('DEBUG: Error in bookParcel:', error); // DEBUG
        document.getElementById('bookingStatus').textContent = 'Booking failed: ' + error.message;
        alert('Booking failed. ' + error.message);
    }
}

document.addEventListener('DOMContentLoaded', function() {
    console.log("DEBUG: DOMContentLoaded event fired"); // DEBUG
    initMap();
    initRouteMap();
    setMapMode('pickup');
});