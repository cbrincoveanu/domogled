package cb;

import cb.ml.Features;
import cb.ml.Movement;
import cb.util.BattleFieldUtils;
import cb.util.Shadow;
import robocode.util.Utils;

import java.awt.geom.Point2D;
import java.util.ArrayList;

public class Wave {
    private static final int BOT_HALF_WIDTH = 18;
    private static final int BOT_WIDTH = 36;
    Point2D.Double source;
    long time;
    double bulletPower;
    double bulletVelocity;
    double angle;
    double distance;
    double lateralVelocity;
    double lateralAcceleration;
    double advancingVelocity;
    int direction;
    ArrayList<Shadow> shadows = new ArrayList<>();

    public double getAngle(double guessFactor) {
        return angle + direction * guessFactor * BattleFieldUtils.maximumEscapeAngle(bulletPower);
    }

    public double getGuessFactor(Point2D.Double point) {
        double a = BattleFieldUtils.absoluteBearing(source, point);
        return direction * Utils.normalRelativeAngle(a - angle) / BattleFieldUtils.maximumEscapeAngle(bulletPower);
    }

    public double getDanger(Point2D.Double point, Movement movement) {
        double minGF = Double.POSITIVE_INFINITY;
        double maxGF = Double.NEGATIVE_INFINITY;
        for (int x = -BOT_HALF_WIDTH; x <= BOT_HALF_WIDTH; x += BOT_WIDTH) {
            for (int y = -BOT_HALF_WIDTH; y <= BOT_HALF_WIDTH; y += BOT_WIDTH) {
                double gf = getGuessFactor(new Point2D.Double(point.x + x, point.y + y));
                minGF = Math.min(gf, minGF);
                maxGF = Math.max(gf, maxGF);
            }
        }
        return movement.getProbability(getFeatures(), minGF, maxGF);
    }

    public double getTimeUntilHit(Point2D.Double point, long time) {
        return source.distance(point) / bulletVelocity - time + this.time;
    }

    public double getDistanceTraveled(long time) {
        return (time - this.time) * bulletVelocity;
    }

    public void addShadow(Point2D.Double p1, Point2D.Double p2) {
        double gf1 = getGuessFactor(p1);
        double gf2 = getGuessFactor(p2);
        shadows.add(new Shadow(gf1, gf2));
        //TODO merge shadows
    }

    public Features getFeatures() {
        Features features = new Features();
        features.setFeature("latVelocity", lateralVelocity);
        features.setFeature("advVelocity", advancingVelocity);
        features.setFeature("distance", distance);
        features.setFeature("bulletPower", bulletPower);
        return features;
    }

    public ArrayList<Shadow> getShadows() {
        return shadows;
    }
}
