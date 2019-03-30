package cb.util;

import cb.ml.Features;
import robocode.Bullet;
import robocode.Rules;
import robocode.util.Utils;

import java.awt.geom.Point2D;

public class BulletWave {
    private double bulletAngle;
    private long time;
    private Point2D.Double source;
    private double myBulletPower;
    private double bulletVelocity;
    private double distance;
    private double opponentLateralVelocity;
    private double opponentAdvancingVelocity;
    private double opponentCurrentGuessFactor;
    private Point2D.Double target;
    private Point2D.Double minEscapePoint;
    private Point2D.Double maxEscapePoint;
    private int direction;

    public BulletWave(MovementState myState, MovementState opponentState, double bulletPower, double cgf, BattleFieldUtils battleField) {
        time = myState.time;
        source = myState.location;
        target = opponentState.location;
        double absoluteBearing = BattleFieldUtils.absoluteBearing(myState.location, opponentState.location);
        double latVel = opponentState.velocity * Math.sin(opponentState.heading - absoluteBearing);
        direction = opponentState.velocity * Math.sin(opponentState.heading - absoluteBearing) > 0 ? 1 : -1;
        opponentLateralVelocity = latVel * direction;
        opponentAdvancingVelocity = opponentState.velocity * Math.cos(opponentState.heading - absoluteBearing);
        myBulletPower = bulletPower;
        bulletVelocity = Rules.getBulletSpeed(this.myBulletPower);
        distance = myState.location.distance(opponentState.location);
        MovementState robotState = new MovementState(time, opponentState.location, opponentState.heading, opponentState.velocity);
        minEscapePoint = battleField.maximumEscapeAngle(-direction, robotState, myState.location, bulletPower);
        maxEscapePoint = battleField.maximumEscapeAngle(direction, robotState, myState.location, bulletPower);
        //minEscapeAngle = BattleFieldUtils.absoluteBearing(myState.location, minEscapePoint);
        //maxEscapeAngle = BattleFieldUtils.absoluteBearing(myState.location, maxEscapePoint);
        opponentCurrentGuessFactor = cgf;
    }

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
        features.setFeature("opponentLateralVelocity", opponentLateralVelocity);
        features.setFeature("opponentAdvancingVelocity", opponentAdvancingVelocity);
        features.setFeature("opponentCurrentGuessFactor", opponentCurrentGuessFactor);
        features.setFeature("distance", distance);
        features.setFeature("myBulletPower", myBulletPower);
        return features;
    }

    public void setBullet(Bullet bullet) {
        //TODO check this stuff
        //this.source = new Point2D.Double();
        //this.source.setLocation(source);
        this.bulletAngle = bullet.getHeadingRadians();
        //this.myBulletPower = power;
        //this.bulletVelocity = Rules.getBulletSpeed(power);
    }

    public Point2D.Double getLocation(long time) {
        return BattleFieldUtils.project(source, bulletAngle, (time-this.time) * bulletVelocity);
    }

    public double getDiffUntilHit(Point2D.Double location, long time) {
        return source.distance(location) - (time - this.time) * bulletVelocity;
    }

    public double getMyBulletPower() {
        return myBulletPower;
    }


    public long getTime() {
        return time;
    }
}
