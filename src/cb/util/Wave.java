package cb.util;

import cb.ml.Features;
import cb.ml.Movement;
import robocode.Rules;
import robocode.util.Utils;

import java.awt.geom.Point2D;
import java.util.ArrayList;

public class Wave {
    private static final int BOT_HALF_WIDTH = 18;
    private static final int BOT_WIDTH = 36;
    private ArrayList<Shadow> shadows = new ArrayList<>();
    private Point2D.Double source;
    private long time;
    private double enemyBulletPower;
    private double bulletVelocity;
    private double angle;
    private double distance;
    private double myLateralVelocity;
    private double myLateralAcceleration;
    private double myAdvancingVelocity;
    private int direction;

    public Wave(MovementState opponentState, double energyLoss, MovementState myOld2State, MovementState myOld3State) {
        source = new Point2D.Double();
        source.setLocation(opponentState.location);
        time = opponentState.time;
        enemyBulletPower = energyLoss;
        bulletVelocity = Rules.getBulletSpeed(energyLoss);
        angle = BattleFieldUtils.absoluteBearing(opponentState.location, myOld2State.location);
        distance = myOld2State.location.distance(opponentState.location);
        double latVel = myOld2State.velocity * Math.sin(myOld2State.heading - angle);
        double latAcc = (myOld2State.velocity - myOld3State.velocity) * Math.sin(myOld2State.heading - angle);
        direction = latVel >= 0 ? 1 : -1;
        myLateralVelocity = latVel * direction;
        myLateralAcceleration = latAcc * direction;
        myAdvancingVelocity = myOld2State.velocity * Math.cos(myOld2State.heading - angle);
    }

    public Point2D.Double getSource() {
        return source;
    }

    public long getTime() {
        return time;
    }

    public double getEnemyBulletPower() {
        return enemyBulletPower;
    }

    public double getAngle(double guessFactor) {
        return angle + direction * guessFactor * BattleFieldUtils.maximumEscapeAngle(enemyBulletPower);
    }


    public double getAngle() {
        return getAngle(0);
    }

    public double getGuessFactor(Point2D.Double point) {
        double a = BattleFieldUtils.absoluteBearing(source, point);
        return direction * Utils.normalRelativeAngle(a - angle) / BattleFieldUtils.maximumEscapeAngle(enemyBulletPower);
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
        features.setFeature("myLateralVelocity", myLateralVelocity);
        features.setFeature("myLateralAcceleration", myLateralAcceleration);
        features.setFeature("myAdvancingVelocity", myAdvancingVelocity);
        features.setFeature("distance", distance);
        features.setFeature("enemyBulletPower", enemyBulletPower);
        return features;
    }

    public ArrayList<Shadow> getShadows() {
        return shadows;
    }

    public boolean hasPassed(Point2D.Double location, long time) {
        double timeUntilPass = (location.distance(source) + BattleFieldUtils.BOT_HALF_WIDTH - (time - this.time) * bulletVelocity) / bulletVelocity;
        return timeUntilPass < 0;
    }
}
