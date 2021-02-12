package ddx.fill;

import ddx.common.Const;
import ddx.common.Progress;
import ddx.common.Utils;
import ddx.performance.Xoshiro256p;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 *
 * @author druidman
 */
public class FillFile implements Progress {
    
    public static enum Command {FILL, CHECK};

    private Command command;
    private final Settings settings = new Settings();
    private MessageDigest md;
    
    @Override
    public void starting() {
    }
    
    @Override
    public void completed() {
    }

    private boolean init() throws Exception {
        
        if (command.equals(Command.FILL) && settings.fileSize < 1) {
            
            Utils.out.println("Invalid file size!");
            return false;
        }
        
        if (settings.bufferSize < 1 || settings.bufferSize > Const.GB) {
            
            Utils.out.println("Invalid buffer size!");
            return false;
        }
        
        return true;
    }

    private boolean initMessageDigest() {

        try {
            
            md = MessageDigest.getInstance(settings.algorithm);
        } catch (NoSuchAlgorithmException ex) {
            
            Utils.out.println("Hash algorithm ["+settings.algorithm+"] is not found!");
            return false;
        }
        
        return true;
    }

    private byte[] createRandomBuffer(int size, long[] xoshiroState) throws Exception {

        byte[] bf = new byte[size];

        int pos = 0;

        while (pos < size) {

            long value = Xoshiro256p.getNextLong(xoshiroState);

            for (int i = 0; i < 8; i++) {

                bf[pos++] = (byte)(value & 0xFF);
                value >>= 8;
            }
        }
        
        return bf;
    }
    
    private boolean processFill() throws Exception {

        if (!initMessageDigest()) return false;
        
        File f = new File(settings.fileName);
        
        if (f.exists() && !f.delete()) {

            Utils.out.print("File ["+settings.fileName+"] exists can't be deleted!");
            return false;
        }
        
        long randomSeed = settings.randomSeed==null?System.currentTimeMillis():(long)settings.randomSeed;
        
        Utils.out.println("Buffer ["+Utils.describeFileLength(settings.bufferSize)+"]");
        Utils.out.println("Hash algorithm ["+settings.algorithm+"]");
        Utils.out.println("Random seed ["+randomSeed+"]");
        Utils.out.print("Writing file ["+f.getAbsolutePath()+"] size ["+Utils.describeFileLength(settings.fileSize)+"] ");

        long start = System.currentTimeMillis();
        
        starting();

        FileOutputStream fos = new FileOutputStream(f);
        fos.write(Utils.longToBytes(settings.fileSize));
        fos.write(settings.algorithm.length());
        fos.write(settings.algorithm.getBytes());
        
        int written = 0;
        long left = settings.fileSize;
        long[] xoshiroState = new long[4];
        Random random = new Random(randomSeed);
        for (int i = 0; i < xoshiroState.length; i++) xoshiroState[i] = random.nextLong();
        
        while (left > 0) {
            
            int bflen = settings.bufferSize;
            if (bflen > left) bflen = (int)left;
            byte[] bf = createRandomBuffer(bflen, xoshiroState);
        
            fos.write(bf);
            md.update(bf);
            
            written += bflen;
            left -= bflen;
            settings.filesSizeProcessed += bflen;
            
            if (written >= Const.PRINT_STR_AT_EVERY_BYTES) {

                Utils.out.print(Const.PRINT_STR);
                written -= Const.PRINT_STR_AT_EVERY_BYTES;
            }
        }
        
        byte[] hash = md.digest();
        
        fos.write(hash);
        fos.flush();
        fos.close();
        
        Utils.out.println(" done");

        completed();

        long end = System.currentTimeMillis();
        long time = (end - start) / 1_000;
        
        if (time > 0) {
            
            long bytesPerSecong = settings.filesSizeProcessed / time;
            Utils.out.println("Processing speed: "+Utils.describeFileLength(bytesPerSecong)+" per second.");
            Utils.out.println("Time consumed: "+Utils.describeTimeTotal(time, false));
        }

        if (settings.showHash)
            Utils.out.println("Content hash ["+Utils.printHexBinary(hash)+"]");
        
        return true;
    }
    
    private boolean processCheck() throws Exception {

        File f = new File(settings.fileName);
        
        if (!f.exists()) {

            Utils.out.print("File ["+settings.fileName+"] is not exists!");
            return false;
        }

        if (!f.isFile()) {

            Utils.out.print("File ["+settings.fileName+"] is not file!");
            return false;
        }
        
        Utils.out.println("Checking file ["+f.getAbsolutePath()+"]");
        
        long start = System.currentTimeMillis();
        
        starting();

        FileInputStream fis = new FileInputStream(f);
        byte[] tmp = new byte[8];
        if (fis.read(tmp) != tmp.length) {
            
            Utils.out.println("Can't read header!");
            fis.close();
            return false;
        }
        
        settings.fileSize = Utils.bytesToLong(tmp);
        
        Utils.out.println("Content size found: " + Utils.describeFileLength(settings.fileSize));
        
        int t = fis.read();
        tmp = new byte[t];
        if (fis.read(tmp) != tmp.length) {
            
            Utils.out.println("Can't read header!");
            fis.close();
            return false;
        }
        
        settings.algorithm = new String(tmp);

        Utils.out.println("Hash algorithm found: " + settings.algorithm);
        
        if (!initMessageDigest()) return false;

        Utils.out.println("Buffer ["+Utils.describeFileLength(settings.bufferSize)+"]");
        Utils.out.print("Checking content ");
        
        int read = 0;
        long left = settings.fileSize;
        
        byte[] bf = new byte[settings.bufferSize];
        
        while (left > 0) {
            
            int bflen = settings.bufferSize;
            if (bflen > left) bflen = (int)left;
            
            int bfRead = fis.read(bf, 0, bflen);

            md.update(bf, 0, bfRead);
            
            read += bfRead;
            left -= bfRead;
            settings.filesSizeProcessed += bfRead;
            
            if (read >= Const.PRINT_STR_AT_EVERY_BYTES) {

                Utils.out.print(Const.PRINT_STR);
                read -= Const.PRINT_STR_AT_EVERY_BYTES;
            }
        }
        
        byte[] hash = md.digest();
        byte[] hashOri = new byte[hash.length];
        
        if (fis.read(hashOri) != hash.length) {
            
            Utils.out.println("Can't read original hash!");
            fis.close();
            return false;
        };
        
        fis.close();
        
        Utils.out.println(" done");

        completed();

        long end = System.currentTimeMillis();
        long time = (end - start) / 1_000;
        
        if (time > 0) {
            
            long bytesPerSecong = settings.filesSizeProcessed / time;
            Utils.out.println("Processing speed: "+Utils.describeFileLength(bytesPerSecong)+" per second.");
            Utils.out.println("Time consumed: "+Utils.describeTimeTotal(time, false));
        }
        
        boolean equals = Arrays.equals(hash, hashOri);
        
        Utils.out.println("File content ["+settings.fileName+"] "+(equals?"IS GOOD":"IS DAMAGED"));
        
        if (settings.showHash) {
            
            if (equals) {
                
                Utils.out.println("Content hash ["+Utils.printHexBinary(hash)+"]");
            } else {
                
                Utils.out.println("Content hash calculated ["+Utils.printHexBinary(hash)+"]");
                Utils.out.println("Content hash stored     ["+Utils.printHexBinary(hashOri)+"]");
            }
        }
        
        return true;
    }
    
    public boolean run() throws Exception {
        
        if (!init()) return false;
        
        switch (command) {
            
            case FILL               : return processFill();
            case CHECK              : return processCheck();
            default : return false;
        }
    }
    
    public void processArg(String arg) throws Exception {
        
        String argBuffer = "buffer=";
        String argAlgo = "algo=";
        String argSeed = "seed=";
        String argShowHash = "showhash=";
        if (arg.startsWith(argBuffer)) settings.bufferSize = (int)Utils.parseFileLength(arg.substring(argBuffer.length())); else
        if (arg.startsWith(argAlgo)) settings.algorithm = arg.substring(argAlgo.length()); else
        if (arg.startsWith(argSeed)) settings.randomSeed = Long.parseLong(arg.substring(argSeed.length())); else
        if (arg.startsWith(argShowHash)) settings.showHash = Boolean.parseBoolean(arg.substring(argShowHash.length())); else
        Utils.out.println("Unknown argument: "+arg);
    }

    public void processArgs(String[] args, int startIndex) throws Exception {
        
        for (int i = startIndex; i < args.length; i++) processArg(args[i]);
    }

    private static void printInfo() {
        
        Utils.out.println("Usage: fill command <params> [options]");
        Utils.out.println("Commands:");
        Utils.out.println( 5, "fill - write file with random generated bytes (adding hash to the end of file)");
        Utils.out.println(10, "Params: <filename> <filesize>");
        Utils.out.println(10, "Available options:");
        Utils.out.println(15, "buffer=size - size of buffer when writing file (default: "+Settings.DEFAULT_BUFFER_SIZE+")");
        Utils.out.println(15, "algo=algorithm - specify hash algorithm (default: "+Settings.DEFAULT_ALGORITHM+")");
        Utils.out.println(15, "seed=long - initial seed for random generator (default: system.milliseconds)");
        Utils.out.println(15, "showhash=true/false - show content hash (default: "+Settings.DEFAULT_SHOW_HASH+")");
        Utils.out.println( 5, "check - read content of written file and check its hash");
        Utils.out.println(10, "Params: <filename>");
        Utils.out.println(10, "Available options:");
        Utils.out.println(15, "buffer=size - size of buffer when reading file (default: "+Settings.DEFAULT_BUFFER_SIZE+")");
        Utils.out.println(15, "showhash=true/false - show content hash (default: "+Settings.DEFAULT_SHOW_HASH+")");
        Utils.out.println( 5, "list - print all available hash algorithms");
    }

    public static List<String> getAlgorithms() {
        
        List<String> items = new ArrayList<>(20);
        items.addAll(Security.getAlgorithms("MessageDigest"));
        Collections.sort(items);
        return items;
    }
    
    private static void printAlgorithms() {
        
        for (String item : getAlgorithms()) Utils.out.println(item + (Settings.DEFAULT_ALGORITHM.equalsIgnoreCase(item)?" (default)":""));
    }
    
    public void setCommand(Command command) {
        this.command = command;
    }

    public void setFileName(String fileName) {
        settings.fileName = fileName;
    }

    public void setFileSize(long fileSize) {
        settings.fileSize = fileSize;
    }
    
    public static void main(String[] args) {
        
        try {

            if (args == null || args.length == 0) {
                
                printInfo();
                return;
            }

            FillFile tool = new FillFile();
            
            switch (args[0].toLowerCase()) {
                
                case "fill"  : 
                    tool.setCommand(Command.FILL); 
                    if (!Utils.checkArgs(args, 3)) return;
                    tool.setFileName(args[1]);
                    tool.setFileSize(Utils.parseFileLength(args[2]));
                    tool.processArgs(args, 3);
                    break;
                case "check"  : 
                    tool.setCommand(Command.CHECK); 
                    if (!Utils.checkArgs(args, 2)) return;
                    tool.setFileName(args[1]);
                    tool.processArgs(args, 2);
                    break;
                case "list"     : 
                    printAlgorithms(); 
                    return;
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
