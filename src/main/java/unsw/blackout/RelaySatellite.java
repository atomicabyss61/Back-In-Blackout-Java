package unsw.blackout;

import unsw.utils.Angle;

public class RelaySatellite extends Satellite {
  private boolean isAntiClockwise;

  public RelaySatellite(String satelliteId, String type, double height, Angle position) {
    super(satelliteId, type, height, position);
    Angle pos = position;
    // outside expected range
    if (pos.toDegrees() < 140 || pos.toDegrees() > 190) {
      // needs to move anti-clockwise
      if (pos.toDegrees() < 140 || pos.toDegrees() >= 345) {
        this.isAntiClockwise = true;
      } else {
        this.isAntiClockwise = false;
      }
    } else {
      this.isAntiClockwise = false;
    }
  }

  public boolean isAntiClockwise() {
    return isAntiClockwise;
  }

  public void setAntiClockwise(boolean isAntiClockwise) {
    this.isAntiClockwise = isAntiClockwise;
  }

  double getRange() {
    return 300000;
  }

  public void move() {

    Angle curr = this.getPosition();
    Angle moved;
    Angle sum;
    Angle finalAngle;
    if (this.isAntiClockwise()) {
      // moved more than a half revolution in the anti-clockwise direction.
      if (curr.toDegrees() > 190 && curr.toDegrees() < 345) {
        moved = Angle.fromDegrees(-1500 / (this.getHeight()) * (180 / Math.PI));
        sum = curr.add(moved);
        this.setAntiClockwise(false);
      } else {
        moved = Angle.fromDegrees(1500 / (this.getHeight()) * (180 / Math.PI));
        sum = curr.add(moved);
      }
    } else {
      // moved more than a half revolution in the clockwise direction.
      if (curr.toDegrees() < 140) {
        moved = Angle.fromDegrees(1500 / (this.getHeight()) * (180 / Math.PI));
        sum = curr.add(moved);
        finalAngle = Angle.fromDegrees(sum.toDegrees() % 360);
        this.setAntiClockwise(true);
      } else {
        moved = Angle.fromDegrees(-1500 / (this.getHeight()) * (180 / Math.PI));
        sum = curr.add(moved);
      }
    }
    finalAngle = Angle.fromDegrees(sum.toDegrees() % 360);
    this.setPosition(finalAngle);
  }

  public boolean isAvailableBandwidth(boolean forSending) {
    return true;
  }

  public int isStorage(Integer additional) {
    return 0;
  }

  public int getRecievingBandwidth() {
    return 2147483647;
  }

  public int getSendingBandwidth() {
    return 2147483647;
  }
}
