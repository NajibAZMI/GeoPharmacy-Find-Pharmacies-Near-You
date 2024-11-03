package org.example.pharmacie;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;



public class HelloApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Charger le fichier FXML
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("hello-view.fxml"));

        // Créer la scène
        Scene scene = new Scene(fxmlLoader.load(),1200,640);

        // Configurer et afficher la fenêtre principale
        primaryStage.setScene(scene);
        primaryStage.setTitle("Localisation des Pharmacies");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
