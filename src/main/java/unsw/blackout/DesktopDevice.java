package unsw.blackout;

import unsw.utils.Angle;

public class DesktopDevice extends Device {
  public DesktopDevice(String deviceId, String type, Angle position, boolean isMoving) {
    super(deviceId, type, position, isMoving);
  }

  @Override
  double getRange() {
    return 200000;
  }

  public double getVelocity() {
    return 20;
  }
}
