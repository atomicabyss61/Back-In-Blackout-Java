package unsw.blackout;

import unsw.utils.Angle;

public class HandheldDevice extends Device {
  public HandheldDevice(String deviceId, String type, Angle position, boolean isMoving) {
    super(deviceId, type, position, isMoving);
  }

  double getRange() {
    return 50000;
  }

  public double getVelocity() {
    return 50;
  }
}
