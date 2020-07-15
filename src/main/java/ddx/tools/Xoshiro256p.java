package ddx.tools;

import java.nio.ByteBuffer;

/**
 *
 * @author druidman
 */
public class Xoshiro256p {


    private static long rol64(long x, int k) {
        
        return (x << k) | (x >> (64 - k));
    }

    public static long getNextLong(long[] s) {
        
        long result = s[0] + s[3];
        long t = s[1] << 17;

        s[2] ^= s[0];
        s[3] ^= s[1];
        s[1] ^= s[2];
        s[0] ^= s[3];

        s[2] ^= t;
        s[3] = rol64(s[3], 45);

        return result;
    }
    
    public static byte[] longToBytes(long x) {
        
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(x);
        return buffer.array();
    }
    
}