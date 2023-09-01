package trashsoftware.trashSnooker.recorder;

import java.io.IOException;

public class RecordingException extends IOException {
    
    public RecordingException() {
        super();
    }
    
    public RecordingException(String msg) {
        super(msg);
    }
}
