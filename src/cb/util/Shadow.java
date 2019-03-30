package cb.util;

public class Shadow {
    private double minGf, maxGf;

    public Shadow(double gf1, double gf2) {
        this.minGf = Math.min(gf1, gf2);
        this.maxGf = Math.max(gf1, gf2);
    }

    public double getMinGf() {
        return minGf;
    }

    public double getMaxGf() {
        return maxGf;
    }
}
