package unsw.blackout;

import unsw.utils.Angle;

public class Slope {
  private int startAngle;
  private int endAngle;
  private int gradient;

  public Slope(int startAngle, int endAngle, int gradient) {
    this.startAngle = startAngle;
    this.endAngle = endAngle;
    this.gradient = gradient;
  }

  public int getGradient() {
    return gradient;
  }

  // Checks if the position is in the range of the slope
  public boolean isInRange(Angle position) {
    if (position.toDegrees() >= startAngle && position.toDegrees() <= endAngle) {
      return true;
    } else if (startAngle > endAngle) {

      if (position.toDegrees() >= startAngle && position.toDegrees() <= endAngle + 360) {
        return true;
      } else if (position.toDegrees() + 360 >= startAngle && position.toDegrees() <= endAngle) {
        return true;
      }
    }

    return false;
  }

  public double heightOnSlope(Angle pos) {

    Angle diff;
    Angle relativePos;
    if (endAngle < startAngle) {
      diff = Angle.fromDegrees(endAngle + 360 - startAngle);
    } else {
      diff = Angle.fromDegrees(endAngle - startAngle);
    }

    return 0;
  }
}
