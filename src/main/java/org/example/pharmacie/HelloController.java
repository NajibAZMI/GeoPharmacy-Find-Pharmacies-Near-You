package org.example.pharmacie;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;

public class HelloController {

    @FXML
    private TextField locationField;

    @FXML
    private ListView<String> pharmacyListView;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    @FXML
    private void GPS(){

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

        if (elements.length() == 0) {
            pharmacyListView.getItems().add("Aucune pharmacie trouvée.");
        } else {
            for (int i = 0; i < elements.length(); i++) {
                JSONObject element = elements.getJSONObject(i);
                String name = element.has("tags") && element.getJSONObject("tags").has("name")
                        ? element.getJSONObject("tags").getString("name")
                        : "Pharmacie sans nom";
                pharmacyListView.getItems().add(name);
            }
        }
    }
}
