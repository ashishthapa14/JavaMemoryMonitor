package com.memory.monitor;

public class MemoryData {
    private final long timestamp;
    private final long heapUsedBytes;
    private final long heapCommittedBytes;
    private final long heapMaxBytes;
    private final long nonHeapUsedBytes;
    private final long nonHeapCommittedBytes;
    private final long gcCount;
    private final long gcTimeMillis;
    private final double allocationRateMBps;

    public MemoryData(long timestamp, long heapUsedBytes, long heapCommittedBytes, long heapMaxBytes,
                      long nonHeapUsedBytes, long nonHeapCommittedBytes,
                      long gcCount, long gcTimeMillis, double allocationRateMBps) {
        this.timestamp = timestamp;
        this.heapUsedBytes = heapUsedBytes;
        this.heapCommittedBytes = heapCommittedBytes;
        this.heapMaxBytes = heapMaxBytes;
        this.nonHeapUsedBytes = nonHeapUsedBytes;
        this.nonHeapCommittedBytes = nonHeapCommittedBytes;
        this.gcCount = gcCount;
        this.gcTimeMillis = gcTimeMillis;
        this.allocationRateMBps = allocationRateMBps;
    }

    // --- Getters ---
    public long getTimestamp() { return timestamp; }
    public long getHeapUsedBytes() { return heapUsedBytes; }
    public long getHeapCommittedBytes() { return heapCommittedBytes; }
    public long getHeapMaxBytes() { return heapMaxBytes; }
    public long getNonHeapUsedBytes() { return nonHeapUsedBytes; }
    public long getNonHeapCommittedBytes() { return nonHeapCommittedBytes; }
    public long getGcCount() { return gcCount; }
    public long getGcTimeMillis() { return gcTimeMillis; }
    public double getAllocationRateMBps() { return allocationRateMBps; }

    // Helper to convert bytes to MB for display
    public double getHeapUsedMB() { return bytesToMB(heapUsedBytes); }
    public double getHeapCommittedMB() { return bytesToMB(heapCommittedBytes); }
    public double getHeapMaxMB() { return bytesToMB(heapMaxBytes); }
    public double getNonHeapUsedMB() { return bytesToMB(nonHeapUsedBytes); }
    public double getNonHeapCommittedMB() { return bytesToMB(nonHeapCommittedBytes); }

    private double bytesToMB(long bytes) {
        return bytes / (1024.0 * 1024.0);
    }

    @Override
    public String toString() {
        return String.format(
            "Time: %dms | Heap: %.2fMB/%.2fMB (Max %.2fMB) | Non-Heap: %.2fMB/%.2fMB | GC: %d (Time: %dms) | Alloc Rate: %.2f MB/s",
            timestamp, getHeapUsedMB(), getHeapCommittedMB(), getHeapMaxMB(),
            getNonHeapUsedMB(), getNonHeapCommittedMB(), gcCount, gcTimeMillis, allocationRateMBps
        );
    }
}