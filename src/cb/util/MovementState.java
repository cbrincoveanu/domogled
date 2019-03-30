package cb.util;

import robocode.Rules;
import robocode.util.Utils;

import java.awt.geom.Point2D;

public class MovementState {
    public long time;
    public Point2D.Double location;
    public double heading;
    public double velocity;

    public MovementState(long time, Point2D.Double location, double heading, double velocity) {
        this.time = time;
        this.location = location;
        this.heading = heading;
        this.velocity = velocity;
    }

    public MovementState predict(MovementCommands commands) {
        double v;
        if (this.velocity * commands.getDistance() < 0) {
            v = this.velocity + 2 * Math.signum(commands.getDistance());
        } else {
            v = this.velocity + Math.signum(commands.getDistance());
            if (Math.abs(v) > commands.getMaxVelocity()) {
                v = commands.getMaxVelocity() * Math.signum(v);
            }
        }
        v = BattleFieldUtils.limit(-8, v, 8);
        double maxTurning = Rules.getTurnRateRadians(v);
        double h = Utils.normalRelativeAngle(this.heading + BattleFieldUtils.limit(-maxTurning, commands.getTurnAngle(), maxTurning));
        return new MovementState(this.time + 1, BattleFieldUtils.project(this.location, h, v), h, v);
    }
}
