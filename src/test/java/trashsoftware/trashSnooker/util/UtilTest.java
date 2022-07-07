package trashsoftware.trashSnooker.util;

import org.junit.Test;

public class UtilTest {
    
    @Test
    public void testBytes() {
        byte[] buf = new byte[8];
        int a = 21876;
        Util.intToBytesN(a, buf, 0, 2);
        long res = Util.bytesToIntN(buf, 0, 2);
        System.out.println(res);
    }
}
