package cb;

import cb.ml.Features;
import cb.util.BattleFieldUtils;
import robocode.Rules;
import robocode.util.Utils;

import java.awt.geom.Point2D;

public class BulletWave {
    long time;
    Point2D.Double source;
    double bulletPower;
    double bulletVelocity;
    double absoluteBearing;
    double distance;
    double lateralVelocity;
    double advancingVelocity;
    double minEscapeAngle;
    double maxEscapeAngle;
    Point2D.Double target;
    Point2D.Double minEscapePoint;
    Point2D.Double maxEscapePoint;
    double currentGuessFactor;
    int direction;
    boolean firing = false;
    double bulletAngle;

    public Point2D.Double getPoint(double guessFactor) {
        if (guessFactor < 0) {
            return BattleFieldUtils.between(target, minEscapePoint, -guessFactor);
        } else {
            return BattleFieldUtils.between(target, maxEscapePoint, guessFactor);
        }
    }

    public double getGuessFactor(Point2D.Double point) {
        double absoluteBearing = BattleFieldUtils.absoluteBearing(source, target);
        double diff = Utils.normalRelativeAngle(BattleFieldUtils.absoluteBearing(source, point) - absoluteBearing);
        double guessFactor;
        if (diff * direction > 0) {
            guessFactor = diff / Utils.normalRelativeAngle(BattleFieldUtils.absoluteBearing(source, maxEscapePoint) - absoluteBearing);
        } else {
            guessFactor = -diff / Utils.normalRelativeAngle(BattleFieldUtils.absoluteBearing(source, minEscapePoint) - absoluteBearing);
        }
        return BattleFieldUtils.limit(-1, guessFactor, 1);
    }

    public Features getFeatures() {
        Features features = new Features();
        features.setFeature("latVelocity", lateralVelocity);
        features.setFeature("advVelocity", advancingVelocity);
        features.setFeature("distance", distance);
        features.setFeature("bulletPower", bulletPower);
        return features;
    }

    public void setFiring(Point2D.Double source, double headingRadians, double power) {
        this.firing = true;
        this.source = source;
        this.bulletAngle = headingRadians;
        this.bulletPower = power;
        this.bulletVelocity = Rules.getBulletSpeed(power);
    }

    public boolean isFiring() {
        return firing;
    }

    public Point2D.Double getLocation(long time) {
        return BattleFieldUtils.project(source, bulletAngle, (time-this.time) * bulletVelocity);
    }
}
