package metrics;

public class EfficiencyIndexCalculator {
    private static final double W_TIME = 0.7;
    private static final double W_MEM = 0.3;

    public static double computeDeploymentIndex(double time, double memDelta,
                                              double tMin, double tMax,
                                              double mMin, double mMax) {
        double normTime = safeNorm(time, tMin, tMax);
        double normMem = safeNorm(memDelta, mMin, mMax);
        return (W_TIME * normTime) + (W_MEM * normMem);
    }

    public static double computeRollbackIndex(double time, double tMin, double tMax) {
        return safeNorm(time, tMin, tMax);
    }

    private static double safeNorm(double val, double min, double max) {
        if (max == min) return 1.0; // All values equal
        return (max - val) / (max - min);
    }
}