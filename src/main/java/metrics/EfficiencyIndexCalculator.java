// package metrics;

// public class EfficiencyIndexCalculator {
//     private static final double W_TIME = 0.7;
//     private static final double W_MEM  = 0.3;

//     public static double computeIndex(
//         int t, int tMin, int tMax,
//         long m, long mMin, long mMax
//     ) {
//         double normT = (double)(t - tMin) / (tMax - tMin);
//         double normM = (double)(m - mMin) / (mMax - mMin);
//         return W_TIME * normT + W_MEM * normM;
//     }
// }


package metrics;

public class EfficiencyIndexCalculator {
    private static final double W_TIME = 0.7;
    private static final double W_MEM  = 0.3;

    public static double computeIndex(
        double t, double tMin, double tMax,
        double m, double mMin, double mMax
    ) {
        // Higher index for lower time/memory
    double normT = (tMax != tMin) ? (tMax - t) / (tMax - tMin) : 1.0;
    double normM = (mMax != mMin) ? (mMax - m) / (mMax - mMin) : 1.0;
        return (W_TIME * normT) + (W_MEM * normM);
    }
}