package cb;

import cb.ml.*;
import cb.util.*;
import robocode.*;
import robocode.util.Utils;

import java.awt.*;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class Domogled extends AdvancedRobot {
    private static boolean initialized = false;
    private static BattleFieldUtils battleField;
    private long time;
    private int circleDirection = 1;
    private MovementState myState;
    private MovementState myOldState;
    private MovementState myOld2State;
    private MovementState myOld3State;
    private double myEnergy;
    private double myBulletPower;
    private static double myBulletPowerFired = 0;
    private static double myBulletPowerHit = 0;
    private static String opponentName = null;
    private MovementState opponentState;
    private double opponentEnergy = 100;
    private double opponentBulletPower = 2;
    private Point2D.Double destination = new Point2D.Double();
    private Point2D.Double target = new Point2D.Double();
    private ArrayList<Wave> waves = new ArrayList<>();
    private ArrayList<Point2D.Double> possibleDestinations = new ArrayList<>();
    private ArrayList<BulletWave> bulletWaves = new ArrayList<>();
    private static Targeting targeting = new KNNTargeting();
    private static Movement movement = new KNNMovement();
    private static ArrayList<Observation> targetingObservations = new ArrayList<>();
    private static ArrayList<Observation> movementObservations = new ArrayList<>();

    private final void initializeBot() {
        setColors(new Color(255-172, 255-5, 255-30), new Color(255-235, 255-193, 255-55), new Color(255-255, 255-93, 255-55));
        this.battleField = new BattleFieldUtils();
        this.battleField.setDimensions(getBattleFieldWidth(), getBattleFieldHeight());
        initialized = true;
    }

    @Override
    public final void run() {
        setAdjustRadarForGunTurn(true);
        setAdjustGunForRobotTurn(true);
        while (true) {
            radar();
            movement();
            gun();
            execute();
        }
    }

    private void radar() {
        if (opponentState == null) {
            setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
        } else {
            double angle = BattleFieldUtils.absoluteBearing(myState.location, opponentState.location);
            double maxDistance = (myState.time - opponentState.time) * 8;
            double extraTurn = Math.atan(maxDistance / myState.location.distance(opponentState.location));
            if (Utils.normalRelativeAngle(angle - getRadarHeadingRadians()) < 0) {
                setTurnRadarRightRadians(Utils.normalRelativeAngle(angle - extraTurn - getRadarHeadingRadians()));
            } else {
                setTurnRadarRightRadians(Utils.normalRelativeAngle(angle + extraTurn - getRadarHeadingRadians()));
            }
        }
    }

    private void movement() {
        possibleDestinations.clear();
        Collections.sort(waves, (w1, w2) -> {
            if (w1.getTimeUntilHit (myState.location, time) > w2.getTimeUntilHit (myState.location, time))
                return 1;
            else
                return -1;
        });
        if (waves.size() > 0 && opponentState != null) {
            Wave firstWave = waves.get(0);
            double lowestDanger = Double.POSITIVE_INFINITY;
            for (int direction = -1; direction <= 1; direction += 2) {
                MovementState state = battleField.predictPosition(direction, myState, opponentState.location, firstWave.source, firstWave.time, firstWave.bulletPower, getDesiredDistance(), 8);
                possibleDestinations.add(state.location);
                double danger = firstWave.bulletPower * firstWave.getDanger(state.location, movement);
                if (waves.size() > 1) {
                    Wave secondWave = waves.get(1);
                    double minSecondDanger = secondWave.bulletPower * secondWave.getDanger(state.location, movement);
                    if (waves.size() > 2) {
                        minSecondDanger += 0.5 * waves.get(2).bulletPower * waves.get(2).getDanger(state.location, movement);
                    }
                    for (int d2 = -1; d2 <= 1; d2 += 2) {
                        MovementState secondState = battleField.predictPosition(d2, state, opponentState.location, secondWave.source, secondWave.time, secondWave.bulletPower, getDesiredDistance(), 8);
                        double d = secondWave.bulletPower * secondWave.getDanger(secondState.location, movement);
                        if (waves.size() > 2) {
                            d += 0.5 * waves.get(2).bulletPower * waves.get(2).getDanger(state.location, movement);
                        }
                        minSecondDanger = Math.min(minSecondDanger, d);
                    }
                    danger += minSecondDanger;
                }
                if (danger < lowestDanger) {
                    lowestDanger = danger;
                    circleDirection = direction;
                }
            }
            if (myState.location.distance(opponentState.location) < 150) {
                Point2D.Double destination = battleField.circle(myState.location, circleDirection, opponentState.location, getDesiredDistance(), opponentBulletPower);
                Point2D.Double destination2 = battleField.circle(myState.location, -circleDirection, opponentState.location, getDesiredDistance(), opponentBulletPower);
                if (opponentState.location.distance(destination) < myState.location.distance(destination) && opponentState.location.distance(destination2) > myState.location.distance(destination2)) {
                    circleDirection = -circleDirection;
                }
            }
            destination = battleField.circle(myState.location, circleDirection, opponentState.location, getDesiredDistance(), opponentBulletPower);
        } else if (opponentState != null) {
            destination = battleField.circle(myState.location, circleDirection, opponentState.location, getDesiredDistance(), opponentBulletPower);
            Point2D.Double b = battleField.circle(myState.location, -circleDirection, opponentState.location, getDesiredDistance(), opponentBulletPower);
            if (opponentState.location.distance(destination) < myState.location.distance(destination) && opponentState.location.distance(b) > myState.location.distance(b)) {
                circleDirection = -circleDirection;
                destination = b;
            }
        } else {
            destination.setLocation(myState.location.x, myState.location.y);
        }
        MovementCommands commands = battleField.goTo(myState, destination);
        setTurnRightRadians(commands.getTurnAngle());
        setMaxVelocity(commands.getMaxVelocity());
        setAhead(commands.getDistance());
    }

    private double getDesiredDistance() {
        return BattleFieldUtils.limit(350, myState.location.distance(opponentState.location) + 80, 1000);
    }

    private void gun() {
        if (opponentState != null) {
            calculateBulletPower();
            double currentGuessFactor = 0;
            double smallestDiff = Double.POSITIVE_INFINITY;
            for (int i = 0; i < bulletWaves.size(); i++) {
                BulletWave bulletWave = bulletWaves.get(i);
                double distance = bulletWave.source.distance(opponentState.location);
                double diff = distance - (time - bulletWave.time) * bulletWave.bulletVelocity;
                if (diff < 0) {
                    addTargetingMiss(bulletWave.getFeatures(), bulletWave.getGuessFactor(opponentState.location));
                    bulletWaves.remove(i);
                    i--;
                } else if (diff < smallestDiff) {
                    smallestDiff = diff;
                    currentGuessFactor = bulletWave.getGuessFactor(opponentState.location);
                }
            }
            BulletWave newBulletWave = new BulletWave();
            newBulletWave.time = time;
            newBulletWave.source = myState.location;
            newBulletWave.target = opponentState.location;
            newBulletWave.absoluteBearing = BattleFieldUtils.absoluteBearing(myState.location, opponentState.location);
            double lateralVelocity = opponentState.velocity * Math.sin(opponentState.heading - newBulletWave.absoluteBearing);
            //double lateralAcceleration = opponentState.acceleration * Math.sin(opponentState.heading - newBulletWave.absoluteBearing);
            newBulletWave.direction = opponentState.velocity * Math.sin(opponentState.heading - newBulletWave.absoluteBearing) > 0 ? 1 : -1;
            newBulletWave.lateralVelocity = lateralVelocity * newBulletWave.direction;
            //newBulletWave.lateralAcceleration = lateralAcceleration * newBulletWave.direction;
            newBulletWave.advancingVelocity = opponentState.velocity * Math.cos(opponentState.heading - newBulletWave.absoluteBearing);
            newBulletWave.bulletPower = myBulletPower;
            newBulletWave.bulletVelocity = Rules.getBulletSpeed(myBulletPower);
            newBulletWave.distance = myState.location.distance(opponentState.location);
            //newBulletWave.shotsFired = robot.shotsFired;
            //newBulletWave.timeSinceLastDeceleration = robot.timeSinceLastDeceleration;
            //newBulletWave.timeSinceDirectionChange = robot.timeSinceDirectionChange;
            //newBulletWave.distLast10 = robot.distLast10;
            MovementState robotState = new MovementState(time, opponentState.location, opponentState.heading, opponentState.velocity);
            newBulletWave.minEscapePoint = battleField.maximumEscapeAngle(-newBulletWave.direction, robotState, myState.location, myBulletPower);
            newBulletWave.maxEscapePoint = battleField.maximumEscapeAngle(newBulletWave.direction, robotState, myState.location, myBulletPower);
            newBulletWave.minEscapeAngle = BattleFieldUtils.absoluteBearing(myState.location, newBulletWave.minEscapePoint);
            newBulletWave.maxEscapeAngle = BattleFieldUtils.absoluteBearing(myState.location, newBulletWave.maxEscapePoint);
            newBulletWave.currentGuessFactor = currentGuessFactor;
            //newBulletWave.antiWallAngle = robot.antiWallAngle;
            //newBulletWave.timeSinceBulletShot = timeSinceBulletShot;
            //java.util.List<KdTree.Entry<Double>> list = robot.guessFactors.nearestNeighbor(newBulletWave.getLocation(), Math.max(1, targets.size()), false);
            //for (KdTree.Entry<Double> entry : list) {

            //bulletWaves.add(newBulletWave); TODO check this, and check set firing
            target = newBulletWave.getPoint(targeting.aim(newBulletWave.getFeatures()));
            double targetAngle = BattleFieldUtils.absoluteBearing(myState.location, target);
            double angleTolerance = Math.atan(14 / myState.location.distance(target));
            double randomOffset = (0.5 - Math.random()) * Math.atan(3 / myState.location.distance(target));
            setTurnGunRightRadians(Utils.normalRelativeAngle(targetAngle + randomOffset - getGunHeadingRadians()));
            if (myEnergy > 0.101 && Math.abs(getGunTurnRemainingRadians()) < angleTolerance) {
                Bullet bullet = setFireBullet(myBulletPower);
                if (bullet != null) {
                    myBulletPowerFired += myBulletPower;
                    bulletWaves.add(newBulletWave);
                    newBulletWave.setFiring(new Point2D.Double(bullet.getX(), bullet.getY()), bullet.getHeadingRadians(), bullet.getPower());
                }
            }
        }
    }

    private void calculateBulletPower() {
        double distance = myState.location.distance(opponentState.location);
        if (distance < 140) {
            myBulletPower = myEnergy;
        } else {
            myBulletPower = 1.99;
            double myHitRate = myBulletPowerHit / myBulletPowerFired;
            if (myBulletPowerFired > 20 && myHitRate > 0.25) {
                myBulletPower = 2.49;
                if (myHitRate > 0.33) {
                    myBulletPower = 2.99;
                }
            }
            if (distance > 325 && myEnergy < 63) {
                if (distance > 600 && (myEnergy < 20 || myEnergy - 10 < opponentEnergy)) {
                    myBulletPower = 0.1;
                } else {
                    double powerDownPoint = BattleFieldUtils.limit(35, 63 + 4 * (opponentEnergy - myEnergy), 63);
                    if (myEnergy < powerDownPoint) {
                        double v = myEnergy / powerDownPoint;
                        myBulletPower = Math.min(myBulletPower, v * v * v * 1.99);
                    }
                    if (myEnergy - 25 < opponentEnergy) {
                        myBulletPower = Math.min(myBulletPower, opponentBulletPower * 0.9);
                    }
                }
            }
            myBulletPower = Math.min(myBulletPower, opponentEnergy / 4);
            myBulletPower = Math.min(myBulletPower, myEnergy);
        }
        myBulletPower = BattleFieldUtils.limit(0.1, myBulletPower, 2.999);
    }

    @Override
    public final void onStatus(StatusEvent e) {
        if (!initialized) {
            initializeBot();
        }
        time = e.getTime();
        myEnergy = e.getStatus().getEnergy();
        myOld3State = myOld2State;
        myOld2State = myOldState;
        myOldState = myState;
        myState = new MovementState(time, new Point2D.Double(e.getStatus().getX(), e.getStatus().getY()), e.getStatus().getHeadingRadians(), e.getStatus().getVelocity());
        for (int i = 0; i < waves.size(); i++) {
            Wave wave = waves.get(i);
            double timeUntilPass = (myState.location.distance(wave.source) + BattleFieldUtils.BOT_HALF_WIDTH - (time - wave.time) * wave.bulletVelocity) / wave.bulletVelocity;
            if (timeUntilPass < 0) {
                addMovementMiss(wave.getFeatures(), wave.getGuessFactor(myState.location));
                waves.remove(i);
                i--;
            }
        }
        for (int i = 0; i < bulletWaves.size(); i++) {
            BulletWave bulletWave = bulletWaves.get(i);
            if (bulletWave.isFiring()) {
                Point2D.Double p1 = bulletWave.getLocation(time);
                if (!battleField.contains(p1, 0)) {
                    bulletWaves.remove(i);
                    i--;
                } else {
                    for (int j = 0; j < waves.size(); j++) {
                        Wave wave = waves.get(j);
                        double distance1 = wave.getDistanceTraveled(time);
                        if (wave.source.distance(p1) < distance1) {
                            Point2D.Double p2 = bulletWave.getLocation(time-1);
                            double distance2 = wave.getDistanceTraveled(time-1);
                            if (wave.source.distance(p2) > distance2) {
                                wave.addShadow(p1, p2);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public final void onScannedRobot(ScannedRobotEvent e) {
        opponentName = e.getName();
        Point2D.Double location = BattleFieldUtils.project(myState.location, e.getBearingRadians() + myState.heading, e.getDistance());
        double energyLoss = opponentEnergy - e.getEnergy();
        if (energyLoss > 0.099 && energyLoss < 3.01) {
            opponentBulletPower = energyLoss;
            Wave wave = new Wave();
            wave.source = new Point2D.Double();
            wave.source.setLocation(opponentState.location);
            wave.time = opponentState.time;
            wave.bulletPower = energyLoss;
            wave.bulletVelocity = Rules.getBulletSpeed(energyLoss);
            wave.angle = BattleFieldUtils.absoluteBearing(opponentState.location, myOld2State.location);
            wave.distance = myState.location.distance(opponentState.location);
            double lateralVelocity = myOld2State.velocity * Math.sin(myOld2State.heading - wave.angle);
            double lateralAcceleration = (myOld2State.velocity - myOld3State.velocity) * Math.sin(myOld2State.heading - wave.angle);
            wave.direction = lateralVelocity >= 0 ? 1 : -1;
            wave.lateralVelocity = lateralVelocity * wave.direction;
            wave.lateralAcceleration = lateralAcceleration * wave.direction;
            wave.advancingVelocity = myOld2State.velocity * Math.cos(myOld2State.heading - wave.angle);
            waves.add(wave);
        }
        opponentEnergy = e.getEnergy();
        opponentState = new MovementState(time, location, e.getHeadingRadians(), e.getVelocity());
    }

    @Override
    public void onBulletHit(BulletHitEvent e) {
        myBulletPowerHit += e.getBullet().getPower();
        opponentEnergy -= Rules.getBulletDamage(e.getBullet().getPower());
        Point2D.Double p = new Point2D.Double(e.getBullet().getX(), e.getBullet().getY());
        for (int k = 0; k < bulletWaves.size(); k++) {
            BulletWave bulletWave = bulletWaves.get(k);
            if (bulletWave.isFiring() && Math.abs(bulletWave.bulletPower - e.getBullet().getPower()) < 0.01 && p.distance(bulletWave.getLocation(e.getTime())) < 50) {
                addTargetingHit(bulletWave.getFeatures(), bulletWave.getGuessFactor(p));
                bulletWaves.remove(k);
                k--;
            }
        }
    }

    @Override
    public void onHitByBullet(HitByBulletEvent e) {
        opponentEnergy += Rules.getBulletHitBonus(e.getBullet().getPower());
        Point2D.Double p = new Point2D.Double(e.getBullet().getX(), e.getBullet().getY());
        for (int k = 0; k < waves.size(); k++) {
            Wave wave = waves.get(k);
            if (Math.abs(e.getPower() - wave.bulletPower) < 0.01 && Math.abs(wave.getTimeUntilHit(myState.location, e.getTime())) < 2.5) {
                addMovementHit(wave.getFeatures(), wave.getGuessFactor(p));
                waves.remove(k);
                return;
            }
        }
    }

    @Override
    public void onBulletHitBullet(BulletHitBulletEvent e) {
        Point2D.Double p = new Point2D.Double(e.getHitBullet().getX(), e.getHitBullet().getY());
        for (int k = 0; k < waves.size(); k++) {
            Wave wave = waves.get(k);
            if (Math.abs(e.getHitBullet().getPower() - wave.bulletPower) < 0.01 && Math.abs(wave.getTimeUntilHit(p, e.getTime())) < 2.5) {
                addMovementBulletHitBullet(wave.getFeatures(), wave.getGuessFactor(p));
                waves.remove(k);
                k--;
            }
        }
        p = new Point2D.Double(e.getBullet().getX(), e.getBullet().getY());
        for (int k = 0; k < bulletWaves.size(); k++) {
            BulletWave bulletWave = bulletWaves.get(k);
            if (bulletWave.isFiring() && Math.abs(bulletWave.bulletPower - e.getBullet().getPower()) < 0.01 && p.distance(bulletWave.getLocation(e.getTime())) < 50) {
                addTargetingBulletHitBullet(bulletWave.getFeatures(), bulletWave.getGuessFactor(p));
                bulletWaves.remove(k);
                k--;
            }
        }
    }

    @Override
    public void onRobotDeath(RobotDeathEvent e) {
        opponentState = null;
    }

    @Override
    public void onRoundEnded(RoundEndedEvent e) {
        System.out.println("Weighted total hit rate: " + Math.round(1000*myBulletPowerHit / myBulletPowerFired)/10.0 + "%");
    }

    @Override
    public void onBattleEnded(BattleEndedEvent e) {
        if (opponentName == null) {
            System.out.println("No opponent.");
        } else {
            try {
                DataStorage.writeCSV(getDataFile("movement_" + opponentName + ".csv"), movementObservations);
                DataStorage.writeCSV(getDataFile("targeting_" + opponentName + ".csv"), targetingObservations);
            } catch (IOException e1) {
                System.out.println("Could not write csv file.");
            }
        }
    }

    @Override
    public void onPaint(Graphics2D g) {
        g.setColor(Color.ORANGE);
        for (Point2D.Double point : possibleDestinations) {
            g.fillOval((int) point.getX() - 3, (int) point.getY() - 3, 6, 6);
        }
        g.setColor(Color.GREEN);
        if (destination != null) {
            g.fillOval((int) destination.getX() - 3, (int) destination.getY() - 3, 6, 6);
        }
        g.setColor(Color.DARK_GRAY);
        for (Wave wave : waves) {
            double r = (time - wave.time) * Rules.getBulletSpeed(wave.bulletPower);
            double x = wave.source.x - r;
            double y = wave.source.y - r;
            g.drawOval((int) x, (int) y, (int) (2 * r), (int) (2 * r));
            Point2D.Double t = BattleFieldUtils.project(wave.source, wave.angle, r);
            g.drawLine((int) wave.source.x, (int) wave.source.y, (int) t.x, (int) t.y);
        }
        g.setColor(Color.ORANGE);
        for (BulletWave bulletWave : bulletWaves) {
            if (bulletWave.isFiring()) {
                Point2D.Double p1 = bulletWave.getLocation(time);
                Point2D.Double p2 = bulletWave.getLocation(time-1);
                g.drawLine((int)p1.x, (int)p1.y, (int)p2.x, (int)p2.y);
            }
        }
        for (Wave wave : waves) {
            double minDanger = Double.POSITIVE_INFINITY;
            double maxDanger = Double.NEGATIVE_INFINITY;
            double step = 0.1;
            for (double gf = -1; gf <= 1; gf += step) {
                Point2D.Double p = BattleFieldUtils.project(wave.source, wave.getAngle(gf), (time - wave.time - 1) * Rules.getBulletSpeed(wave.bulletPower));
                double danger = wave.getDanger(p, movement);
                minDanger = Math.min(minDanger, danger);
                maxDanger = Math.max(maxDanger, danger);
            }
            for (double gf = -1; gf <= 1; gf += step) {
                Point2D.Double p = BattleFieldUtils.project(wave.source, wave.getAngle(gf), (time - wave.time - 1) * Rules.getBulletSpeed(wave.bulletPower));
                double danger = wave.getDanger(p, movement);
                if (danger == 0) {
                    g.setColor(Color.BLUE);
                } else {
                    double factor = (danger-minDanger) / (maxDanger-minDanger);
                    double t = 0.2;
                    if (factor > t) {
                        factor = (factor-t)/(1-t);
                        g.setColor(new Color(255, (int)(255*(1-factor)), 0));
                    } else {
                        factor = factor / t;
                        g.setColor(new Color((int)(factor*255), 255, 0));
                    }
                }
                Point2D.Double p1 = BattleFieldUtils.project(wave.source, wave.getAngle(gf), (time - wave.time - 1) * Rules.getBulletSpeed(wave.bulletPower));
                Point2D.Double p2 = BattleFieldUtils.project(wave.source, wave.getAngle(gf), (time - wave.time) * Rules.getBulletSpeed(wave.bulletPower));
                g.drawLine((int)p1.x, (int)p1.y, (int)p2.x, (int)p2.y);
            }
            g.setColor(Color.CYAN);
            for (int i = 0; i < wave.getShadows().size(); i++) {
                Shadow shadow = wave.getShadows().get(i);
                Point2D.Double p1 = BattleFieldUtils.project(wave.source, wave.getAngle(shadow.getMinGf()-step/2), (time - wave.time) * Rules.getBulletSpeed(wave.bulletPower));
                Point2D.Double p2 = BattleFieldUtils.project(wave.source, wave.getAngle(shadow.getMaxGf()+step/2), (time - wave.time) * Rules.getBulletSpeed(wave.bulletPower));
                g.drawLine((int)p1.x, (int)p1.y, (int)p2.x, (int)p2.y);
            }
        }

        g.setColor(Color.LIGHT_GRAY);
        g.drawString("My bullet power: " + Math.round(myBulletPower * 1000) / 1000.0, 10, 10);
        g.setColor(new Color(60, 60, 60));
        g.setColor(Color.RED);
        if (target != null) {
            g.drawLine((int) target.getX() - 18, (int) target.getY(), (int) target.getX() + 18, (int) target.getY());
            g.drawLine((int) target.getX(), (int) target.getY() - 18, (int) target.getX(), (int) target.getY() + 18);
        }
    }

    private void addMovementHit(Features features, double guessFactor) {
        movementObservations.add(new Observation(features, ObservationType.HIT, guessFactor));
        movement.addPoint(features, guessFactor);
    }

    private void addMovementBulletHitBullet(Features features, double guessFactor) {
        movementObservations.add(new Observation(features, ObservationType.BULLETHITBULLET, guessFactor));
        movement.addPoint(features, guessFactor);
    }

    private void addMovementMiss(Features features, double guessFactor) {
        movementObservations.add(new Observation(features, ObservationType.MISS, guessFactor));
        //TODO: add movement.addPoint(features, guessFactor);
    }

    private void addTargetingHit(Features features, double guessFactor) {
        targetingObservations.add(new Observation(features, ObservationType.HIT, guessFactor));
        targeting.addPoint(features, guessFactor);
    }

    private void addTargetingBulletHitBullet(Features features, double guessFactor) {
        targetingObservations.add(new Observation(features, ObservationType.BULLETHITBULLET, guessFactor));
        //TODO: add targeting.addPoint(features, guessFactor);
    }

    private void addTargetingMiss(Features features, double guessFactor) {
        targetingObservations.add(new Observation(features, ObservationType.MISS, guessFactor));
        targeting.addPoint(features, guessFactor);
    }
}
