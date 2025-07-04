package com.memory.monitor;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.BarChart;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

public class MemoryMonitorController implements Initializable {

    @FXML private Label heapUsedLabel;
    @FXML private Label heapCommittedLabel;
    @FXML private Label heapMaxLabel;
    @FXML private Label nonHeapUsedLabel;
    @FXML private Label nonHeapCommittedLabel;
    @FXML private Label gcCountLabel;
    @FXML private Label gcTimeLabel;
    @FXML private Label allocationRateLabel;
    @FXML private TextArea gcLogTextArea;

    @FXML private LineChart<Number, Number> heapLineChart;
    @FXML private BarChart<String, Number> nonHeapBarChart; // For Non-Heap, can be adapted for thread count
    @FXML private BarChart<String, Number> memoryUsageBarChart; // For snapshot memory usage
    @FXML private LineChart<Number, Number> allocationRateChart;

    private MemoryMonitorService monitorService;

    // Series for the charts
    private XYChart.Series<Number, Number> heapUsedSeries;
    private XYChart.Series<Number, Number> heapCommittedSeries;
    private XYChart.Series<Number, Number> allocationRateSeries;
    private XYChart.Series<Number, Number> gcCountSeries; // For tracking GC events on chart

    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private long startTimeMillis;
    private long lastGcEventCount = 0; // To detect new GC events

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        monitorService = new MemoryMonitorService();
        startTimeMillis = System.currentTimeMillis();

        // Initialize Heap Line Chart
        heapUsedSeries = new XYChart.Series<>();
        heapUsedSeries.setName("Used Heap");
        heapCommittedSeries = new XYChart.Series<>();
        heapCommittedSeries.setName("Committed Heap");
        heapLineChart.getData().addAll(heapUsedSeries, heapCommittedSeries);
        ((NumberAxis)heapLineChart.getXAxis()).setForceZeroInRange(false); // Allows chart to scroll
        ((NumberAxis)heapLineChart.getYAxis()).setForceZeroInRange(false);

        // Initialize Allocation Rate Chart
        allocationRateSeries = new XYChart.Series<>();
        allocationRateSeries.setName("Allocation Rate");
        allocationRateChart.getData().add(allocationRateSeries);
        ((NumberAxis)allocationRateChart.getXAxis()).setForceZeroInRange(false);
        ((NumberAxis)allocationRateChart.getYAxis()).setForceZeroInRange(true); // Rate can be zero

        // Initialize Memory Usage Bar Chart (Current snapshot)
        // This chart will be updated with each data point, displaying current values
        XYChart.Series<String, Number> currentMemorySeries = new XYChart.Series<>();
        currentMemorySeries.setName("Current Usage");
        currentMemorySeries.getData().add(new XYChart.Data<>("Heap Used", 0));
        currentMemorySeries.getData().add(new XYChart.Data<>("Heap Committed", 0));
        currentMemorySeries.getData().add(new XYChart.Data<>("Non-Heap Used", 0));
        currentMemorySeries.getData().add(new XYChart.Data<>("Non-Heap Committed", 0));
        memoryUsageBarChart.getData().add(currentMemorySeries);

        // Initialize Non-Heap Bar Chart (Similar to screenshot, showing different non-heap segments or just total)
        XYChart.Series<String, Number> nonHeapSeries = new XYChart.Series<>();
        nonHeapSeries.setName("Non-Heap");
        nonHeapSeries.getData().add(new XYChart.Data<>("Used", 0));
        nonHeapSeries.getData().add(new XYChart.Data<>("Committed", 0));
        nonHeapBarChart.getData().add(nonHeapSeries);


        // Set fixed range for X-axis (time) to show a rolling window
        ((NumberAxis)heapLineChart.getXAxis()).setTickUnit(10); // Every 10 seconds
        ((NumberAxis)heapLineChart.getXAxis()).setLowerBound(0);
        ((NumberAxis)heapLineChart.getXAxis()).setUpperBound(60); // Show last 60 seconds of data initially

        ((NumberAxis)allocationRateChart.getXAxis()).setTickUnit(10);
        ((NumberAxis)allocationRateChart.getXAxis()).setLowerBound(0);
        ((NumberAxis)allocationRateChart.getXAxis()).setUpperBound(60);

        // Set up the data consumer to update the GUI
        monitorService.startMonitoring(0, 5, TimeUnit.SECONDS, this::updateGUI);
    }

    // Called when the "Start Monitor" button is pressed (or automatically on app start)
    @FXML
    public void startMonitoring() {
        if (!monitorService.scheduler.isShutdown()) { // Avoid starting if already running
            System.out.println("Monitor already running.");
            return;
        }
        monitorService = new MemoryMonitorService(); // Re-initialize if stopped
        lastGcEventCount = 0; // Reset GC count on restart
        startTimeMillis = System.currentTimeMillis();
        // Clear old chart data if restarting
        heapUsedSeries.getData().clear();
        heapCommittedSeries.getData().clear();
        allocationRateSeries.getData().clear();
        gcLogTextArea.clear();
        // Reset current memory bar chart data
        ((XYChart.Series<String, Number>) memoryUsageBarChart.getData().get(0)).getData().forEach(data -> data.setYValue(0));
        ((XYChart.Series<String, Number>) nonHeapBarChart.getData().get(0)).getData().forEach(data -> data.setYValue(0));

        monitorService.startMonitoring(0, 1, TimeUnit.SECONDS, this::updateGUI);
    }

    // Called when the "Stop Monitor" button is pressed
    @FXML
    public void stopMonitoring() {
        monitorService.stopMonitoring();
    }


    private void updateGUI(MemoryData data) {
        // All GUI updates must be on the JavaFX Application Thread
        Platform.runLater(() -> {
            // Update Labels
            heapUsedLabel.setText(String.format("%.2f MB", data.getHeapUsedMB()));
            heapCommittedLabel.setText(String.format("%.2f MB", data.getHeapCommittedMB()));
            heapMaxLabel.setText(String.format("%.2f MB", data.getHeapMaxMB()));
            nonHeapUsedLabel.setText(String.format("%.2f MB", data.getNonHeapUsedMB()));
            nonHeapCommittedLabel.setText(String.format("%.2f MB", data.getNonHeapCommittedMB()));
            gcCountLabel.setText(String.valueOf(data.getGcCount()));
            gcTimeLabel.setText(String.format("%d ms", data.getGcTimeMillis()));
            allocationRateLabel.setText(String.format("%.2f MB/s", data.getAllocationRateMBps()));

            // Update Line Charts
            double elapsedSeconds = (data.getTimestamp() - startTimeMillis) / 1000.0;
            heapUsedSeries.getData().add(new XYChart.Data<>(elapsedSeconds, data.getHeapUsedMB()));
            heapCommittedSeries.getData().add(new XYChart.Data<>(elapsedSeconds, data.getHeapCommittedMB()));
            allocationRateSeries.getData().add(new XYChart.Data<>(elapsedSeconds, data.getAllocationRateMBps()));

            // Keep only the last N data points for a rolling window
            int maxDataPoints = 60; // For a 60-second window if polling every 1 second
            if (heapUsedSeries.getData().size() > maxDataPoints) {
                heapUsedSeries.getData().remove(0);
                heapCommittedSeries.getData().remove(0);
            }
            if (allocationRateSeries.getData().size() > maxDataPoints) {
                allocationRateSeries.getData().remove(0);
            }

            // Adjust X-axis range dynamically for rolling window
            NumberAxis heapXAxis = (NumberAxis) heapLineChart.getXAxis();
            heapXAxis.setLowerBound(elapsedSeconds - maxDataPoints + 1);
            heapXAxis.setUpperBound(elapsedSeconds + 1);

            NumberAxis allocXAxis = (NumberAxis) allocationRateChart.getXAxis();
            allocXAxis.setLowerBound(elapsedSeconds - maxDataPoints + 1);
            allocXAxis.setUpperBound(elapsedSeconds + 1);


            // Update Memory Usage Bar Chart (snapshot)
            XYChart.Series<String, Number> currentMemorySeries = (XYChart.Series<String, Number>) memoryUsageBarChart.getData().get(0);
            currentMemorySeries.getData().get(0).setYValue(data.getHeapUsedMB());
            currentMemorySeries.getData().get(1).setYValue(data.getHeapCommittedMB());
            currentMemorySeries.getData().get(2).setYValue(data.getNonHeapUsedMB());
            currentMemorySeries.getData().get(3).setYValue(data.getNonHeapCommittedMB());

            // Update Non-Heap Bar Chart
            XYChart.Series<String, Number> nonHeapSeries = (XYChart.Series<String, Number>) nonHeapBarChart.getData().get(0);
            nonHeapSeries.getData().get(0).setYValue(data.getNonHeapUsedMB());
            nonHeapSeries.getData().get(1).setYValue(data.getNonHeapCommittedMB());


            // Log GC Events if count has increased
            if (data.getGcCount() > lastGcEventCount) {
                gcLogTextArea.appendText(String.format("[%s] GC Event: Count=%d, Total Time=%dms%n",
                    timeFormat.format(new Date(data.getTimestamp())), data.getGcCount(), data.getGcTimeMillis()));
                lastGcEventCount = data.getGcCount();
            }
        });
    }
}
