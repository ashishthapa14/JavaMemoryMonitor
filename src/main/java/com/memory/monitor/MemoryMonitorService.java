package com.memory.monitor;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MemoryMonitorService {

    public final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final MemoryMXBean memoryMXBean;
    private final List<GarbageCollectorMXBean> gcMXBeans;
    private Consumer<MemoryData> dataConsumer;

    // Variables to track for allocation rate and GC events
    private long lastHeapUsedBytes = 0;
    private long lastTimestamp = 0;
    private long lastGcCount = 0;

    public MemoryMonitorService() {
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
        this.gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
    }

    /**
     * Starts the memory monitoring at a fixed rate.
     * @param initialDelay The time to delay first execution.
     * @param period The period between successive executions.
     * @param unit The time unit of the initialDelay and period parameters.
     * @param consumer A callback function to receive MemoryData updates.
     */
    public void startMonitoring(long initialDelay, long period, TimeUnit unit, Consumer<MemoryData> consumer) {
        this.dataConsumer = consumer;
        scheduler.scheduleAtFixedRate(this::collectAndPublishData, initialDelay, period, unit);
        System.out.println("Memory monitoring service started.");
    }

    /**
     * Stops the memory monitoring service.
     */
    public void stopMonitoring() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow(); // Force shutdown if not terminated gracefully
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt(); // Restore interrupt status
            System.err.println("Memory monitoring service interrupted during shutdown.");
        }
        System.out.println("Memory monitoring service stopped.");
    }

    private void collectAndPublishData() {
        try {
            long currentTimestamp = System.currentTimeMillis();

            // --- Heap Memory Usage ---
            MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
            long heapUsed = heapUsage.getUsed();
            long heapCommitted = heapUsage.getCommitted();
            long heapMax = heapUsage.getMax();

            // --- Non-Heap Memory Usage ---
            // This includes Metaspace, Code Cache, etc., but NOT individual thread stacks directly.
            MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();
            long nonHeapUsed = nonHeapUsage.getUsed();
            long nonHeapCommitted = nonHeapUsage.getCommitted();

            // --- Garbage Collection Statistics ---
            long totalGcCount = 0;
            long totalGcTimeMillis = 0;
            for (GarbageCollectorMXBean gcBean : gcMXBeans) {
                totalGcCount += gcBean.getCollectionCount();
                totalGcTimeMillis += gcBean.getCollectionTime();
            }

            // --- Estimate Memory Allocation Rate ---
            double allocationRateMBps = 0.0;
            if (lastTimestamp != 0 && currentTimestamp > lastTimestamp) {
                long timeElapsedMillis = currentTimestamp - lastTimestamp;
                double timeElapsedSeconds = timeElapsedMillis / 1000.0;

                // Simple estimation: difference in used heap.
                // This is an approximation. A more precise rate would need tracking
                // allocated bytes by the JVM, which is not directly exposed by MemoryMXBean.
                // If a GC occurred, the 'used' memory might drop significantly.
                // This simple model will show a high allocation rate before GC and a drop after.
                long allocatedBytesSinceLastCheck = heapUsed - lastHeapUsedBytes;

                // If memory was freed (allocatedBytesSinceLastCheck < 0) due to GC,
                // we don't count it as "allocation". We're interested in *new* allocations.
                if (allocatedBytesSinceLastCheck > 0) {
                     allocationRateMBps = (allocatedBytesSinceLastCheck / (1024.0 * 1024.0)) / timeElapsedSeconds;
                }
            }

            // Create and publish the data object
            MemoryData data = new MemoryData(
                currentTimestamp,
                heapUsed, heapCommitted, heapMax,
                nonHeapUsed, nonHeapCommitted,
                totalGcCount, totalGcTimeMillis,
                allocationRateMBps
            );

            if (dataConsumer != null) {
                dataConsumer.accept(data);
            }

            // Update last known values for next calculation
            this.lastHeapUsedBytes = heapUsed;
            this.lastTimestamp = currentTimestamp;
            this.lastGcCount = totalGcCount; // Keep track of GC count to detect new collections
            
        } catch (Exception e) {
            System.err.println("Error collecting memory data: " + e.getMessage());
            e.printStackTrace();
            // Consider stopping monitoring or displaying an error in the GUI
        }
    }
}
