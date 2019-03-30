package cb.ml;

import cb.util.BattleFieldUtils;

public class DummyMovement implements Movement {
    private static final int BINS = 31;
    private double[] c = new double[BINS];

    @Override
    public void addPoint(Features features, double guessFactor, ObservationType observationType) {
        if (observationType != ObservationType.MISS) {
            int index = (int) Math.round((BINS - 1) / 2.0 * (guessFactor + 1));
            for (int i = 0; i < BINS; i++) {
                c[i] += 1.0 / (1 + Math.pow(i-index, 2));
            }
        }
    }

    @Override
    public double getProbability(Features features, double minGuessFactor, double maxGuessFactor) {
        double min = BattleFieldUtils.limit(-1, minGuessFactor, 1);
        double max = BattleFieldUtils.limit(-1, maxGuessFactor, 1);
        int minIndex = (int) Math.floor((BINS - 1) / 2.0 * (min + 1));
        int maxIndex = (int) Math.ceil((BINS - 1) / 2.0 * (max + 1));
        double sum = 0;
        int n = 0;
        for (int i = minIndex; i <= maxIndex; i++) {
            sum += c[i];
            n++;
        }
        double total = 0;
        for (int i = 0; i < BINS; i++) {
             total += c[i];
        }
        return sum / total / n * 0.5 * (max - min);
    }
}
