package unsw.blackout;

import unsw.utils.Angle;

public class StandardSatellite extends Satellite {
  public StandardSatellite(String satelliteId, String type, double height, Angle position) {
    super(satelliteId, type, height, position);
  }

  @Override
  public double getRange() {
    return 150000;
  }

  public void move() {
    Angle curr = this.getPosition();
    Angle moved = Angle.fromDegrees(-2500 / (this.getHeight()) * (180 / Math.PI));
    Angle sum = curr.add(moved);
    if (sum.toDegrees() < 0) {
      sum = Angle.fromDegrees(sum.toDegrees() + 360);
    }
    Angle finalAngle = Angle.fromDegrees(sum.toDegrees() % 360);
    this.setPosition(finalAngle);
  }

  public boolean isAvailableBandwidth(boolean forSending) {
    int numTransfer = this.getRecievingQueue().size() + this.getSendingQueue().size();

    if (numTransfer >= 1) {
      return false;
    }
    return true;
  }

  public int isStorage(Integer additional) {
    int storage = 0;
    for (File currFile : this.getFiles()) {
      storage = storage + currFile.getBytes();
    }

    for (File currFile : this.getRecievingQueue()) {
      storage = storage + currFile.getBytes();
    }

    // finds whatevers lower
    if (this.getFiles().size() >= 3) {
      return 1;
    } else if (storage + additional > 80) {
      return 2;
    }

    return 0;
  }

  int getRecievingBandwidth() {
    return 1;
  }

  int getSendingBandwidth() {
    return 1;
  }

}
