package cb.ml;

import cb.util.BattleFieldUtils;
import cb.util.KdTree;

import java.util.List;

public class BaselineMovement implements Movement {
    //TODO add implementation without bins
    private static final int BINS = 31;
    private KdTree<Double> tree = new KdTree.Manhattan<>(4, 500);

    @Override
    public void addPoint(Features features, double guessFactor, ObservationType observationType) {
        if (observationType != ObservationType.MISS) {
            tree.addPoint(getLocation(features), guessFactor);
        }
    }

    @Override
    public double getProbability(Features features, double minGuessFactor, double maxGuessFactor) {
        List<KdTree.Entry<Double>> entries = tree.nearestNeighbor(getLocation(features), 13, false);
        double[] c = new double[BINS];
        for (KdTree.Entry<Double> entry : entries) {
            int index = (int) Math.round((BINS - 1) / 2.0 * (entry.value + 1));
            for (int i = 0; i < BINS; i++) {
                c[i] += 1.0 / (1 + Math.pow(i-index, 2)) / (1 + entry.distance);
            }
        }
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

    private double[] getLocation(Features features) {
        double[] location = new double[4];
        location[0] = features.getFeature("myAdvancingVelocity") / 16.0;
        location[1] = features.getFeature("enemyBulletPower") / 3.0;
        location[2] = 1.0 / (1.0 + features.getFeature("distance") / 160.0);
        location[3] = features.getFeature("myLateralVelocity") / 8.0;
        return location;
    }
}
