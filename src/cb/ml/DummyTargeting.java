package cb.ml;

public class DummyTargeting implements Targeting {
    private static final int BINS = 31;
    private double[] c = new double[BINS];
    @Override
    public void addPoint(Features features, double guessFactor, ObservationType observationType) {
        if (observationType != ObservationType.BULLETHITBULLET) {
            int index = (int) Math.round((BINS - 1) / 2.0 * (guessFactor + 1));
            for (int i = 0; i < BINS; i++) {
                c[i] += 1.0 / (1 + Math.pow(i-index, 2));
            }
        }
    }

    @Override
    public double aim(Features features) {
        int best = (BINS - 1) / 2;
        for (int i = 0; i < BINS; i++) {
            if (c[i] > c[best]) {
                best = i;
            }
        }
        return (double)(best - (BINS - 1) / 2) / ((BINS - 1) / 2.0);
    }
}
