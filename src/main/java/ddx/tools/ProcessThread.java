package ddx.tools;

import ddx.common.EmptyThread;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author druidman
 */
public class ProcessThread extends EmptyThread {

    public static final int MB = 1024 * 1024;
    public static final int DEFLATE_LEVEL = 5;
    public static final int[] POSSIBLE_FILE_SIZES = new int[]{1*MB, 2*MB, 4*MB, 8*MB, 16*MB, 32*MB};
    
    private final Random random = new Random(1);
    private final long[] xoshiroState = new long[4];
    private final Keys keys = new Keys();
    private final boolean fillBufferWithRandom;
    private final boolean encryptionEnabled;
    private final boolean compressionEnabled;
    private long totalProcessed; // in bytes
    private long totalWritten; // in bytes
    private double totalTimeConsumed; // in seconds
    private long processingSpeed; // in bytes per second

    private class Keys {
        
        public SecretKeySpec keySpec;
        public IvParameterSpec iv;
    }
    
    private void initKeys() throws Exception {
        
	final String PASSWORD = "suppose to be secret";
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
	byte[] key = sha.digest(PASSWORD.getBytes("UTF-8"));
        if (key.length != 32) throw new Exception("Password hash suppose to be 32 bytes long!");
	byte[] keyA = Arrays.copyOf(key, 16);
        byte[] keyB = Arrays.copyOfRange(key, 16, 32);
        
        keys.keySpec = new SecretKeySpec(keyA, "AES");
        keys.iv = new IvParameterSpec(keyB);
    }
    
    private Cipher getCipher(int mode) throws Exception {
        
        Cipher cipher = Cipher.getInstance(Settings.DEFAULT_CRYPT_ALGORITHM);
        cipher.init(mode, keys.keySpec, keys.iv);

        return cipher;
    }
    
    private byte[] createRandomBuffer(int size) throws Exception {

        byte[] bf = new byte[size];

        if (fillBufferWithRandom) {
        
            int pos = 0;
            
            while (pos < size) {

                long value = Xoshiro256p.getNextLong(xoshiroState);
                
                for (int i = 0; i < 8; i++) {
                    
                    bf[pos++] = (byte)(value & 0xFF);
                    value >>= 8;
                }
            }
        }
        
        return bf;
    }
    
    private void encryptFiles() throws Exception {

        OutputStream eos = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                
                totalWritten++;
            }
        };

        OutputStream oos = eos;
        CipherOutputStream cos = null;
        if (encryptionEnabled) oos = cos = new CipherOutputStream(eos, getCipher(Cipher.ENCRYPT_MODE));
        ZipOutputStream zos = new ZipOutputStream(oos);
        zos.setLevel(compressionEnabled?DEFLATE_LEVEL:0);

        final int supplyPortion = 32 * 1024;
        int c = 1;
        
        while (!stopped) {
            
            zos.putNextEntry(new ZipEntry(String.valueOf(c++)));

            int supplied = 0;
            int fileSize = POSSIBLE_FILE_SIZES[random.nextInt(POSSIBLE_FILE_SIZES.length)];
            
            while (supplied < fileSize) {

                int supply = supplyPortion;
                if (supplied + supply > fileSize) supply = fileSize - supplied;
                zos.write(createRandomBuffer(supply));
                supplied += supply;
                totalProcessed += supply;
            }
            
            zos.closeEntry();
        }

        zos.flush();
        if (cos != null) cos.flush();
        eos.flush();
        zos.close();
        if (cos != null) cos.close();
        eos.close();
    }

    private void processEncrypt() throws Exception {
    
        initKeys();
        
        for (int i = 0; i < xoshiroState.length; i++) xoshiroState[i] = random.nextLong();
        
        long start = System.currentTimeMillis();
        
        encryptFiles();
        
        long end = System.currentTimeMillis();
        totalTimeConsumed = (end - start) / 1_000.0d;
        
        if (totalTimeConsumed > 0.0d) processingSpeed = (long)(totalProcessed / totalTimeConsumed);
        
        doComplete();
    }

    @Override
    public void run(){

        try {
            
            processEncrypt();
        } catch (Exception ex) { ex.printStackTrace(); stopped = true; }
    }
    
    public long getTotalProcessed() {
        return totalProcessed;
    }

    public long getTotalWritten() {
        return totalWritten;
    }

    public double getTotalTimeConsumed() {
        return totalTimeConsumed;
    }

    public long getProcessingSpeed() {
        return processingSpeed;
    }

    public ProcessThread(Settings settings) {
        
        fillBufferWithRandom = settings.fillBufferWithRandom;
        encryptionEnabled = settings.encryptionEnabled;
        compressionEnabled = settings.compressionEnabled;
    }

}