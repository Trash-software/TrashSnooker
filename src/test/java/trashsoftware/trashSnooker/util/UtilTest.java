package trashsoftware.trashSnooker.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.Assert.*;

public class UtilTest {
    
    @Test
    public void testBytes() {
        byte[] buf = new byte[8];
        int a = 21876;
        Util.intToBytesN(a, buf, 0, 2);
        long res = Util.bytesToIntN(buf, 0, 2);
        System.out.println(res);
    }
    
    @Test
    public void testString() {
        byte[] buf = new byte[]{'a', 'b', 'c', 0, 0, 0, 0, 0, 0, 0};
        String s = new String(buf);
        System.out.println(s);
    }
    
    @Test
    public void testHex() {
        System.out.println(Util.decimalToHex(16, 2));
    }
    
    @Test
    public void testCamelSingle() {
        Assert.assertEquals("test", Util.toLowerCamelCase("test"));
    }

    @Test
    public void testCamelUpper() {
        Assert.assertEquals("testString", Util.toLowerCamelCase("TestString"));
    }
    
    @Test
    public void testCamelAllCaps() {
        Assert.assertEquals("testStringTwo", Util.toLowerCamelCase("TEST_STRING_TWO"));
    }

    @Test
    public void testCamelAllCapsWithNumber() {
        Assert.assertEquals("test10StringTwo", Util.toLowerCamelCase("TEST_10_STRING_TWO"));
    }

    @Test
    public void testCamelAllCapsWithNumberTail() {
        Assert.assertEquals("test10StringTwo3", Util.toLowerCamelCase("TEST_10_STRING_TWO_3"));
    }

    @Test
    public void testCamelUpperWithNumber() {
        Assert.assertEquals("test10StringTwo", Util.toLowerCamelCase("Test10StringTwo"));
    }

    @Test
    public void testCamelUpperWithNumberTail() {
        Assert.assertEquals("test10StringTwo3", Util.toLowerCamelCase("Test10StringTwo3"));
    }
}
