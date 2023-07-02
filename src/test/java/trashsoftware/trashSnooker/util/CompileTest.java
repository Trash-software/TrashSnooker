package trashsoftware.trashSnooker.util;

import org.junit.Test;
import trashsoftware.trashSnooker.util.db.DBAccess;

public class CompileTest {
    
    @Test
    public void testDbCorrectlySet() {
        assert DBAccess.SAVE;
    }
}
