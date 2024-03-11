package unsw.blackout;

import unsw.utils.MathsHelper;
import unsw.response.models.FileInfoResponse;
import unsw.utils.Angle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Satellite {
  private String satelliteId;
  private String type;
  private double height;
  private Angle position;
  private ArrayList<File> files = new ArrayList<File>();
  private ArrayList<File> sendingQueue = new ArrayList<File>();
  private ArrayList<File> recievingQueue = new ArrayList<File>();

  public Satellite(String satelliteId, String type, double height, Angle position) {
    this.satelliteId = satelliteId;
    this.type = type;
    this.height = height;
    this.position = position;
  }

  public ArrayList<File> getSendingQueue() {
    return sendingQueue;
  }

  public ArrayList<File> getRecievingQueue() {
    return recievingQueue;
  }

  public String getSatelliteId() {
    return satelliteId;
  }

  public String getType() {
    return type;
  }

  public double getHeight() {
    return height;
  }

  public Angle getPosition() {
    return position;
  }

  public void setPosition(Angle position) {
    this.position = position;
  }

  public ArrayList<File> getFiles() {
    return files;
  }

  public void addSendQueue(File newFile, String targetId) {
    File copy = new File(newFile.getId(), newFile.getTitle(), newFile.getContent(), true, targetId);
    this.sendingQueue.add(copy);
  }

  public void removeSendQueue(String title) {
    File remove = null;
    for (File currFile : this.sendingQueue) {
      if (title.equals(currFile.getTitle())) {
        remove = currFile;
      }
    }
    this.sendingQueue.remove(remove);
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
    this.recievingQueue.remove(remove);
  }

  public File getFile(String fileName) {
    for (File currFile : this.files) {
      if (currFile.getTitle().equals(fileName)) {
        return currFile;
      }
    }

    for (File currFile : this.getRecievingQueue()) {
      if (currFile.getTitle().equals(fileName)) {
        return currFile;
      }
    }
    return null;
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

  // try to change the tranfer method into an interface only involving standard
  // and teleporting satellites.
  public void transfer(List<Satellite> satList, List<Device> devList, Map<String, Integer> sendBandwidth,
      Map<String, Integer> recieveBandwidth) {

    List<File> removeSending = new ArrayList<File>();

    if (sendingQueue.size() == 0) {
      return;
    }

    List<String> withinRange = new ArrayList<String>();
    inRange(satList, devList, withinRange, false, false, false);

    for (File transferFile : sendingQueue) {
      // Finding respective device/satellite that is targetted.
      Device dev = devList.stream().filter(myDev -> myDev.getDeviceId().equals(transferFile.getTargetId())).findFirst()
          .orElse(null);
      Satellite sat = satList.stream().filter(mySat -> mySat.getSatelliteId().equals(transferFile.getTargetId()))
          .findFirst().orElse(null);

      String fileTitle = transferFile.getTitle();

      int targetBandwidth;

      // removes file from entities if out of range and get bandwidths of recieving
      // entity.
      if (dev != null) {
        if (!withinRange.contains(dev.getDeviceId())) {
          dev.removeRecieveSat(fileTitle, this);
          removeSending.add(transferFile);
          continue;
        }
        targetBandwidth = dev.getRecievingBandwidth();
      } else {
        if (!withinRange.contains(sat.getSatelliteId())) {
          sat.removeRecieveSat(fileTitle, this);
          removeSending.add(transferFile);
          continue;
        }
        targetBandwidth = recieveBandwidth.get(sat.getSatelliteId());
      }

      targetBandwidth = Math.min(targetBandwidth, sendBandwidth.get(satelliteId));

      // incrementing cursor by targetBandiwdth (min of sender/reciever)
      transferFile.incrementCursor(targetBandwidth);
      boolean isTransfered;
      File tranFile;

      // checking to see if file has been fully transfered
      if (dev != null) {
        tranFile = dev.getFile(fileTitle);
      } else {
        tranFile = sat.getFile(fileTitle);
      }

      isTransfered = tranFile.incrementCursor(targetBandwidth);

      // remove file from both queues if uploaded, and add to normal files list.
      if (isTransfered) {
        if (dev != null) {
          dev.removeRecieveQueue(fileTitle);
          dev.addFile(tranFile);
        } else {
          sat.removeRecieveQueue(fileTitle);
          sat.addFile(tranFile);
        }
        removeSending.add(transferFile);
      }
    }

    // perform removal of removeSending
    for (File remove : removeSending) {
      sendingQueue.remove(remove);
    }
  }

  public void inRange(List<Satellite> satList, List<Device> devList, List<String> list, boolean fromStandard,
      boolean fromDesktop, boolean fromDevice) {

    // find only satellites in range (every subclass has some requirements except
    // for range)
    for (Satellite currSat : satList) {
      // dfs base case
      if (!list.contains(currSat.getSatelliteId()) && isSatReachable(currSat, fromStandard, fromDesktop, fromDevice)) {
        list.add(currSat.getSatelliteId());
      } else {
        continue;
      }

      // recursive dfs for relay satellites
      if (currSat.getType().equals("RelaySatellite")) {
        currSat.inRange(satList, devList, list, fromStandard, fromDesktop, fromDevice);
      }
    }

    for (Device currDev : devList) {
      // dfs base case
      if (!list.contains(currDev.getDeviceId()) && isDevReachable(currDev, fromStandard, fromDesktop, fromDevice)) {
        list.add(currDev.getDeviceId());
      }
    }
  }

  public boolean isSatReachable(Satellite sat, boolean fromStandard, boolean fromDesktop, boolean fromDevice) {

    // if not in range continue
    if (this.getRange() < MathsHelper.getDistance(height, position, sat.getHeight(), sat.getPosition())) {
      return false;
    }

    // if not visible continue
    if (!MathsHelper.isVisible(height, position, sat.getHeight(), sat.getPosition())) {
      return false;
    }

    // standard to desktop communication is banned
    if (fromDesktop && sat.getType().equals("StandardSatellite")) {
      return false;
    }

    return true;
  }

  public boolean isDevReachable(Device dev, boolean fromStandard, boolean fromDesktop, boolean fromDevice) {

    // if not in range continue
    if (this.getRange() < MathsHelper.getDistance(height, position, dev.getPosition())) {
      return false;
    }

    // if not visible continue
    if (!MathsHelper.isVisible(height, position, dev.getPosition())) {
      return false;
    }

    // standard to desktop communication is banned
    if (fromStandard && dev.getType().equals("DesktopDevice")) {
      return false;
    }

    // device can't communicate with other devices.
    if (fromDevice) {
      return false;
    }

    return true;
  }

  public void addFile(File newFile) {
    files.add(newFile);
  }

  public void removeRecieveSat(String title, Satellite sat) {
    // either current entity or sat just teleported.
    if ((sat.getType().equals("TeleportingSatellite") && ((TeleportingSatellite) sat).isJustTeleported())
        || (type.equals("TeleportingSatellite") && ((TeleportingSatellite) this).isJustTeleported())) {
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

  public void removeRecieveDev(String fileTitle, Device dev) {
    if (type.equals("TeleportingSatellite") && ((TeleportingSatellite) this).isJustTeleported()) {

      dev.removeTFile(fileTitle);
    }
    removeRecieveQueue(fileTitle);
  }

  abstract void move();

  abstract double getRange();

  abstract boolean isAvailableBandwidth(boolean forSending);

  abstract int isStorage(Integer additional);

  abstract int getRecievingBandwidth();

  abstract int getSendingBandwidth();
}
