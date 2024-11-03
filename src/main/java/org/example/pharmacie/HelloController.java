package org.example.pharmacie;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
public class HelloController {

    @FXML
    private TextField locationField;

    @FXML
    private ListView<String> pharmacyListView;

    @FXML
    private Label pharmacyNameLabel;      // Nouveau label pour afficher le nom de la pharmacie
    @FXML
    private Label pharmacyDistanceLabel;   // Nouveau label pour afficher la distance
    @FXML
    private Label pharmacyLatLabel;        // Nouveau label pour afficher la latitude
    @FXML
    private Label pharmacyLonLabel;        // Nouveau label pour afficher la longitude
    @FXML
    private WebView mapWebView;
    private static final int EARTH_RADIUS = 6371;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private double userLat;
    private double userLon;

    @FXML
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c; // Distance en kilomètres
    }

    @FXML
    private void GPS() {
        String apiUrl = "http://ip-api.com/json"; // API pour obtenir la localisation de l'IP

        try {
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            // Fermez les flux
            in.close();
            conn.disconnect();

            // Traitement de la réponse JSON
            JSONObject json = new JSONObject(content.toString());
            userLat = json.getDouble("lat");
            userLon = json.getDouble("lon");

            String locationCoords = userLat + "," + userLon;
            locationField.setText(locationCoords); // Mettre à jour le champ de texte avec les coordonnées
            fetchPharmacies(locationCoords); // Rechercher des pharmacies avec les coordonnées

        } catch (Exception e) {
            pharmacyListView.getItems().add("Erreur lors de la récupération des coordonnées : " + e.getMessage());
        }
    }

    @FXML
    private void handleSearch() {
        pharmacyListView.getItems().clear();
        String location = locationField.getText();

        if (!location.isEmpty()) {
            fetchPharmacies(location);
        } else {
            pharmacyListView.getItems().add("Veuillez entrer une localisation !");
        }
    }

    private void fetchPharmacies(String location) {
        // Exemple de requête à l'API Overpass pour trouver des pharmacies près de la localisation
        String overpassApiUrl = "https://overpass-api.de/api/interpreter?data=[out:json];node[amenity=pharmacy](around:1000," + location + ");out;";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(overpassApiUrl))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(this::parseAndDisplayPharmacies)
                .exceptionally(ex -> {
                    pharmacyListView.getItems().add("Erreur de connexion : " + ex.getMessage());
                    return null;
                });
    }

    private void parseAndDisplayPharmacies(String responseBody) {
        JSONObject json = new JSONObject(responseBody);
        JSONArray elements = json.getJSONArray("elements");

        List<Pharmacy> pharmacies = new ArrayList<>();

        if (elements.length() == 0) {
            pharmacyListView.getItems().add("Aucune pharmacie trouvée.");
        } else {
            for (int i = 0; i < elements.length(); i++) {
                JSONObject element = elements.getJSONObject(i);
                String name = element.has("tags") && element.getJSONObject("tags").has("name")
                        ? element.getJSONObject("tags").getString("name")
                        : "Pharmacie sans nom";

                double lat = element.getDouble("lat");
                double lon = element.getDouble("lon");
                double distance = calculateDistance(userLat, userLon, lat, lon);

                pharmacies.add(new Pharmacy(name, lat, lon, distance));
            }

            // Trier par distance
            pharmacies.sort(Comparator.comparingDouble(Pharmacy::getDistance));

            // Afficher les pharmacies triées
            for (Pharmacy pharmacy : pharmacies) {
                pharmacyListView.getItems().add(
                        pharmacy.getName() +
                                " - Distance: " + String.format("%.2f km", pharmacy.getDistance()) +
                                " (Lat: " + pharmacy.getLat() + ", Lon: " + pharmacy.getLon() + ")"
                );
            }
        }
    }
    @FXML
    private void loadMap(double latitude, double longitude) {
        WebEngine webEngine = mapWebView.getEngine();
        String mapUrl = "https://www.openstreetmap.org/export/embed.html?bbox="
                + (longitude - 0.01) + "%2C" + (latitude - 0.01) + "%2C"
                + (longitude + 0.01) + "%2C" + (latitude + 0.01)
                + "&layer=mapnik&marker=" + latitude + "%2C" + longitude;
        webEngine.load(mapUrl);
    }
    // Méthode pour afficher les détails de la pharmacie sélectionnée
    @FXML
    private void displayPharmacyDetails(MouseEvent event) {
        // Vérifier si un élément a été sélectionné
        if (event.getClickCount() == 2) { // Double clic pour sélectionner
            String selectedPharmacy = pharmacyListView.getSelectionModel().getSelectedItem();
            if (selectedPharmacy != null) {
                String[] parts = selectedPharmacy.split(" - ");
                String name = parts[0];
                String[] coords = parts[1].split("\\(")[1].split("\\)")[0].split(", ");
                double lat = Double.parseDouble(coords[0].split(": ")[1]);
                double lon = Double.parseDouble(coords[1].split(": ")[1]);

                // Mettre à jour les labels avec les détails de la pharmacie
                pharmacyNameLabel.setText("Nom : " + name);
                pharmacyDistanceLabel.setText("Distance : " + String.format("%.2f km", calculateDistance(userLat, userLon, lat, lon)));
                pharmacyLatLabel.setText("Latitude : " + lat);
                pharmacyLonLabel.setText("Longitude : " + lon);

                loadMap(lat, lon);
            }
        }
    }

    // Classe Pharmacy pour stocker les informations des pharmacies
    private static class Pharmacy {
        private final String name;
        private final double lat;
        private final double lon;
        private final double distance;

        public Pharmacy(String name, double lat, double lon, double distance) {
            this.name = name;
            this.lat = lat;
            this.lon = lon;
            this.distance = distance;
        }

        public String getName() {
            return name;
        }

        public double getDistance() {
            return distance;
        }
        public double getLat() { // Nouvelle méthode
            return lat;
        }

        public double getLon() { // Nouvelle méthode
            return lon;
        }
    }
}
