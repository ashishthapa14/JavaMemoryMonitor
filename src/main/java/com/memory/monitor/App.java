package com.memory.monitor;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    private MemoryMonitorController controller;

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/memory/monitor/MemoryMonitorView.fxml"));
        Parent root = loader.load(); // Load the FXML layout
        controller = loader.getController(); // Get the controller instance

        Scene scene = new Scene(root, 1000, 700); // Set initial scene size
        primaryStage.setTitle("JVM Memory Monitor");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Start monitoring when the application starts
        controller.startMonitoring();
    }

    @Override
    public void stop() throws Exception {
        // Stop monitoring when the application closes
        if (controller != null) {
            controller.stopMonitoring();
        }
        super.stop();
    }

    public static void main(String[] args) {
        launch(); // Launch the JavaFX application
    }
}
