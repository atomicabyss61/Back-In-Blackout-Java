package unsw.blackout;

import unsw.utils.Angle;
import java.util.List;

public class TeleportingSatellite extends Satellite {
  private boolean isAntiClockwise;
  private boolean justTeleported;

  public TeleportingSatellite(String satelliteId, String type, double height, Angle position) {
    super(satelliteId, type, height, position);
    this.isAntiClockwise = true;
    this.justTeleported = false;
  }

  public void move() {
    Angle curr = this.getPosition();
    this.justTeleported = false;

    if (this.isAntiClockwise()) {
      // moved more than a half revolution in the anti-clockwise direction.
      Angle moved = Angle.fromDegrees(1000 / (this.getHeight()) * (180 / Math.PI));
      Angle sum = curr.add(moved);

      if (sum.toDegrees() > 180 && curr.toDegrees() < 180) {
        Angle reset = Angle.fromDegrees(360);
        this.setPosition(reset);
        this.setAntiClockwise(false);
        this.justTeleported = true;
      } else {
        Angle finalAngle = Angle.fromDegrees(sum.toDegrees() % 360);
        this.setPosition(finalAngle);
      }
    } else {
      // moved more than a half revolution in the clockwise direction.
      Angle moved = Angle.fromDegrees(-1000 / (this.getHeight()) * (180 / Math.PI));
      Angle sum = curr.add(moved);
      // System.out.println(sum.toDegrees());
      if (sum.toDegrees() < 180) {
        Angle reset = new Angle();
        this.setPosition(reset);
        this.setAntiClockwise(true);
        this.justTeleported = true;
      } else {
        Angle finalAngle = Angle.fromDegrees(sum.toDegrees() % 360);
        this.setPosition(finalAngle);
      }
    }
  }

  public boolean isAntiClockwise() {
    return isAntiClockwise;
  }

  public void setAntiClockwise(boolean isAntiClockwise) {
    this.isAntiClockwise = isAntiClockwise;
  }

  public double getRange() {
    return 200000;
  }

  public boolean isAvailableBandwidth(boolean forSending) {
    int numSending = this.getSendingQueue().size();
    int numRecieving = this.getRecievingQueue().size();

    if (forSending && numSending < 10) {
      return true;
    } else if (!forSending && numRecieving < 15) {
      return true;
    }
    return false;
  }

  public int isStorage(Integer additional) {
    int storage = 0;
    for (File currFile : this.getFiles()) {
      storage += currFile.getBytes();
    }

    for (File currFile : this.getRecievingQueue()) {
      storage += currFile.getBytes();
    }

    if (storage + additional > 200) {
      return 2;
    }

    return 0;
  }

  public int getRecievingBandwidth() {
    List<File> recieveFiles = this.getRecievingQueue();

    if (recieveFiles.size() == 0) {
      return 15;
    }

    return (int) Math.floor(15 / recieveFiles.size());
  }

  int getSendingBandwidth() {
    List<File> sendFiles = this.getSendingQueue();

    if (sendFiles.size() == 0) {
      return 10;
    }

    return (int) Math.floor(10 / sendFiles.size());
  }

  public boolean isJustTeleported() {
    return justTeleported;
  }
}
