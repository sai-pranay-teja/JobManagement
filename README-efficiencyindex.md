# 📊 Efficiency Index Module — CI/CD Performance Benchmarking

This module implements **normalized efficiency indices** for benchmarking CI/CD performance across **GitHub Actions**, **AWS CodeBuild**, and **Jenkins**, using **time**, **memory**, and **cost** metrics. It supports deployment, rollback, and composite efficiency calculations, all standardized on a 0–1 scale.

---

## ⚙️ Efficiency Formulas

### 🟢 Deployment Efficiency Index

```text
Index_deploy = 0.5 × normalized_time + 0.3 × normalized_memory
```

Normalized values:

```text
normalized_time = (t_max − t) / (t_max − t_min)
normalized_memory = (m_max − m) / (m_max − m_min)
```

### 🔁 Rollback Efficiency Index

```text
Index_rollback = (t_max − t) / (t_max − t_min)
```

Only time is considered for rollback efficiency.

### 🧮 Composite Efficiency Index

```text
Index_composite = 0.5 × normalized_time + 0.3 × normalized_memory + 0.2 × normalized_cost
```

Cost normalization:

```text
normalized_cost = (c_max − c) / (c_max − c_min)
```

If all values in a metric range are equal, the normalization defaults to 1.0.

---

## 📁 Java Implementation

```java
public class EfficiencyIndexCalculator {
    private static final double W_TIME = 0.5;
    private static final double W_MEM = 0.3;
    private static final double W_COST = 0.2;

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

    public static double computeCompositeIndex(
            double time, double memDelta, double cost,
            double tMin, double tMax,
            double mMin, double mMax,
            double cMin, double cMax) {
        double nT = safeNorm(time, tMin, tMax);
        double nM = safeNorm(memDelta, mMin, mMax);
        double nC = safeNorm(cost, cMin, cMax);
        return W_TIME*nT + W_MEM*nM + W_COST*nC;
    }

    private static double safeNorm(double val, double min, double max) {
        if (max == min) return 1.0;
        return (max - val) / (max - min);
    }
}
```

---

## 📈 Use Case

This module powers a **statistical benchmarking study** for CI/CD platforms by:

- Scoring individual runs
- Supporting ANOVA/Tukey/Nonparametric analysis
- Enabling visualization via live dashboard (CSV backend)

---

## 👤 Author

Developed by **Sai Pranay Teja** as part of a Master's thesis in empirical software engineering.