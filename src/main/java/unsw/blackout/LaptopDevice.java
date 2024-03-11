package unsw.blackout;

import unsw.utils.Angle;

public class LaptopDevice extends Device {
  public LaptopDevice(String deviceId, String type, Angle position, boolean isMoving) {
    super(deviceId, type, position, isMoving);
  }

  double getRange() {
    return 100000;
  }

  public double getVelocity() {
    return 30;
  }
}
