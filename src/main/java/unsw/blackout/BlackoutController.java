package unsw.blackout;

import java.util.ArrayList;
import java.util.List;

import unsw.response.models.EntityInfoResponse;
import unsw.response.models.FileInfoResponse;
import unsw.utils.Angle;
import java.util.Map;
import java.util.HashMap;

public class BlackoutController {
  private List<Device> deviceList = new ArrayList<Device>();
  private List<Satellite> satelliteList = new ArrayList<Satellite>();
  private List<Slope> slopeList = new ArrayList<Slope>();

  public void createDevice(String deviceId, String type, Angle position) {
    Device newDev = null;

    switch (type) {
    case "HandheldDevice":
      newDev = new HandheldDevice(deviceId, type, position, false);
      break;
    case "LaptopDevice":
      newDev = new LaptopDevice(deviceId, type, position, false);
      break;
    case "DesktopDevice":
      newDev = new DesktopDevice(deviceId, type, position, false);
      break;
    default:
      return;
    }

    deviceList.add(newDev);
  }

  public void removeDevice(String deviceId) {
    for (Device myDev : deviceList) {
      if (myDev.getDeviceId().equals(deviceId)) {
        deviceList.remove(myDev);
        return;
      }
    }
  }

  public void createSatellite(String satelliteId, String type, double height, Angle position) {
    Satellite newSat = null;

    switch (type) {
    case "StandardSatellite":
      newSat = new StandardSatellite(satelliteId, type, height, position);
      break;
    case "TeleportingSatellite":
      newSat = new TeleportingSatellite(satelliteId, type, height, position);
      break;
    case "RelaySatellite":
      newSat = new RelaySatellite(satelliteId, type, height, position);
      break;
    default:
      return;
    }

    satelliteList.add(newSat);
  }

  public void removeSatellite(String satelliteId) {
    for (Satellite mySat : satelliteList) {
      if (mySat.getSatelliteId().equals(satelliteId)) {
        satelliteList.remove(mySat);
        return;
      }
    }
  }

  public List<String> listDeviceIds() {
    List<String> ids = new ArrayList<String>();

    for (Device myDev : deviceList) {
      ids.add(myDev.getDeviceId());
    }

    return ids;
  }

  public List<String> listSatelliteIds() {
    List<String> ids = new ArrayList<String>();

    for (Satellite mySat : satelliteList) {
      ids.add(mySat.getSatelliteId());
    }

    return ids;
  }

  public void addFileToDevice(String deviceId, String filename, String content) {
    File newFile = new File(deviceId, filename, content, false, deviceId);

    deviceList.stream().filter(myDev -> myDev.getDeviceId().equals(deviceId)).findFirst().ifPresent(myDev -> {
      myDev.addFile(newFile);
    });
  }

  public EntityInfoResponse getInfo(String id) {
    Device dev = deviceList.stream().filter(myDev -> myDev.getDeviceId().equals(id)).findFirst().orElse(null);
    Satellite sat = satelliteList.stream().filter(mySat -> mySat.getSatelliteId().equals(id)).findFirst().orElse(null);

    Angle position;
    double height;
    String type;
    Map<String, FileInfoResponse> files = new HashMap<>();

    // Acquiring information of device/satelitte.
    if (dev != null) {
      position = dev.getPosition();
      height = dev.getHeight();
      type = dev.getType();
      files = dev.filesToInfo();
    } else {
      position = sat.getPosition();
      height = sat.getHeight();
      type = sat.getType();
      files = sat.filesToInfo();
    }

    return new EntityInfoResponse(id, position, height, type, files);
  }

  // needs to be done in specific order to avoid certain edge cases, hence
  // repeated code.
  public void simulate() {
    // move for satellites.
    for (Satellite currSat : satelliteList) {
      currSat.move();
    }

    // moving 'moving' devices.
    for (Device currDev : deviceList) {
      if (currDev.isMoving()) {
        currDev.move(slopeList);
      }
    }

    // Calculating bandwidth for each type (Bandwidth is as calculated BEFORE any
    // transfers has commenced)
    Map<String, Integer> sendBandwidth = new HashMap<>();
    Map<String, Integer> recieveBandwidth = new HashMap<>();

    for (Satellite currSat : satelliteList) {
      sendBandwidth.put(currSat.getSatelliteId(), currSat.getSendingBandwidth());
      recieveBandwidth.put(currSat.getSatelliteId(), currSat.getRecievingBandwidth());
    }

    // transfer files for 1 tick
    for (Satellite currSat : satelliteList) {
      currSat.transfer(satelliteList, deviceList, sendBandwidth, recieveBandwidth);
    }

    for (Device currDev : deviceList) {
      currDev.transfer(satelliteList, deviceList, sendBandwidth, recieveBandwidth);
    }
  }

  /**
   * Simulate for the specified number of minutes. You shouldn't need to modify
   * this function.
   */
  public void simulate(int numberOfMinutes) {
    for (int i = 0; i < numberOfMinutes; i++) {
      simulate();
    }
  }

  public List<String> communicableEntitiesInRange(String id) {
    Device dev = deviceList.stream().filter(myDev -> myDev.getDeviceId().equals(id)).findFirst().orElse(null);
    Satellite sat = satelliteList.stream().filter(mySat -> mySat.getSatelliteId().equals(id)).findFirst().orElse(null);

    List<String> list = new ArrayList<String>();

    // MAKE SURE ENTITY ONLY ADDS ENTITIES THAT IT IS ALLOWED TO REACH
    if (dev != null) {
      if (dev.getType().equals("DesktopDevice")) {
        dev.inRange(satelliteList, deviceList, list, false, true, true);
      } else {
        dev.inRange(satelliteList, deviceList, list, false, false, true);
      }
      list.remove(dev.getDeviceId());
    } else {
      if (sat.getType().equals("StandardSatellite")) {
        sat.inRange(satelliteList, deviceList, list, true, false, false);
      } else {
        sat.inRange(satelliteList, deviceList, list, false, false, false);
      }
      list.remove(sat.getSatelliteId());
    }

    return list;
  }

  public void sendFile(String fileName, String fromId, String toId) throws FileTransferException {
    Device devFrom = deviceList.stream().filter(myDev -> myDev.getDeviceId().equals(fromId)).findFirst().orElse(null);
    Device devTo = deviceList.stream().filter(myDev -> myDev.getDeviceId().equals(toId)).findFirst().orElse(null);

    Satellite satFrom = satelliteList.stream().filter(mySat -> mySat.getSatelliteId().equals(fromId)).findFirst()
        .orElse(null);
    Satellite satTo = satelliteList.stream().filter(mySat -> mySat.getSatelliteId().equals(toId)).findFirst()
        .orElse(null);

    File sendFile;

    // finding file from object
    if (devFrom != null) {
      sendFile = devFrom.getFile(fileName);
    } else {
      sendFile = satFrom.getFile(fileName);
    }

    // partial file/doesn't exist
    if (sendFile == null || !sendFile.isComplete()) {
      throw new FileTransferException.VirtualFileNotFoundException(fileName);
    }

    // hit bandwidth limit
    if (satFrom != null && !satFrom.isAvailableBandwidth(true)) {
      throw new FileTransferException.VirtualFileNoBandwidthException(satFrom.getSatelliteId());
    }

    if (satTo != null) {
      if (!satTo.isAvailableBandwidth(false) || satTo.getType().equals("RelaySatellite")) {
        throw new FileTransferException.VirtualFileNoBandwidthException(satTo.getSatelliteId());
      }
    }

    // File already exists
    if (devTo != null && devTo.getFile(fileName) != null) {
      throw new FileTransferException.VirtualFileAlreadyExistsException(fileName);
    } else if (satTo != null && satTo.getFile(fileName) != null) {
      throw new FileTransferException.VirtualFileAlreadyExistsException(fileName);
    }

    // storage exception checking
    if (satTo != null) {
      // ret = 0, no storage error, ret = 1 max files reached, ret = 2 max bytes
      // reached
      int storageRet = satTo.isStorage(sendFile.getBytes());
      if (storageRet == 1) {
        throw new FileTransferException.VirtualFileNoStorageSpaceException("Max Files Reached");
      } else if (storageRet == 2) {
        throw new FileTransferException.VirtualFileNoStorageSpaceException("Max Storage Reached");
      }
    }

    // passes all exceptions
    if (devFrom != null) {
      devFrom.addSendQueue(sendFile, toId);
    } else {
      satFrom.addSendQueue(sendFile, toId);
    }

    if (devTo != null) {
      devTo.addRecieveQueue(sendFile, fromId);
    } else {
      satTo.addRecieveQueue(sendFile, fromId);
    }
  }

  public void createDevice(String deviceId, String type, Angle position, boolean isMoving) {
    Device newDev = null;

    switch (type) {
    case "HandheldDevice":
      newDev = new HandheldDevice(deviceId, type, position, isMoving);
      break;
    case "LaptopDevice":
      newDev = new LaptopDevice(deviceId, type, position, isMoving);
      break;
    case "DesktopDevice":
      newDev = new DesktopDevice(deviceId, type, position, isMoving);
      break;
    default:
      return;
    }

    deviceList.add(newDev);
  }

  public void createSlope(int startAngle, int endAngle, int gradient) {
    slopeList.add(new Slope(startAngle, endAngle, gradient));
  }
}
