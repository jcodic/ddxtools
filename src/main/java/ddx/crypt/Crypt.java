package ddx.crypt;

import ddx.common.Const;
import ddx.common.PrinterConsole;
import ddx.common.Progress;
import ddx.common.Utils;
import ddx.common.Xoshiro256p;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.Provider;
import java.security.Provider.Service;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author druidman
 */
public class Crypt implements Progress {
    
    public static enum Command {ENCRYPT, ENCRYPT_STRING, ENCRYPT_FILE_STRING, DECRYPT, DECRYPT_STRING, DECRYPT_STRING_FILE, CHECK, TO_HEX, FROM_HEX, SELF_TEST};
    
    private Command command;
    private final Settings settings = new Settings();
    private final Keys keys = new Keys();

    private class Keys {
        
        public String algo;
        public SecretKeySpec keySpec;
        public IvParameterSpec iv;
        public byte[] hashedPassword;
        public int keySize;
        public int ivSize;
    }
    
    private boolean init() throws Exception {
    
        if (settings.useEncryption) {
            
            if (Utils.isEmptyBuffer(settings.password)) {
                
                settings.password = new String(System.console().readPassword("Enter password: ")).getBytes("UTF-8");
            }
            
            if (Utils.isEmptyBuffer(settings.password)) {
                
                Utils.out.println("Password is not provided or bad!");
                return false;
            }
            
            initKeys();
        }
        
        return true;
    }

    private void initKeys() throws Exception {
        
	MessageDigest sha = MessageDigest.getInstance("SHA-256");
	keys.hashedPassword = sha.digest(settings.password);
        if (keys.hashedPassword.length != 32) throw new Exception("Password hash suppose to be 32 bytes long!");
	
        keys.algo = settings.cryptAlgorithm.toUpperCase();
        
        if (keys.algo.startsWith("DESEDE")) {
            
            byte[] keyA = Arrays.copyOfRange(keys.hashedPassword,  0, 24);
            byte[] keyB = Arrays.copyOfRange(keys.hashedPassword, 24, 32);

            keys.keySpec = new SecretKeySpec(keyA, "DESEDE");
            keys.iv = new IvParameterSpec(keyB);
            keys.keySize = keyA.length;
            keys.ivSize = keyB.length;
        } else if (keys.algo.startsWith("AES") && keys.algo.contains("256")) {
        
            sha = MessageDigest.getInstance("SHA-512");
            keys.hashedPassword = sha.digest(settings.password);
            
            byte[] keyA = Arrays.copyOfRange(keys.hashedPassword,  0, 32);
            byte[] keyB = Arrays.copyOfRange(keys.hashedPassword, 32, 48);

            keys.keySpec = new SecretKeySpec(keyA, "AES");
            keys.iv = new IvParameterSpec(keyB);
            keys.keySize = keyA.length;
            keys.ivSize = keyB.length;
        } else if (keys.algo.startsWith("AES") && keys.algo.contains("192")) {
        
            sha = MessageDigest.getInstance("SHA-512");
            keys.hashedPassword = sha.digest(settings.password);
            
            byte[] keyA = Arrays.copyOfRange(keys.hashedPassword,  0, 24);
            byte[] keyB = Arrays.copyOfRange(keys.hashedPassword, 24, 40);

            keys.keySpec = new SecretKeySpec(keyA, "AES");
            keys.iv = new IvParameterSpec(keyB);
            keys.keySize = keyA.length;
            keys.ivSize = keyB.length;
        } else if (keys.algo.startsWith("AES")) {
        
            byte[] keyA = Arrays.copyOfRange(keys.hashedPassword,  0, 16);
            byte[] keyB = Arrays.copyOfRange(keys.hashedPassword, 16, 32);

            keys.keySpec = new SecretKeySpec(keyA, "AES");
            keys.iv = new IvParameterSpec(keyB);
            keys.keySize = keyA.length;
            keys.ivSize = keyB.length;
        } else if (keys.algo.startsWith("DES")) {
        
            byte[] keyA = Arrays.copyOfRange(keys.hashedPassword,  0,  8);
            byte[] keyB = Arrays.copyOfRange(keys.hashedPassword, 24, 32);

            keys.keySpec = new SecretKeySpec(keyA, "DES");
            keys.iv = new IvParameterSpec(keyB);
            keys.keySize = keyA.length;
            keys.ivSize = keyB.length;
        } else if (keys.algo.startsWith("BLOWFISH")) {
        
            byte[] keyA = Arrays.copyOfRange(keys.hashedPassword,  0, 16);
            byte[] keyB = Arrays.copyOfRange(keys.hashedPassword, 24, 32);

            keys.keySpec = new SecretKeySpec(keyA, "BLOWFISH"); 
            keys.iv = new IvParameterSpec(keyB);
            keys.keySize = keyA.length;
            keys.ivSize = keyB.length;
        } else if (keys.algo.startsWith("ARCFOUR")) {
        
            keys.keySpec = new SecretKeySpec(keys.hashedPassword, "ARCFOUR");
            keys.keySize = keys.hashedPassword.length;
            keys.ivSize = 0;
        }
    }
    
    private Cipher getCipher(int mode) throws Exception {
        
        Cipher cipher = Cipher.getInstance(settings.cryptAlgorithm);

        if (keys.algo.contains("ARCFOUR")) {
            
            cipher.init(mode, keys.keySpec);
        } else if (keys.algo.indexOf("/ECB") > 0 || keys.algo.indexOf("/GCM") > 0) {
            
            Utils.out.println("Cipher mode not support IV, dropping IV "+keys.iv.getIV().length * 8+" bits");
            keys.ivSize = 0;
            cipher.init(mode, keys.keySpec);
        } else {
            
            cipher.init(mode, keys.keySpec, keys.iv);
        }
        
        Utils.out.println("Password: initial "+settings.password.length * 8+" bits, hashed "+keys.hashedPassword.length * 8+" bits");
        Utils.out.println("Cipher ["+keys.keySpec.getAlgorithm()+"] key "+keys.keySize * 8+" bits, IV "+keys.ivSize * 8+" bits");

        return cipher;
    }
    
    private void encryptFiles(List<File> files) throws Exception {

        String outputPath = settings.destinationPath;
        
        Path parent = Paths.get(outputPath).getParent();
        if (parent != null && !Files.exists(parent)) Files.createDirectories(parent);

        FileOutputStream fos = new FileOutputStream(outputPath);
        OutputStream oos = fos;
        CipherOutputStream cos = null;
        if (settings.useEncryption) oos = cos = new CipherOutputStream(fos, getCipher(Cipher.ENCRYPT_MODE));
        ZipOutputStream zos = new ZipOutputStream(oos);
        if (settings.compressLevel != null) zos.setLevel(settings.compressLevel);
        
        boolean onlySingleFile = files.size() == 1 && files.get(0).isFile();
    
        for (File file : files) {

            if (file.isDirectory()) {

                Utils.out.print(" [dir] " + file.getPath() + " ");
                
            } else {

                Utils.out.print("[file] " + file.getPath() + " [" + Utils.describeFileLength(file.length()) + "] ");
            }
            
            String path = onlySingleFile?file.getName():Utils.getPathLess(file.getPath(), settings.sourcePath);
            
            if (file.isDirectory()) {
                
                if (!Utils.isEmptyString(path)) {
                    
                    zos.putNextEntry(new ZipEntry(Utils.getZipPath(Utils.completePath(path))));
                    Utils.out.println(" done");
                } else {
                    
                    Utils.out.println(" skip");
                }
                
                continue;
            }
            
            long fileCRC = -1;
            long fileSizeTotal = file.length();
            
            if (settings.calcCRC) {
                
                CRC32 crc = new CRC32();

                FileInputStream streamIn = new FileInputStream(file);
                int loaded;
                byte[] bf = new byte[Const.FILE_BUFFER_SIZE];
                
                while ((loaded = streamIn.read(bf)) > 0) crc.update(bf, 0, loaded);

                fileCRC = crc.getValue();
                streamIn.close();
            }
            
            FileInputStream streamIn = new FileInputStream(file);
            ZipEntry ze = new ZipEntry(Utils.getZipPath(path));
            
            if (settings.calcCRC) {
                
                ze.setSize(fileSizeTotal);
                ze.setCrc(fileCRC);
                ze.setTime(System.currentTimeMillis());
            }
            
            zos.putNextEntry(ze);

            int loaded;
            int loadedTotal = 0;
            byte[] bf = new byte[Const.FILE_BUFFER_SIZE];

            while ((loaded = streamIn.read(bf)) > 0) {

                zos.write(bf, 0, loaded);

                loadedTotal += loaded;
                settings.filesSizeProcessed += loaded;
                if (loadedTotal >= Const.PRINT_STR_AT_EVERY_BYTES) {

                    Utils.out.print(Const.PRINT_STR);
                    loadedTotal -= Const.PRINT_STR_AT_EVERY_BYTES;
                }
            }
            
            Utils.out.println(" done");
            zos.closeEntry();
            streamIn.close();
        }
        
        zos.flush();
        if (cos != null) cos.flush();
        fos.flush();
        zos.close();
        if (cos != null) cos.close();
        fos.close();
    }
    
    private boolean decryptFiles() throws Exception {

        FileInputStream fis = new FileInputStream(settings.sourcePath);
        InputStream ois = fis;
        CipherInputStream cis = null;
        if (settings.useEncryption) ois = cis = new CipherInputStream(fis, getCipher(Cipher.DECRYPT_MODE));
        ZipInputStream zis = new ZipInputStream(ois);
        
        String outputPath = settings.destinationPath;
        
        if (!(Files.exists(Paths.get(outputPath)))) Files.createDirectories(Paths.get(outputPath));
        
        int totalZipEntries = 0;
        
        ZipEntry entry = zis.getNextEntry();
        while (entry != null) {
            
            totalZipEntries++;
            
            Path filePath = Paths.get(outputPath, entry.getName());
            String filePathAbs = filePath.toAbsolutePath().toString();
            long crcOriginal = entry.getCrc();
            boolean crcCheck = crcOriginal != -1;
            String crcInfo = "";
            
            boolean includePath = Utils.includePath(filePathAbs, settings);

            if (!entry.isDirectory()) {

                Utils.out.print("[file] " + filePathAbs + " ");
                
                if (includePath) {
                
                    if (!filePath.getParent().toFile().exists()) Files.createDirectories(filePath.getParent());

                    FileOutputStream fos = new FileOutputStream(filePathAbs);
                    BufferedOutputStream bos = new BufferedOutputStream(fos);

                    CRC32 crc = crcCheck?new CRC32():null;
                
                    int loaded;
                    int loadedTotal = 0;
                    long fileSize = 0;
                    byte[] bf = new byte[Const.FILE_BUFFER_SIZE];

                    while ((loaded = zis.read(bf)) > 0) {

                        if (crcCheck) crc.update(bf, 0, loaded);
                    
                        bos.write(bf, 0, loaded);

                        loadedTotal += loaded;
                        fileSize += loaded;
                        settings.filesSizeProcessed += loaded;
                        if (loadedTotal >= Const.PRINT_STR_AT_EVERY_BYTES) {

                            Utils.out.print(Const.PRINT_STR);
                            loadedTotal -= Const.PRINT_STR_AT_EVERY_BYTES;
                        }
                    }

                    bos.flush();
                    fos.flush();
                    bos.close();
                    fos.close();

                    if (crcCheck) crcInfo = (crcOriginal == crc.getValue())?", crc ok":", crc error!";
                
                    Utils.out.print(Utils.describeFileLength(fileSize));
                }
                
            } else {

                Utils.out.print(" [dir] " + filePathAbs + " ");
                
                if (includePath) {
                
                    Files.createDirectories(filePath);
                }
            }

            Utils.out.println(includePath?(" done"+crcInfo):" skip");
            
            zis.closeEntry();
            entry = zis.getNextEntry();
        }
        
        zis.close();
        if (cis != null) cis.close();
        fis.close();
        
        return totalZipEntries > 0;
    }
    
    private boolean checkFiles() throws Exception {

        FileInputStream fis = new FileInputStream(settings.sourcePath);
        InputStream ois = fis;
        CipherInputStream cis = null;
        if (settings.useEncryption) ois = cis = new CipherInputStream(fis, getCipher(Cipher.DECRYPT_MODE));
        ZipInputStream zis = new ZipInputStream(ois);
        
        int totalZipEntries = 0;
        
        ZipEntry entry = zis.getNextEntry();
        while (entry != null) {
            
            totalZipEntries++;
            
            String filePathAbs = entry.getName();
            long crcOriginal = entry.getCrc();
            boolean crcCheck = crcOriginal != -1;
            String crcInfo = "";
            
            if (!entry.isDirectory()) {

                Utils.out.print("[file] " + filePathAbs + " ");
                
                CRC32 crc = crcCheck?new CRC32():null;
                
                int loaded;
                int loadedTotal = 0;
                long fileSize = 0;
                byte[] bf = new byte[Const.FILE_BUFFER_SIZE];

                while ((loaded = zis.read(bf)) > 0) {

                    if (crcCheck) crc.update(bf, 0, loaded);
                    
                    loadedTotal += loaded;
                    fileSize += loaded;
                    settings.filesSizeProcessed += loaded;
                    if (loadedTotal >= Const.PRINT_STR_AT_EVERY_BYTES) {

                        Utils.out.print(Const.PRINT_STR);
                        loadedTotal -= Const.PRINT_STR_AT_EVERY_BYTES;
                    }
                }
                
                if (crcCheck) crcInfo = (crcOriginal == crc.getValue())?", crc ok":", crc error!";
                
                Utils.out.print(Utils.describeFileLength(fileSize));
                
            } else {

                Utils.out.print(" [dir] " + filePathAbs + " ");
            }

            Utils.out.println(" done"+crcInfo);
            
            zis.closeEntry();
            entry = zis.getNextEntry();
        }
        
        zis.close();
        if (cis != null) cis.close();
        fis.close();
        
        return totalZipEntries > 0;
    }
    
    private String encryptString(String source) throws Exception {

        return encryptBufferToString(source.getBytes("UTF-8"));
    }
    
    private String decryptString(String source) throws Exception {

        ByteArrayInputStream bis = new ByteArrayInputStream(Utils.fromHex(source));
        CipherInputStream cis = new CipherInputStream(bis, getCipher(Cipher.DECRYPT_MODE));
        byte[] decryptedData = new byte[64 * 1024];
        int decryptedLen = 0;
        int i;
        while ((i = cis.read(decryptedData, decryptedLen, decryptedData.length - decryptedLen)) > -1) decryptedLen += i;
        cis.close();
        bis.close();
        
        return new String(decryptedData, 0, decryptedLen, "UTF-8");
    }

    private String encryptBufferToString(byte[] source) throws Exception {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        CipherOutputStream cos = new CipherOutputStream(bos, getCipher(Cipher.ENCRYPT_MODE));
        cos.write(source);
        cos.flush();
        cos.close();
        bos.flush();
        byte[] bf = bos.toByteArray();
        bos.close();
        
        return Utils.toHex(bf);
    }
    
    private byte[] decryptStringToBuffer(String source) throws Exception {

        ByteArrayInputStream bis = new ByteArrayInputStream(Utils.fromHex(source));
        CipherInputStream cis = new CipherInputStream(bis, getCipher(Cipher.DECRYPT_MODE));
        byte[] decryptedData = new byte[Settings.FILE_TO_STRING_MAX_SIZE + 10 * 1024];
        int decryptedLen = 0;
        int i;
        while ((i = cis.read(decryptedData, decryptedLen, decryptedData.length - decryptedLen)) > -1) decryptedLen += i;
        cis.close();
        bis.close();
        
        return Arrays.copyOfRange(decryptedData, 0, decryptedLen);
    }

    private void printEncryption() {
        
        Utils.out.println(5, "Use encryption: "+settings.useEncryption);
        if (settings.useEncryption) Utils.out.println(5, "Algorithm: "+settings.cryptAlgorithm);
    }

    private void printIncludeExclude() {
        
        if (settings.includeFiles != null) 
            for (String item : settings.includeFiles) 
                Utils.out.println("Include: "+item);
        if (settings.excludeFiles != null) 
            for (String item : settings.excludeFiles) 
                Utils.out.println("Exclude: "+item);
    }
    
    private boolean processEncrypt() throws Exception {
    
        if (!new File(settings.sourcePath).exists()) {
            
            Utils.out.println("Source path ["+settings.sourcePath+"] not exists!");
            return false;
        }

        Utils.out.println("Encrypting path ["+settings.sourcePath+"] with settings: ");
        printEncryption();
        Utils.out.println(5, "Compression level: "+(settings.compressLevel!=null?settings.compressLevel:"default"));
        printIncludeExclude();
        Utils.out.println(5, "Output file: "+settings.destinationPath);
        
        long start = System.currentTimeMillis();
        
        Utils.out.print("Scanning "+settings.sourcePath+" ");
        
        List<File> files = Utils.scanForFiles(new File(settings.sourcePath), settings);
        
        Utils.out.println();
        Utils.out.println("Files scanned : "+settings.filesScanned);
        Utils.out.println("Files included: "+settings.filesIncluded);
        Utils.out.println("Files size found: "+Utils.describeFileLength(settings.filesSizeFound));
        Utils.out.println("Files size included: "+Utils.describeFileLength(settings.filesSizeIncluded));
        
        if (settings.filesIncluded == 0) {
            
            Utils.out.println("No files included, nothing todo, exit.");
            return false;
        }
        
        starting();
        encryptFiles(files);
        completed();
        
        long end = System.currentTimeMillis();
        long time = (end - start) / 1_000;
        
        if (time > 0) {
            
            long bytesPerSecong = settings.filesSizeProcessed / time;
            Utils.out.println("Processing speed: "+Utils.describeFileLength(bytesPerSecong)+" per second.");
        }
        
        return true;
    }
    
    private boolean processDecrypt() throws Exception {

        File file = new File(settings.sourcePath);
        if (!file.exists()) {
            
            Utils.out.println("Source file ["+settings.sourcePath+"] not exists!");
            return false;
        }
        if (!file.isFile()) {
            
            Utils.out.println("Source ["+settings.sourcePath+"] is not a file!");
            return false;
        }

        Utils.out.println("Decrypting file ["+settings.sourcePath+"] with settings: ");
        printEncryption();
        printIncludeExclude();
        Utils.out.println(5, "Output path: "+settings.destinationPath);
        
        long start = System.currentTimeMillis();
        
        starting();
        boolean success = decryptFiles();
        completed();
        
        long end = System.currentTimeMillis();
        long time = (end - start) / 1_000;
        
        if (time > 0) {
            
            long bytesPerSecong = settings.filesSizeProcessed / time;
            Utils.out.println("Processing speed: "+Utils.describeFileLength(bytesPerSecong)+" per second.");
        }
        
        return success;
    }
    
    private boolean processCheck() throws Exception {

        File file = new File(settings.sourcePath);
        if (!file.exists()) {
            
            Utils.out.println("Source file ["+settings.sourcePath+"] not exists!");
            return false;
        }
        if (!file.isFile()) {
            
            Utils.out.println("Source ["+settings.sourcePath+"] is not a file!");
            return false;
        }

        Utils.out.println("Checking file ["+settings.sourcePath+"] with settings: ");
        printEncryption();
        
        long start = System.currentTimeMillis();
        
        starting();
        boolean success = checkFiles();
        completed();
        
        long end = System.currentTimeMillis();
        long time = (end - start) / 1_000;
        
        if (time > 0) {
            
            long bytesPerSecong = settings.filesSizeProcessed / time;
            Utils.out.println("Processing speed: "+Utils.describeFileLength(bytesPerSecong)+" per second.");
        }
        
        if (success) {
            
            Utils.out.println("Total content size checked: "+Utils.describeFileLength(settings.filesSizeProcessed));
        } else {
            
            Utils.out.println("Wrong password or file damaged!");
        }
        
        return success;
    }
    
    private boolean processEncryptString() throws Exception {
    
        Utils.out.println("Encrypting string ["+settings.sourceString+"] with settings: ");
        printEncryption();

        if (settings.sourceString == null || settings.sourceString.length() == 0) {
            
            Utils.out.println("String is empty, nothing todo, exit.");
            return false;
        }
        
        settings.destinationString = encryptString(settings.sourceString);
        Utils.out.println("Encrypted string ["+settings.destinationString+"]");
        
        if (settings.copyResultToClipboard) Utils.sendStringToClipboard(settings.destinationString);

        return true;
    }

    private boolean processDecryptString() throws Exception {
    
        Utils.out.println("Decrypting string ["+settings.sourceString+"] with settings: ");
        printEncryption();

        if (settings.sourceString == null || settings.sourceString.length() == 0) {
            
            Utils.out.println("String is empty, nothing todo, exit.");
            return false;
        }
        
        settings.destinationString = decryptString(settings.sourceString);
        Utils.out.println("Decrypted string ["+settings.destinationString+"]");

        if (settings.copyResultToClipboard) Utils.sendStringToClipboard(settings.destinationString);

        return true;
    }

    private boolean processEncryptFileString() throws Exception {
    
        File file = new File(settings.sourcePath);
        
        if (!file.exists()) {
            
            Utils.out.println("Source file ["+settings.sourcePath+"] not exists!");
            return false;
        }

        if (!file.isFile()) {
            
            Utils.out.println("Source ["+settings.sourcePath+"] is not a file!");
            return false;
        }

	int len = (int)file.length();
	if (len > Settings.FILE_TO_STRING_MAX_SIZE) {
            
            Utils.out.println("File size exceed "+Utils.describeFileLength(Settings.FILE_TO_STRING_MAX_SIZE)+", too large.");
            return false;
        }

        Utils.out.println("Encrypting file ["+settings.sourcePath+"] to string with settings: ");
        printEncryption();
        boolean useCompression = settings.compressLevel == null || settings.compressLevel > 0;
        Utils.out.println(5, "Use compression: "+(useCompression?"true":"false"));
        
        long start = System.currentTimeMillis();
	
        starting();

        byte[] bf = new byte[len];
	    
	InputStream stream = new FileInputStream(file);
	int loaded = stream.read(bf);
	stream.close();
	
	if (loaded != len) throw new RuntimeException("File is not loaded properly.");
	
        if (useCompression) bf = CryptUtils.compress(bf, 9);

        settings.destinationString = encryptBufferToString(bf);
        Utils.out.println("Encrypted string ["+settings.destinationString+"]");
        
        completed();
        
        long end = System.currentTimeMillis();
        long time = (end - start) / 1_000;
        
        if (settings.copyResultToClipboard) Utils.sendStringToClipboard(settings.destinationString);

        return true;
    }
    
    private boolean processDecryptStringFile() throws Exception {
    
        Utils.out.println("Decrypting string ["+settings.sourceString+"] with settings: ");
        printEncryption();
        boolean useCompression = settings.compressLevel == null || settings.compressLevel > 0;
        Utils.out.println(5, "Use decompression: "+(useCompression?"true":"false"));
        Utils.out.println(5, "Output file: "+settings.destinationPath);

        if (settings.sourceString == null || settings.sourceString.length() == 0) {
            
            Utils.out.println("String is empty, nothing todo, exit.");
            return false;
        }
        
        if (settings.destinationPath == null || settings.destinationPath.length() == 0) {
            
            Utils.out.println("Destination file is absent, nothing todo, exit.");
            return false;
        }
        
        long start = System.currentTimeMillis();
	
        starting();

        byte[] bf = decryptStringToBuffer(settings.sourceString);
        
        if (useCompression) bf = CryptUtils.decompress(bf);
        
	OutputStream stream = new FileOutputStream(new File(settings.destinationPath));
	
	stream.write(bf);
	stream.flush();
	stream.close();
        
        completed();
        
        long end = System.currentTimeMillis();
        long time = (end - start) / 1_000;
        
        return true;
    }

    private boolean processToHex() throws Exception {
        
        Utils.out.println("["+settings.sourceString+"] -> ["+Utils.toHex(settings.sourceString.getBytes("UTF-8"))+"]");
        return true;
    }
    
    private boolean processFromHex() throws Exception {
        
        Utils.out.println("["+settings.sourceString+"] -> ["+new String(Utils.fromHex(settings.sourceString),"UTF-8")+"]");
        return true;
    }
    

    private boolean processSingleTest(final byte[] bf, String algo, byte[] correctHash) throws Exception {
        
        settings.cryptAlgorithm = algo;
        
        initKeys();
        Cipher cipher = getCipher(Cipher.ENCRYPT_MODE);
        
        byte[] ebf = cipher.doFinal(bf);
        
        MessageDigest md = MessageDigest.getInstance("SHA-256");

        if (!Arrays.equals(correctHash, md.digest(ebf))) return false;
        
        cipher = getCipher(Cipher.DECRYPT_MODE);
        
        byte[] debf = cipher.doFinal(ebf);
        
        return Arrays.equals(bf, debf);
    }

    private boolean processSingleTestString(String algo, String correctString) throws Exception {

        final String BASE_STRING = "Testing is important.";
        
        settings.cryptAlgorithm = algo;
        
        initKeys();
        
        settings.sourceString = BASE_STRING;
        settings.copyResultToClipboard = false;
        
        processEncryptString();
        
        if (!settings.destinationString.equals(correctString)) return false;
    
        settings.sourceString = settings.destinationString;
        
        processDecryptString();
        
        return settings.destinationString.equals(BASE_STRING);
    }

    private void printSelfTestResult(String algo, boolean passed) throws Exception {
        
        StringBuilder sb = new StringBuilder(200);
        sb.append(algo);
        while (sb.length() < 40) sb.append(" ");
        sb.append(" : ");
        sb.append(passed?"PASSED":"FAILED");
        
        Utils.out.println(sb.toString());
    }
    
    private boolean processSelfTest() throws Exception {

        long[] state = new long[4];
        state[0] = Utils.bytesToLong(Utils.fromHex("ABCDF0EFCD7394F2"));
        state[1] = Utils.bytesToLong(Utils.fromHex("DF7D3BC28AF1DEEE"));
        state[2] = Utils.bytesToLong(Utils.fromHex("091B12CC752DBC11"));
        state[3] = Utils.bytesToLong(Utils.fromHex("10BCFFCDEB1000FF"));
        
        byte[] bf = Xoshiro256p.createRandomBuffer(1024 * 1024, state);

        settings.password = Utils.fromHex("DABABA00F0FAD5C0DAFF");
        
        String[] algos = {
            "AES/CBC/PKCS5Padding",
            "AES/OFB/PKCS5Padding",
            "AES/ECB/NoPadding",
            "AES_192/ECB/NoPadding",
            "AES_256/ECB/NoPadding",
            "DES/CBC/PKCS5Padding",
            "DES/ECB/NoPadding",
            "DESEDE/CBC/PKCS5Padding",
            "DESEDE/ECB/NoPadding",
            "BLOWFISH/CBC/PKCS5Padding",
            "BLOWFISH/ECB/NoPadding"
        };

        String[] correctHashes = {
            "56DA2F90BB42EB65C03C1D74DC1AFA06948F913EFA510E7B2E430FBB025A360F",
            "2EAA3A8630A59DEE4A0E4E8FF317FAC2E3D0F98A17F06213939D352A579DE5B4",
            "30924D7FEE5B8632B5BFF2E89896C7E41DE19899608C142C55428DC2916BA2EB",
            "81C9522CF1004367114493F454690376BF17015547BE8EB519FE0232E79C0960",
            "BA99F8D4C15529DD0CE53C15247849D326F0233934F11DC8B8E202333DA2E6F1",
            "20D8299DD5D343C9F6627D7AAD198A16B10C180AA536DB113E4B5C5A7FC215E6",
            "A7AD7A540F5819E08EB528EE89CE646EFCB5EE98CEB7EC4AF3AC521479EB7A16",
            "45CE596D26013662F5B8C1A355534F7FB085DABBC8E5696AFB7BECB1AFECE3A4",
            "6D6EEA6658C053397C1C3BC184366EF0667B202E03A7143CF62F0E42EAA33E95",
            "DAC04D8DF14F45EFE9046BDD5A31ABE18B05E16B1B26B053B4413DA5E53CCA7D",
            "0AAD83A084717BF540178BF68460D1705672ECFB045D4177571F8C0B6ECCBF13"
        };

        String[] correctStrings = {
            "13A4246E86A86C306CDD7259997EB2182B9F45700613B171BDABAFA8FB3D562C",
            "319D8FA14B6E94EEBBAF6F4FAA6DE6C11B49F7472DC4223D26C20B3D09B3E3B9",
            "",
            "",
            "",
            "DC695D7F8EA12C58B7B0BC1F2960323DC22087A946954F18",
            "",
            "010A3F6256E0A3FBBFA5BDBDC6DEAC1A65F3A58BE766942B",
            "",
            "3682C8F585F701D5EC4A6AEC801AEEFA6B72BD2ECA421D9A",
            ""
        };
        
        boolean allPassed = true;
        
        for (int i = 0; i < algos.length; i++) {
            
            ((PrinterConsole)Utils.out).setPrintAllowed(false);
            boolean passed = processSingleTest(bf, algos[i], Utils.fromHex(correctHashes[i]));
            if (!passed) allPassed = false;
            ((PrinterConsole)Utils.out).setPrintAllowed(true);
            printSelfTestResult(algos[i], passed);
        }
        
        for (int i = 0; i < algos.length; i++) {
            
            if (Utils.isEmptyString(correctStrings[i])) continue; // exclude nopadding modes
            
            ((PrinterConsole)Utils.out).setPrintAllowed(false);
            boolean passed = processSingleTestString(algos[i], correctStrings[i]);
            if (!passed) allPassed = false;
            ((PrinterConsole)Utils.out).setPrintAllowed(true);
            printSelfTestResult(algos[i] + " (string)", passed);
        }
        
        printSelfTestResult("ALL TESTS", allPassed);
        
        return allPassed;
    }
    
    public boolean run() throws Exception {
        
        if (!init()) return false;
        
        switch (command) {
            
            case ENCRYPT                : return processEncrypt();
            case ENCRYPT_STRING         : return processEncryptString();
            case ENCRYPT_FILE_STRING    : return processEncryptFileString();
            case DECRYPT                : return processDecrypt();
            case DECRYPT_STRING         : return processDecryptString();
            case DECRYPT_STRING_FILE    : return processDecryptStringFile();
            case CHECK                  : return processCheck();
            case TO_HEX                 : return processToHex();
            case FROM_HEX               : return processFromHex();
            case SELF_TEST              : return processSelfTest();
            default                     : return false;
        }
    }
    
    public void processArg(String arg) throws Exception {
        
        String argCompress = "compress=";
        String argUseEncryption = "encrypt=";
        String argPassword = "password=";
        String argPasswordHex = "passwordhex=";
        String argAlgo = "algo=";
        String argInc = "inc=";
        String argExc = "exc=";
        String argCRC = "crc=";
        String argToClip = "toclip";
        if (arg.startsWith(argCompress)) settings.compressLevel = Integer.parseInt(arg.substring(argCompress.length())); else
        if (arg.startsWith(argUseEncryption)) settings.useEncryption = Boolean.parseBoolean(arg.substring(argUseEncryption.length())); else
        if (arg.startsWith(argPassword)) settings.password = arg.substring(argPassword.length()).getBytes("UTF-8"); else
        if (arg.startsWith(argPasswordHex)) settings.password = Utils.fromHex(arg.substring(argPasswordHex.length())); else
        if (arg.startsWith(argAlgo)) settings.cryptAlgorithm = arg.substring(argAlgo.length()); else
        if (arg.startsWith(argInc)) settings.addToInclude(arg.substring(argInc.length())); else
        if (arg.startsWith(argExc)) settings.addToExclude(arg.substring(argExc.length())); else
        if (arg.startsWith(argCRC)) settings.calcCRC = Boolean.parseBoolean(arg.substring(argCRC.length())); else
        if (arg.equals(argToClip)) settings.copyResultToClipboard = true; else
        Utils.out.println("Unknown argument: "+arg);
    }

    public void processArgs(String[] args, int startIndex) throws Exception {
        
        for (int i = startIndex; i < args.length; i++) processArg(args[i]);
    }

    public void setCommand(Command command) {
        this.command = command;
    }

    public void setSourcePath(String sourcePath) {
        settings.sourcePath = sourcePath;
    }

    public void setDestinationPath(String destinationPath) {
        settings.destinationPath = destinationPath;
    }

    public void setSourceString(String sourceString) {
        settings.sourceString = sourceString;
    }

    public void setPassword(String password) throws Exception {
        settings.password = password.getBytes("UTF-8");
    }

    public Settings getSettings() {
        return settings;
    }

    @Override
    public void starting() {
    }
    
    @Override
    public void completed() {
        
    }
    
    private static void printInfo() {
        
        Utils.out.println("Usage: crypt command <params> [options]");
        Utils.out.println("Commands:");
        
        Utils.out.println( 5, "encrypt - compress & encrypt entire content of specified file_or_directory and writes result file");
        Utils.out.println(10, "Params: <source_file_or_directory> <destination_file>");
        Utils.out.println(10, "Available options:");
        Utils.out.println(15, "inc=regexp - files or directories to include if matching");
        Utils.out.println(15, "exc=regexp - files or directories to exclude if matching");
        Utils.out.println(15, "compress=level - compress content where level 0 - no compression, 9 - max compression");
        Utils.out.println(15, "encrypt=true/false - encrypt entire content (default: "+Settings.DEFAULT_USE_ENCRYPTION+")");
        Utils.out.println(15, "algo=algorithm - specify encryption algorithm (default: "+Settings.DEFAULT_CRYPT_ALGORITHM+")");
        Utils.out.println(15, "crc=true/false - calculate file CRC (default: "+Settings.DEFAULT_CALCULATE_CRC+")");
        Utils.out.println(15, "password=secretphrase - specify password (will be prompted in command line if not provided)");
        Utils.out.println(15, "passwordhex=specify password as hex string");
        
        Utils.out.println( 5, "decrypt - decrypt & decompress part or entire encrypted file to specified destination path");
        Utils.out.println(10, "Params: <encrypted_file> <destination_path>");
        Utils.out.println(10, "Available options:");
        Utils.out.println(15, "inc=regexp - files or directories to include if matching");
        Utils.out.println(15, "exc=regexp - files or directories to exclude if matching");
        Utils.out.println(15, "encrypt=true/false - decrypt entire content (default: "+Settings.DEFAULT_USE_ENCRYPTION+")");
        Utils.out.println(15, "algo=algorithm - specify decryption algorithm (default: "+Settings.DEFAULT_CRYPT_ALGORITHM+")");
        Utils.out.println(15, "password=secretphrase - specify password (will be prompted in command line if not provided)");
        
        Utils.out.println( 5, "check - decrypt & decompress entire encrypted file but don't write any result to disk");
        Utils.out.println(10, "Params: <encrypted_file>");
        Utils.out.println(10, "Available options:");
        Utils.out.println(15, "encrypt=true/false - decrypt entire content (default: "+Settings.DEFAULT_USE_ENCRYPTION+")");
        Utils.out.println(15, "algo=algorithm - specify decryption algorithm (default: "+Settings.DEFAULT_CRYPT_ALGORITHM+")");
        Utils.out.println(15, "password=secretphrase - specify password (will be prompted in command line if not provided)");
        
        Utils.out.println( 5, "list - print some encryption algorithms available");
        
        Utils.out.println( 5, "encrypt_string - encrypt string and print output");
        Utils.out.println(10, "Params: <source_string> = \"fromclip\" for source string being taken from clipboard");
        Utils.out.println(10, "Available options:");
        Utils.out.println(15, "algo=algorithm - specify encryption algorithm (default: "+Settings.DEFAULT_CRYPT_ALGORITHM+")");
        Utils.out.println(15, "password=secretphrase - specify password (will be prompted in command line if not provided)");
        Utils.out.println(15, "toclip - copy result string to clipboard");
        
        Utils.out.println( 5, "decrypt_string - decrypt string and print output");
        Utils.out.println(10, "Params: <source_string> = \"fromclip\" for source string being taken from clipboard");
        Utils.out.println(10, "Available options:");
        Utils.out.println(15, "algo=algorithm - specify decryption algorithm (default: "+Settings.DEFAULT_CRYPT_ALGORITHM+")");
        Utils.out.println(15, "password=secretphrase - specify password (will be prompted in command line if not provided)");
        Utils.out.println(15, "toclip - copy result string to clipboard");
        
        Utils.out.println( 5, "encrypt_file_string - compress & encrypt file content and print output as string");
        Utils.out.println(10, "Params: <source_file>");
        Utils.out.println(10, "Available options:");
        Utils.out.println(15, "algo=algorithm - specify encryption algorithm (default: "+Settings.DEFAULT_CRYPT_ALGORITHM+")");
        Utils.out.println(15, "compress=1 - compress content (default: "+true+")");
        Utils.out.println(15, "password=secretphrase - specify password (will be prompted in command line if not provided)");
        Utils.out.println(15, "toclip - copy result string to clipboard");
        
        Utils.out.println( 5, "decrypt_string_file - decrypt & decompress string and write output file");
        Utils.out.println(10, "Params: <source_string> <destination_file>");
        Utils.out.println(10, "<source_string> = \"fromclip\" for source string being taken from clipboard");
        Utils.out.println(10, "Available options:");
        Utils.out.println(15, "algo=algorithm - specify decryption algorithm (default: "+Settings.DEFAULT_CRYPT_ALGORITHM+")");
        Utils.out.println(15, "compress=1 - decompress content (default: "+true+")");
        Utils.out.println(15, "password=secretphrase - specify password (will be prompted in command line if not provided)");

        Utils.out.println( 5, "to_hex - convert string to hex");
        Utils.out.println(10, "Params: <source_string>");

        Utils.out.println( 5, "from_hex - convert hex to string");
        Utils.out.println(10, "Params: <source_string>");

        Utils.out.println( 5, "self_test - process self test on java encryption algorithms");
        
        Utils.out.println();
        Utils.out.println();
        Utils.out.println("Beware of changing default encryption algorithm as encrypted file doesn't store information about");
        Utils.out.println("algorithm it was encrypted with.");
    }

    public static List<String> getProviders() {
        
        List<String> items = new ArrayList<>(20);
        for (Provider provider : Security.getProviders()) {
            provider.getServices().stream()
                    .filter(s -> "Cipher".equals(s.getType()))
                    .map(Service::getAlgorithm)
                    .forEach(items::add);
        }
        Collections.sort(items);
        return items;
    }
    
    private static void printProviders() {
        
        for (String provider: getProviders()) Utils.out.println(provider);
    }
    
    public static void main(String[] args) {
        
        try {
            
            if (args == null || args.length == 0) {
                
                printInfo();
                return;
            }
            
            Crypt tool = new Crypt();

            switch (args[0].toLowerCase()) {
                
                case "encrypt"  : 
                    tool.setCommand(Command.ENCRYPT); 
                    if (!Utils.checkArgs(args, 3)) return;
                    tool.setSourcePath(args[1]);
                    tool.setDestinationPath(args[2]);
                    tool.processArgs(args, 3);
                    break;
                case "encrypt_string"  : 
                    tool.setCommand(Command.ENCRYPT_STRING); 
                    if (!Utils.checkArgs(args, 2)) return;
                    String source = args[1];
                    if (source.equalsIgnoreCase("fromclip")) source = Utils.receiveStringFromClipboard();
                    tool.setSourceString(source);
                    tool.processArgs(args, 2);
                    break;
                case "encrypt_file_string"  : 
                    tool.setCommand(Command.ENCRYPT_FILE_STRING); 
                    if (!Utils.checkArgs(args, 2)) return;
                    tool.setSourcePath(args[1]);
                    tool.processArgs(args, 2);
                    break;
                case "decrypt"  :
                    tool.setCommand(Command.DECRYPT); 
                    if (!Utils.checkArgs(args, 3)) return;
                    tool.setSourcePath(args[1]);
                    tool.setDestinationPath(args[2]);
                    tool.processArgs(args, 3);
                    break;
                case "decrypt_string"  : 
                    tool.setCommand(Command.DECRYPT_STRING); 
                    if (!Utils.checkArgs(args, 2)) return;
                    source = args[1];
                    if (source.equalsIgnoreCase("fromclip")) source = Utils.receiveStringFromClipboard();
                    tool.setSourceString(source);
                    tool.processArgs(args, 2);
                    break;
                case "decrypt_string_file"  : 
                    tool.setCommand(Command.DECRYPT_STRING_FILE); 
                    if (!Utils.checkArgs(args, 3)) return;
                    source = args[1];
                    if (source.equalsIgnoreCase("fromclip")) source = Utils.receiveStringFromClipboard();
                    tool.setSourceString(source);
                    tool.setDestinationPath(args[2]);
                    tool.processArgs(args, 3);
                    break;
                case "check"    : 
                    tool.setCommand(Command.CHECK); 
                    if (!Utils.checkArgs(args, 2)) return;
                    tool.setSourcePath(args[1]);
                    tool.processArgs(args, 2);
                    break;
                case "list"     : 
                    printProviders(); 
                    return;
                case "to_hex"  : 
                    tool.setCommand(Command.TO_HEX); 
                    if (!Utils.checkArgs(args, 2)) return;
                    tool.setSourceString(args[1]);
                    tool.settings.useEncryption = false;
                    break;
                case "from_hex"  : 
                    tool.setCommand(Command.FROM_HEX); 
                    if (!Utils.checkArgs(args, 2)) return;
                    tool.setSourceString(args[1]);
                    tool.settings.useEncryption = false;
                    break;
                case "self_test"  : 
                    tool.setCommand(Command.SELF_TEST); 
                    tool.settings.useEncryption = false;
                    break;
                case "ver"      : 
                case "version"  : 
                    Utils.out.println(Const.TOOLS_VERSION_FULL); 
                    return;
                default         : 
                    Utils.out.println("Unknown command: "+args[0]); 
                    return;
            }
            
            tool.run();
            
        } catch (Exception ex) { ex.printStackTrace(); }
        
    }
    
}