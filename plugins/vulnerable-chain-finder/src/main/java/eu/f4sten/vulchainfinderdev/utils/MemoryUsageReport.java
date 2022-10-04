package eu.f4sten.vulchainfinderdev.utils;

public class MemoryUsageReport {
    private static final double GIGA = 1024.0 * 1024.0 * 1024.0;

    public static double getCurrentMemoryUsage() {
        var curMax = Runtime.getRuntime().totalMemory() / GIGA;
        var curFree = Runtime.getRuntime().freeMemory() / GIGA;
        return curMax - curFree;
    }

    public static double getMaxHeapSize() {
        return Runtime.getRuntime().totalMemory() / GIGA;
    }

    public static String getMemoryUsageInfo() {
        return String.format(" [%.1fGB/%.1fGB]", getCurrentMemoryUsage(), getMaxHeapSize());
    }
}
