package unsw.blackout;

import unsw.response.models.FileInfoResponse;
import unsw.utils.Angle;
import unsw.utils.MathsHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Device {
  private String deviceId;
  private String type;
  private Angle position;
  private boolean isMoving;
  private double height;
  private ArrayList<File> files = new ArrayList<File>();
  private ArrayList<File> sendingQueue = new ArrayList<File>();
  private ArrayList<File> recievingQueue = new ArrayList<File>();

  public Device(String deviceId, String type, Angle position, boolean isMoving) {
    this.deviceId = deviceId;
    this.type = type;
    this.position = position;
    this.isMoving = isMoving;
    this.height = MathsHelper.RADIUS_OF_JUPITER;
  }

  public boolean isMoving() {
    return isMoving;
  }

  public double getHeight() {
    return height;
  }

  public void setHeight(double height) {
    this.height = height;
  }

  public String getDeviceId() {
    return deviceId;
  }

  public String getType() {
    return type;
  }

  public Angle getPosition() {
    return position;
  }

  public void addSendQueue(File newFile, String targetId) {
    File copy = new File(newFile.getId(), newFile.getTitle(), newFile.getContent(), true, targetId);
    this.sendingQueue.add(copy);
  }

  public void removeSendQueue(String title) {
    for (File currFile : sendingQueue) {
      if (title.equals(currFile.getTitle())) {
        sendingQueue.remove(currFile);
        return;
      }
    }
  }

  public ArrayList<File> getSendingQueue() {
    return sendingQueue;
  }

  public void addRecieveQueue(File newFile, String targetId) {
    File copy = new File(newFile.getId(), newFile.getTitle(), newFile.getContent(), true, targetId);
    this.recievingQueue.add(copy);
  }

  public void removeRecieveQueue(String title) {
    File remove = null;
    for (File currFile : this.recievingQueue) {
      if (title.equals(currFile.getTitle())) {
        remove = currFile;
      }
    }
    recievingQueue.remove(remove);
  }

  public File getFile(String fileName) {
    for (File currFile : this.files) {
      if (currFile.getTitle().equals(fileName)) {
        return currFile;
      }
    }

    for (File currFile : recievingQueue) {
      if (currFile.getTitle().equals(fileName)) {
        return currFile;
      }
    }
    return null;
  }

  public void addFile(File newFile) {
    this.files.add(newFile);
  }

  // converts files of a device to the FileInfoResponse type.
  public Map<String, FileInfoResponse> filesToInfo() {

    Map<String, FileInfoResponse> filesMap = new HashMap<>();

    // Converting files from device/satellite that are complete (held in files).
    for (File oldFile : files) {
      filesMap.put(oldFile.getTitle(), oldFile.getFileInfo());
    }

    // files from recieving queue of the device/satellite. (incomplete files)
    for (File oldFile : recievingQueue) {
      filesMap.put(oldFile.getTitle(), oldFile.getFileInfo());
    }

    return filesMap;
  }

  public void removeRecieveSat(String title, Satellite sat) {
    if (sat.getType().equals("TeleportingSatellite") && ((TeleportingSatellite) sat).isJustTeleported()) {
      // remove all 't' for rest of file, remove from reccieve queue and add to
      // files.
      File editFile = getFile(title);
      editFile.removeRemainingT();
      removeRecieveQueue(title);
      addFile(editFile);
    } else {
      removeRecieveQueue(title);
    }
  }

  void inRange(List<Satellite> satList, List<Device> devList, List<String> list, boolean fromStandard,
      boolean fromDesktop, boolean fromDevice) {

    for (Satellite sat : satList) {
      // dfs base case
      if (list.contains(sat.getSatelliteId())) {
        continue;
      }

      // checking if device can reach satellite
      if (this.isReachable(sat)) {
        list.add(sat.getSatelliteId());
      } else {
        continue;
      }

      // relay recursion statement.
      if (sat.getType().equals("RelaySatellite")) {
        sat.inRange(satList, devList, list, fromStandard, fromDesktop, fromDevice);
      }
    }
  }

  // has all common conditions between 3 devices
  public boolean isReachable(Satellite sat) {

    // if not in range continue
    if (this.getRange() < MathsHelper.getDistance(sat.getHeight(), sat.getPosition(), this.getPosition())) {
      return false;
    }

    // if not visible continue
    if (!MathsHelper.isVisible(sat.getHeight(), sat.getPosition(), this.getPosition())) {
      return false;
    }

    // case for standard to desktop communication prohibited.
    if (sat.getType().equals("StandardSatellite") && type.equals("DesktopDevice")) {
      return false;
    }

    return true;
  }

  // Should have no bandwidth restriction
  public int getRecievingBandwidth() {
    return 2147483647;
  }

  public void transfer(List<Satellite> satList, List<Device> devList, Map<String, Integer> sendBandwidth,
      Map<String, Integer> recieveBandwidth) {

    List<String> removeSending = new ArrayList<String>();

    if (sendingQueue.size() == 0) {
      return;
    }

    List<String> withinRange = new ArrayList<String>();
    this.inRange(satList, devList, withinRange, false, false, false);

    for (File transferFile : sendingQueue) {
      String fileTitle = transferFile.getTitle();
      Satellite sat = satList.stream().filter(mySat -> mySat.getSatelliteId().equals(transferFile.getTargetId()))
          .findFirst().orElse(null);

      if (sat == null) {
        continue;
      }

      int targetBandwidth;

      // finding bottleneck on bandwidth (either current satellite or other device)
      // Also checking within range of device

      if (!withinRange.contains(sat.getSatelliteId())) {
        sat.removeRecieveDev(fileTitle, this);
        removeSending.add(fileTitle);
        continue;
      }

      targetBandwidth = recieveBandwidth.get(sat.getSatelliteId());

      // incrementing cursor by targetBandiwdth (min of sender/reciever)
      transferFile.incrementCursor(targetBandwidth);

      File tranFile = sat.getFile(fileTitle);

      // checking to see if file has been fully transfered
      boolean isTransfered = tranFile.incrementCursor(targetBandwidth);

      // remove file from both queues if uploaded, and add to normal files list.
      if (isTransfered) {
        sat.removeRecieveQueue(fileTitle);
        sat.addFile(new File(transferFile.getId(), fileTitle, transferFile.getContent(), false, sat.getSatelliteId()));
        removeSending.add(fileTitle);
      }
    }

    // removing files that are fully uploaded
    for (String remove : removeSending) {
      removeSendQueue(remove);
    }
  }

  // removes all 't' characters from a file.
  public void removeTFile(String fileTitle) {
    File changeFile = getFile(fileTitle);
    changeFile.removeAllT();
  }

  public void move(List<Slope> slopeList) {

    // performs angle change
    Angle curr = position;
    curr = curr.add(Angle.fromDegrees(this.getVelocity() / (height) * (180 / Math.PI)));
    curr = Angle.fromDegrees(curr.toDegrees() % 360);
    position = curr;

    // lazy approach (just gets a valid slope)
    Slope slope = slopeList.stream().filter(currSlope -> currSlope.isInRange(position)).findFirst().orElse(null);
    // Slope slope = null;
    // for (Slope currSlope : slopeList) {
    // if ()
    // }

    // not on any slope
    if (slope == null) {
      // not on a slope but not on the ground level
      if (height != MathsHelper.RADIUS_OF_JUPITER) {
        height = MathsHelper.RADIUS_OF_JUPITER;
      }
      return;
    }

    // finding height change
    double heightChange = slope.getGradient() * ((this.getVelocity() / height) * (180 / Math.PI));

    if (heightChange + height < MathsHelper.RADIUS_OF_JUPITER) {
      this.setHeight(MathsHelper.RADIUS_OF_JUPITER);
    } else {
      this.setHeight(heightChange + this.getHeight());
    }
  }

  abstract double getVelocity();

  abstract double getRange();
}
