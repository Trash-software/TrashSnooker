package trashsoftware.trashSnooker.recorder;


public class VersionException extends Exception {
    public final int recordPrimaryVersion, recordSecondaryVersion;

    public VersionException(int recordPrimaryVersion, int recordSecondaryVersion) {
        super("Cannot replay older version.");

        this.recordPrimaryVersion = recordPrimaryVersion;
        this.recordSecondaryVersion = recordSecondaryVersion;
    }
}
