package unsw.blackout;

import unsw.response.models.FileInfoResponse;

public class File {
  private String id;
  private String title;
  private String content;
  private int bytes;
  private int cursor;
  private String targetId;

  public File(String id, String title, String content, boolean isSend, String targetId) {
    this.id = id;
    this.title = title;
    this.content = content;
    this.bytes = content.length();
    // adding whole file
    if (!isSend) {
      this.cursor = content.length();
    } else {
      this.cursor = 0;
    }
    this.targetId = targetId;
  }

  public String getTargetId() {
    return targetId;
  }

  public String getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public int getCursor() {
    return cursor;
  }

  // sets cursor to new point, returns true if file is fully uploaded,
  // false if not.
  public boolean incrementCursor(int increment) {
    int curr = cursor + increment;
    if (curr >= bytes) {
      cursor = bytes;
      return true;
    }
    cursor = curr;
    return false;
  }

  public String getContent() {
    return content;
  }

  public int getBytes() {
    return bytes;
  }

  public boolean isComplete() {
    if (cursor == bytes) {
      return true;
    }
    return false;
  }

  public FileInfoResponse getFileInfo() {
    String fn = title;
    String fd = content;
    int cur = cursor;
    boolean isFileComplete = isComplete();

    return new FileInfoResponse(fn, fd.substring(0, cur), fd.length(), isFileComplete);
  }

  // removes 't' characters from remaining unsent part of file.
  public void removeRemainingT() {
    String newContent = content.substring(0, cursor);
    newContent += content.substring(cursor, bytes).replace("t", "");
    content = newContent;
    cursor = newContent.length();
    bytes = cursor;
  }

  // removes 't' character from all of file.
  public void removeAllT() {
    cursor = 0;
    removeRemainingT();
  }
}
