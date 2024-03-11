package blackout;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import unsw.blackout.BlackoutController;
import unsw.blackout.FileTransferException.VirtualFileNoBandwidthException;
import unsw.blackout.FileTransferException.VirtualFileNoStorageSpaceException;
import unsw.blackout.FileTransferException.VirtualFileNotFoundException;
import unsw.response.models.FileInfoResponse;
import unsw.utils.Angle;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static unsw.utils.MathsHelper.RADIUS_OF_JUPITER;

@TestInstance(value = Lifecycle.PER_CLASS)
public class ExtraTests {
  @Test
  public void testStandardSend() {
    BlackoutController controller = new BlackoutController();

    controller.createSatellite("Satellite1", "StandardSatellite", 100 + RADIUS_OF_JUPITER, Angle.fromDegrees(180));
    controller.createDevice("Dev1", "HandheldDevice", Angle.fromDegrees(180));

    controller.addFileToDevice("Dev1", "File1", "Here is the content");

    assertDoesNotThrow(() -> controller.sendFile("File1", "Dev1", "Satellite1"));

    controller.simulate();

    // partially recieved
    assertEquals(new FileInfoResponse("File1", "H", 19, false),
        controller.getInfo("Satellite1").getFiles().get("File1"));

    controller.simulate();

    // out of range
    assertEquals(null, controller.getInfo("Satellite1").getFiles().get("File1"));

  }

  @Test
  public void testTeleprotingSend() {
    BlackoutController controller = new BlackoutController();

    controller.createSatellite("Satellite1", "TeleportingSatellite", 5000 + RADIUS_OF_JUPITER, Angle.fromDegrees(179));
    controller.createDevice("Dev1", "HandheldDevice", Angle.fromDegrees(180));

    controller.addFileToDevice("Dev1", "File1", "Here is the content");

    assertDoesNotThrow(() -> controller.sendFile("File1", "Dev1", "Satellite1"));

    assertEquals(new FileInfoResponse("File1", "", 19, false),
        controller.getInfo("Satellite1").getFiles().get("File1"));
    controller.simulate();

    // partially recieved
    assertEquals(new FileInfoResponse("File1", "Here is the con", 19, false),
        controller.getInfo("Satellite1").getFiles().get("File1"));

    controller.simulate();

    // transfer cancelled but reciever's 't's in should be removed
    assertEquals(null, controller.getInfo("Satellite1").getFiles().get("File1"));
    assertEquals(new FileInfoResponse("File1", "Here is he conen", 16, true),
        controller.getInfo("Dev1").getFiles().get("File1"));

  }

  @Test
  public void testTeleportOppositeMovement() {
    BlackoutController controller = new BlackoutController();

    controller.createSatellite("Satellite1", "TeleportingSatellite", 1000 + RADIUS_OF_JUPITER, Angle.fromDegrees(185));

    controller.simulate();

    Angle newPosition = controller.getInfo("Satellite1").getPosition();

    assertTrue(newPosition.toDegrees() > 185);

    // should be on past 0 position
    controller.simulate(239);

    Angle finalPosition = controller.getInfo("Satellite1").getPosition();

    assertTrue(finalPosition.toDegrees() > 0 && finalPosition.toDegrees() < 180);

  }

  @Test
  public void testStandardFileLimit() {
    BlackoutController controller = new BlackoutController();
    controller.createSatellite("Sat1", "StandardSatellite", 5000 + RADIUS_OF_JUPITER, Angle.fromDegrees(300));
    controller.createDevice("Dev1", "LaptopDevice", Angle.fromDegrees(310));
    String msg = "L";
    controller.addFileToDevice("Dev1", "File1", msg);
    controller.addFileToDevice("Dev1", "File2", msg);
    controller.addFileToDevice("Dev1", "File3", msg);
    controller.addFileToDevice("Dev1", "File4", msg);

    // send 3 files
    assertDoesNotThrow(() -> {
      controller.sendFile("File1", "Dev1", "Sat1");
    });
    controller.simulate();

    assertDoesNotThrow(() -> {
      controller.sendFile("File2", "Dev1", "Sat1");
    });
    controller.simulate();

    assertDoesNotThrow(() -> {
      controller.sendFile("File3", "Dev1", "Sat1");
    });
    controller.simulate();

    assertThrows(VirtualFileNoStorageSpaceException.class, () -> {
      controller.sendFile("File4", "Dev1", "Sat1");
    });
  }

  @Test
  public void testStandardByteLimit() {
    BlackoutController controller = new BlackoutController();
    controller.createSatellite("Sat1", "StandardSatellite", 20000 + RADIUS_OF_JUPITER, Angle.fromDegrees(330));
    controller.createDevice("Dev1", "LaptopDevice", Angle.fromDegrees(310));
    String msg = "this is 30 characters longgggg";
    controller.addFileToDevice("Dev1", "File1", msg);
    controller.addFileToDevice("Dev1", "File2", msg);
    controller.addFileToDevice("Dev1", "File3", msg);

    // send 2 files
    assertDoesNotThrow(() -> {
      controller.sendFile("File1", "Dev1", "Sat1");
    });

    // goes around and back into range of device
    controller.simulate(225);

    assertDoesNotThrow(() -> {
      controller.sendFile("File2", "Dev1", "Sat1");
    });

    // goes around and back into range of device
    controller.simulate(225);

    assertThrows(VirtualFileNoStorageSpaceException.class, () -> {
      controller.sendFile("File3", "Dev1", "Sat1");
    });
  }

  @Test
  public void testDesktopToStandard() {
    BlackoutController controller = new BlackoutController();
    controller.createSatellite("Sat1", "StandardSatellite", 20000 + RADIUS_OF_JUPITER, Angle.fromDegrees(0));
    controller.createDevice("Dev1", "DesktopDevice", Angle.fromDegrees(0));

    // doesn't communicate
    assertTrue(!controller.communicableEntitiesInRange("Dev1").contains("Sat1"));

    controller.createSatellite("Sat2", "RelaySatellite", 10000 + RADIUS_OF_JUPITER, Angle.fromDegrees(0));

    // doesn't communicate through 1 relay
    assertTrue(!controller.communicableEntitiesInRange("Dev1").contains("Sat1"));

    controller.createSatellite("Sat3", "RelaySatellite", 10000 + RADIUS_OF_JUPITER, Angle.fromDegrees(5));

    // doesn't communicate through 2 relay
    assertTrue(!controller.communicableEntitiesInRange("Dev1").contains("Sat1"));
  }

  @Test
  public void testStandardNoBandwidth() {
    BlackoutController controller = new BlackoutController();
    controller.createSatellite("Sat1", "StandardSatellite", 20000 + RADIUS_OF_JUPITER, Angle.fromDegrees(0));
    controller.createDevice("Dev1", "HandheldDevice", Angle.fromDegrees(0));
    controller.createSatellite("Sat2", "StandardSatellite", 20000 + RADIUS_OF_JUPITER, Angle.fromDegrees(1));

    String msg = "a";
    controller.addFileToDevice("Dev1", "File1", msg);
    controller.addFileToDevice("Dev1", "File2", msg + msg);
    controller.addFileToDevice("Dev1", "File3", msg);

    assertDoesNotThrow(() -> {
      controller.sendFile("File1", "Dev1", "Sat1");
    });

    assertThrows(VirtualFileNoBandwidthException.class, () -> {
      controller.sendFile("File3", "Dev1", "Sat1");
    });
  }

  @Test
  public void testStandardNoFile() {
    BlackoutController controller = new BlackoutController();
    controller.createSatellite("Sat1", "StandardSatellite", 20000 + RADIUS_OF_JUPITER, Angle.fromDegrees(0));
    controller.createDevice("Dev1", "HandheldDevice", Angle.fromDegrees(0));
    controller.createSatellite("Sat2", "StandardSatellite", 20000 + RADIUS_OF_JUPITER, Angle.fromDegrees(1));

    String msg = "a";
    controller.addFileToDevice("Dev1", "File1", msg);
    controller.addFileToDevice("Dev1", "File2", msg + msg);
    controller.addFileToDevice("Dev1", "File3", msg);

    assertDoesNotThrow(() -> {
      controller.sendFile("File1", "Dev1", "Sat1");
    });

    assertThrows(VirtualFileNoBandwidthException.class, () -> {
      controller.sendFile("File3", "Dev1", "Sat1");
    });

    controller.simulate();

    assertDoesNotThrow(() -> {
      controller.sendFile("File3", "Dev1", "Sat2");
    });

    assertDoesNotThrow(() -> {
      controller.sendFile("File2", "Dev1", "Sat1");
    });

    assertThrows(VirtualFileNotFoundException.class, () -> {
      controller.sendFile("File3", "Sat2", "Sat1");
    });
  }

  @Test
  public void testStandardRecievingBandwidth() {
    BlackoutController controller = new BlackoutController();
    controller.createSatellite("Sat1", "StandardSatellite", 20000 + RADIUS_OF_JUPITER, Angle.fromDegrees(0));
    controller.createDevice("Dev1", "HandheldDevice", Angle.fromDegrees(0));
    controller.createSatellite("Sat2", "StandardSatellite", 20000 + RADIUS_OF_JUPITER, Angle.fromDegrees(1));

    String msg = "a";
    controller.addFileToDevice("Dev1", "File1", msg);
    controller.addFileToDevice("Dev1", "File2", msg + msg);
    controller.addFileToDevice("Dev1", "File3", msg);

    assertDoesNotThrow(() -> {
      controller.sendFile("File1", "Dev1", "Sat1");
    });

    assertThrows(VirtualFileNoBandwidthException.class, () -> {
      controller.sendFile("File3", "Dev1", "Sat1");
    });

    controller.simulate();

    assertDoesNotThrow(() -> {
      controller.sendFile("File3", "Dev1", "Sat2");
    });

    assertDoesNotThrow(() -> {
      controller.sendFile("File2", "Dev1", "Sat1");
    });

    assertThrows(VirtualFileNotFoundException.class, () -> {
      controller.sendFile("File3", "Sat2", "Sat1");
    });

    controller.simulate();

    assertThrows(VirtualFileNoBandwidthException.class, () -> {
      controller.sendFile("File3", "Sat2", "Sat1");
    });
  }

}
