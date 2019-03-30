package cb.util;

import robocode.Rules;
import robocode.util.Utils;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

public class BattleFieldUtils {
    private static final int BOT_WIDTH = 36;
    public static final int BOT_HALF_WIDTH = 18;
    private static final double WALL_BORDER = BOT_HALF_WIDTH + 1.5;
    private static final double SMALLEST_FACTOR = 0.95;
    private static final double BIGGEST_FACTOR = 1.7;
    private static final double MIN_WALL_STICK = 100;
    private static final double MAX_WALL_STICK = 120;
    private double battleFieldWidth;
    private double battleFieldHeight;
    private Rectangle2D.Double battleField;
    private Point2D.Double[] corners;
    private Point2D.Double center;

    public void setDimensions(double battleFieldWidth, double battleFieldHeight) {
        this.battleFieldWidth = battleFieldWidth;
        this.battleFieldHeight = battleFieldHeight;
        this.battleField = new Rectangle2D.Double(BOT_HALF_WIDTH, BOT_HALF_WIDTH, battleFieldWidth - BOT_WIDTH, battleFieldHeight - BOT_WIDTH);
        this.corners = new Point2D.Double[4];
        for (int i = 0; i < 4; i++) {
            this.corners[i] = new Point2D.Double();
            this.corners[i].setLocation(BOT_HALF_WIDTH + (i % 2) * (battleFieldWidth - BOT_WIDTH), i > 1 ? battleFieldHeight - BOT_HALF_WIDTH : BOT_HALF_WIDTH);
        }
        this.center = new Point2D.Double();
        this.center.setLocation(battleFieldWidth / 2, battleFieldHeight / 2);
    }

    public Point2D.Double circle(Point2D.Double sourcePoint, int circleDirection, Point2D.Double enemyLocation, double desiredDistance, double enemyBulletPower) {
        double factor = BattleFieldUtils.limit(SMALLEST_FACTOR, desiredDistance / sourcePoint.distance(enemyLocation), BIGGEST_FACTOR);
        double wallStick = sourcePoint.distance(enemyLocation) * Math.sin(BattleFieldUtils.maximumEscapeAngle(enemyBulletPower));
        wallStick = BattleFieldUtils.limit(MIN_WALL_STICK, wallStick, MAX_WALL_STICK);
        Point2D.Double p = BattleFieldUtils.project(sourcePoint, BattleFieldUtils.absoluteBearing(sourcePoint, enemyLocation) - factor * circleDirection * Math.PI / 2, wallStick);
        return wallSmoothing(sourcePoint, p, circleDirection, wallStick);
    }

    public Point2D.Double wallSmoothing(Point2D.Double location, Point2D.Double destination, int circleDirection, double wallStick) {
        Point2D.Double p = new Point2D.Double(destination.x, destination.y);
        for (int i = 0; !battleField.contains(p) && i < 4; i++) {
            if (p.x < WALL_BORDER) {
                p.x = WALL_BORDER;
                double a = location.x - WALL_BORDER;
                p.y = location.y + circleDirection * Math.sqrt(wallStick * wallStick - a * a);
            } else if (p.y > battleFieldHeight - WALL_BORDER) {
                p.y = battleFieldHeight - WALL_BORDER;
                double a = battleFieldHeight - WALL_BORDER - location.y;
                p.x = location.x + circleDirection * Math.sqrt(wallStick * wallStick - a * a);
            } else if (p.x > battleFieldWidth - WALL_BORDER) {
                p.x = battleFieldWidth - WALL_BORDER;
                double a = battleFieldWidth - WALL_BORDER - location.x;
                p.y = location.y - circleDirection * Math.sqrt(wallStick * wallStick - a * a);
            } else if (p.y < WALL_BORDER) {
                p.y = WALL_BORDER;
                double a = location.y - WALL_BORDER;
                p.x = location.x - circleDirection * Math.sqrt(wallStick * wallStick - a * a);
            }
        }
        return p;
    }

    public MovementState predictPosition(int direction, MovementState initialState, Point2D.Double enemyLocation, Point2D.Double waveSource, long waveTime, double bulletPower, double desiredDistance, double maxVelocity) {
        MovementState state = initialState;
        int counter = 0;
        boolean intercepted = false;
        do {
            MovementCommands commands = goTo(state, circle(state.location, direction, enemyLocation, desiredDistance, bulletPower));
            state = state.predict(commands);
            counter++;
            if (state.location.distance(waveSource) < (counter + 1 + initialState.time - waveTime) * Rules.getBulletSpeed(bulletPower)) {
                intercepted = true;
            }
        } while (!intercepted && counter < 500);
        return state;
    }

    public Point2D.Double maximumEscapeAngle(int direction, MovementState robotState, Point2D.Double fireLocation, double bulletPower) {
        MovementState state = robotState;
        int counter = 0;
        boolean intercepted = false;
        double desiredDistance = fireLocation.distance(robotState.location);
        do {
            MovementCommands commands = goTo(state, circle(state.location, direction, fireLocation, desiredDistance, bulletPower));
            state = state.predict(commands);
            counter++;
            if (state.location.distance(fireLocation) < (counter + 1) * Rules.getBulletSpeed(bulletPower)) {
                intercepted = true;
            }
        } while (!intercepted && counter < 500);

        return state.location;
    }

    public ArrayList<Point2D.Double> predictMovements(MovementState initialState, Point2D.Double destination) {
        MovementState state = initialState;
        int counter = 0;
        ArrayList<Point2D.Double> points = new ArrayList<>();
        do {
            MovementCommands commands = goTo(state, destination);
            state = state.predict(commands);
            counter++;
            points.add(state.location);
        } while (state.location.distance(destination) > 1 && counter < 500);
        return points;
    }

    public MovementCommands goTo(MovementState state, Point2D.Double destination) {
        MovementCommands commands = new MovementCommands();
        double x = destination.x - state.location.x;
        double y = destination.y - state.location.y;
        double angleToTarget = Math.atan2(x, y);
        double targetAngle = Utils.normalRelativeAngle(angleToTarget - state.heading);
        double distance = Math.hypot(x, y);
        double turnAngle = Math.atan(Math.tan(targetAngle));
        if (distance > 1) {
            commands.setTurnAngle(turnAngle);
        }
        double velocity = Math.min(8, Math.cos(turnAngle) * 10) + 1;
        Point2D.Double w;
        do {
            velocity--;
            double len = velocity * velocity / 2 + velocity;
            w = project(state.location, state.heading, len * (targetAngle == turnAngle ? 1 : -1));
        }
        while (velocity > 0 && !battleField.contains(w));
        commands.setMaxVelocity(velocity);
        if (targetAngle == turnAngle) {
            commands.setDistance(distance);
        } else {
            commands.setDistance(-distance);
        }
        if (distance < 15 && Math.abs(turnAngle) > 0.1) {
            commands.setMaxVelocity(0);
        }
        return commands;
    }

    public double getBattleFieldWidth() {
        return battleFieldWidth;
    }

    public double getBattleFieldHeight() {
        return battleFieldHeight;
    }

    public boolean contains(Point2D.Double p, double battleBorder) {
        return p.x >= battleBorder && p.y >= battleBorder && p.x <= battleFieldWidth-battleBorder && p.y <= battleFieldHeight-battleBorder;
    }

    public static Point2D.Double project(Point2D.Double source, double angle, double length) {
        return new Point2D.Double(source.getX() + Math.sin(angle) * length, source.getY() + Math.cos(angle) * length);
    }

    public static double absoluteBearing(Point2D.Double source, Point2D.Double target) {
        return Math.atan2(target.getX() - source.getX(), target.getY() - source.getY());
    }

    public static double limit(double min, double value, double max) {
        return Math.max(min, Math.min(value, max));
    }

    public static double maximumEscapeAngle(double bulletPower) {
        return Math.asin(8.0 / Rules.getBulletSpeed(bulletPower));
    }

    public static Point2D.Double between(Point2D.Double a, Point2D.Double b, double f) {
        return new Point2D.Double(a.x + f * (b.x - a.x), a.y + f * (b.y - a.y));
    }

    public Point2D.Double adjustToBattlefield(Point2D.Double p, double border) {
        return new Point2D.Double(BattleFieldUtils.limit(border, p.x, battleFieldWidth - border), BattleFieldUtils.limit(border, p.y, battleFieldHeight - border));
    }

    private Point2D.Double moveFromCorner(Point2D.Double p, Point2D.Double corner, double cornerRadius) {
        if (p.distance(corner) < cornerRadius) {
            double angle = Math.round(Math.PI / 4 + BattleFieldUtils.absoluteBearing(corner, center) / Math.PI * 4) * Math.PI / 4 - Math.PI / 4;
            Point2D.Double c = BattleFieldUtils.project(corner, angle, cornerRadius * Math.sqrt(2));
            if (c.distance(p) > cornerRadius) {
                return BattleFieldUtils.project(c, BattleFieldUtils.absoluteBearing(c, p), cornerRadius);
            } else {
                return p;
            }
        } else {
            return p;
        }
    }

    public Point2D.Double getClosestCorner(Point2D.Double p) {
        double closest = Double.POSITIVE_INFINITY;
        Point2D.Double closestCorner = corners[0];
        for (int i = 0; i < 4; i++) {
            double distance = p.distance(corners[i]);
            if (distance < closest) {
                closest = distance;
                closestCorner = corners[i];
            }
        }
        return closestCorner;
    }

    public Point2D.Double moveFromCorner(Point2D.Double p, double cornerRadius) {
        return moveFromCorner(p, getClosestCorner(p), cornerRadius);
    }
}
