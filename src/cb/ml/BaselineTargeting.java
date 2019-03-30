package cb.ml;

import cb.util.KdTree;

import java.util.List;

public class BaselineTargeting implements Targeting {
    //TODO add implementation without bins
    private static final int BINS = 31;
    private KdTree<Double> tree = new KdTree.Manhattan<>(4, 500);

    @Override
    public void addPoint(Features features, double guessFactor, ObservationType observationType) {
        if (observationType != ObservationType.BULLETHITBULLET) {
            tree.addPoint(getLocation(features), guessFactor);
        }
    }

    @Override
    public double aim(Features features) {
        List<KdTree.Entry<Double>> entries = tree.nearestNeighbor(getLocation(features), 13, false);
        double[] c = new double[BINS];
        for (KdTree.Entry<Double> entry : entries) {
            int index = (int) Math.round((BINS - 1) / 2.0 * (entry.value + 1));
            for (int i = 0; i < BINS; i++) {
                c[i] += 1.0 / (1 + Math.pow(i-index, 2)) / (1 + entry.distance);
            }
        }
        int best = (BINS - 1) / 2;
        for (int i = 0; i < BINS; i++) {
            if (c[i] > c[best]) {
                best = i;
            }
        }
        return (double)(best - (BINS - 1) / 2) / ((BINS - 1) / 2.0);
    }

    private double[] getLocation(Features features) {
        double[] location = new double[4];
        location[0] = features.getFeature("opponentAdvancingVelocity") / 16.0;
        location[1] = features.getFeature("myBulletPower") / 3.0;
        location[2] = 1.0 / (1.0 + features.getFeature("distance") / 160.0);
        location[3] = features.getFeature("opponentLateralVelocity") / 8.0;
        return location;
    }
}
