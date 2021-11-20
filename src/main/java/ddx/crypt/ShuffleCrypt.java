package ddx.crypt;

import ddx.common.Const;
import ddx.common.PrinterConsole;
import ddx.common.SizeConv;
import ddx.common.Str;
import ddx.common.Utils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import javax.crypto.Cipher;

/**
 *
 * @author druidman
 */
public class ShuffleCrypt extends Crypt {
    
    public static enum Command {ENCRYPT, DECRYPT, CHECK};
    
    private static final String[] ALGOS = {
        "aes/ecb/nopadding",
        "aes/cbc/nopadding",
        "aes/cfb/nopadding",
        "aes/ofb/nopadding",
        "aes_192/ecb/nopadding",
        "aes_192/cbc/nopadding",
        "aes_192/cfb/nopadding",
        "aes_192/ofb/nopadding",
        "aes_256/ecb/nopadding",
        "aes_256/cbc/nopadding",
        "aes_256/cfb/nopadding",
        "aes_256/ofb/nopadding",
        "des/ecb/nopadding",
        "des/cbc/nopadding",
        "des/cfb/nopadding",
        "des/ofb/nopadding",
        "desede/ecb/nopadding",
        "desede/cbc/nopadding",
        "desede/cfb/nopadding",
        "desede/ofb/nopadding",
        "blowfish/ecb/nopadding",
        "blowfish/cbc/nopadding",
        "blowfish/cfb/nopadding",
        "blowfish/ofb/nopadding",
    };
    
    private Command command;
    private Random rnd;
    private long randomSeed = System.currentTimeMillis();
    private int cycleCount = 1;
    private String[] usedAlgo = ALGOS;
    private String algoFilter;
    private boolean deleteInput = false;
    
    private boolean initAlgos() throws Exception {
        
        if (!Str.isEmpty(algoFilter)) {
            
            List<String> suit = new LinkedList<>();
            for (String algo : ALGOS) if (algo.contains(algoFilter)) suit.add(algo);
            if (!suit.isEmpty()) usedAlgo = suit.toArray(new String[0]);
        }
        
        return usedAlgo.length > 0;
    }
    
    private boolean init() throws Exception {
    
        rnd = new Random(randomSeed);
        
        if (!initAlgos()) {
            
            Utils.out.println("Can't init algorithms!");
            return false;
        }
        
        return true;
    }
    
    private void getNextAlgorithmAndPassword() {
        
        settings.cryptAlgorithm = usedAlgo[rnd.nextInt(usedAlgo.length)];
        int passwordLen = 1 + rnd.nextInt(32);
        settings.password = new byte[passwordLen];
        rnd.nextBytes(settings.password);
    }
    
    private Cipher initKeysGetCipher(int mode) throws Exception {
        
        ((PrinterConsole)Utils.out).setPrintAllowed(false);
        initKeys();
        Cipher cipher = getCipher(mode);
        ((PrinterConsole)Utils.out).setPrintAllowed(true);
        return cipher;
    }
    
    private boolean processEncrypt() throws Exception {

        if (Str.isEmpty(settings.sourcePath)) {
            
            Utils.out.println("Wrong source file!");
            return false;
        }
        
        if (settings.sourcePath.equalsIgnoreCase("x") && settings.destinationPath.equalsIgnoreCase("x")) {
            
            List<File> toProcess = new LinkedList<>();
            
            File f = new File(".");
            for (File file : f.listFiles()) {
                
                if (file.exists() && !file.isDirectory() && !file.getName().toLowerCase().endsWith(".sc")) toProcess.add(file);
            }
            
            if (toProcess.isEmpty()) {
                
                Utils.out.println("No files to process!");
                return false;
            }
            
            List<File> toProcessSorted = new ArrayList<>(toProcess.size());
            for (File file : toProcess) toProcessSorted.add(file);
            Collections.sort(toProcessSorted);
            
            boolean reInit = false;
            for (File file : toProcessSorted) {
                
                if (reInit) init();
                String fileName = file.getName();
                if (!processEncrypt(fileName, fileName+".sc")) return false;
                reInit = true;
            }
            
            return true;
            
        } else {
            
            return processEncrypt(settings.sourcePath, settings.destinationPath);
        }
    }
    
    private boolean processEncrypt(String sourcePath, String destinationPath) throws Exception {
        
        File file = new File(sourcePath);
        
        if (!file.exists() || file.isDirectory()) {
            
            Utils.out.println("Wrong source file!");
            return false;
        }
        
        long len = file.length();
        if (len >= SizeConv.GB * 2) {
            
            Utils.out.println("File is too long!");
            return false;
        }
        
        int leni = (int)len;
        int bflen = leni + 4 + 32; // file size + file sha-256 hash + raw data + padding
        while (bflen % 64 != 0) bflen++; // do for nopadding encryption aes-256 max
        
        byte[] bf = new byte[bflen];
        byte[] lenbf = Utils.intToBytes(leni);
        System.arraycopy(lenbf, 0, bf, 0, lenbf.length);
        
        Utils.out.println("Reading file ["+file.getAbsolutePath()+"]");
        Utils.out.println("File size ["+SizeConv.sizeToStrSimple(len)+"]");
        
        FileInputStream fis = new FileInputStream(file);
        int readBytes = fis.read(bf, 4 + 32, leni);
        fis.close();
        if (readBytes != leni) {
            
            Utils.out.println("file read error!");
            return false;
        }
        
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(bf, 4 + 32, leni);
        byte[] hash = md.digest();
        System.arraycopy(hash, 0, bf, 4, hash.length);
        
        Utils.out.println("Sha256 file hash ["+Utils.toHex(hash)+"]");
        
        Utils.out.println("Starting encryption (algos: "+usedAlgo.length+" seed: "+randomSeed+" count: "+cycleCount+" ...");
        
        for (int i = 0; i < cycleCount; i++) {
            
            getNextAlgorithmAndPassword();
            
            Utils.out.print("Cycle ["+String.valueOf(i+1)+"] (algo: "+settings.cryptAlgorithm+" password: "+Utils.toHex(settings.password)+") ...");
            
            bf = initKeysGetCipher(Cipher.ENCRYPT_MODE).doFinal(bf);
            
            if (rnd.nextBoolean()) {
                
                Utils.out.print(" inversing buffer ...");
                Utils.inverseBuffer(bf);
            }
            
            Utils.out.println(" done");
        }
        
        File fileOut = new File(destinationPath);

        Utils.out.println("Writing output file ["+fileOut.getAbsolutePath()+"]");
        
        if (fileOut.exists() && !fileOut.delete()) {
            
            Utils.out.println("Wrong output file!");
            return false;
        }
        
        FileOutputStream fos = new FileOutputStream(fileOut);
        fos.write(bf);
        fos.close();
        
        if (deleteInput) {
            
            boolean success = file.delete();
            Utils.out.println("Deleting input file ... "+(success?"ok":"error"));
        }
        
        return true;
    }
    
    private boolean processDecrypt() throws Exception {
        
        File file = new File(settings.sourcePath);
        
        if (!file.exists() || file.isDirectory()) {
            
            Utils.out.println("Wrong file!");
            return false;
        }

        long len = file.length();
        
        Utils.out.println("Reading file ["+file.getAbsolutePath()+"]");
        Utils.out.println("File size ["+SizeConv.sizeToStrSimple(len)+"]");
        
        int leni = (int)len;
        byte[] bf = new byte[leni];
        
        FileInputStream fis = new FileInputStream(file);
        int readBytes = fis.read(bf);
        fis.close();
        if (readBytes != leni) {
            
            Utils.out.println("file read error!");
            return false;
        }
        
        String[] algos = new String[cycleCount];
        String[] passwords = new String[cycleCount];
        boolean[] inverse = new boolean[cycleCount];
        
        for (int i = 0; i < cycleCount; i++) {
            
            getNextAlgorithmAndPassword();
            
            algos[i] = settings.cryptAlgorithm;
            passwords[i] = Utils.toHex(settings.password);
            inverse[i] = rnd.nextBoolean();
        }
        
        Utils.out.println("Starting decryption (algos: "+usedAlgo.length+" seed: "+randomSeed+" count: "+cycleCount+" ...");
        
        for (int i = cycleCount - 1; i >= 0; i--) {
            
            settings.cryptAlgorithm = algos[i];
            settings.password = Utils.fromHex(passwords[i]);
            
            Utils.out.print("Cycle ["+String.valueOf(i+1)+"] (algo: "+settings.cryptAlgorithm+" password: "+Utils.toHex(settings.password)+") ...");
            
            if (inverse[i]) {
                
                Utils.out.print(" inversing buffer ...");
                Utils.inverseBuffer(bf);
            }

            bf = initKeysGetCipher(Cipher.DECRYPT_MODE).doFinal(bf);
            
            Utils.out.println(" done");
        }
        
        byte[] lenbf = new byte[4];
        System.arraycopy(bf, 0, lenbf, 0, lenbf.length);
        int orilen = Utils.bytesToInt(lenbf);
        
        if (orilen <= 0 || orilen >= leni) {
            
            Utils.out.println("Restored length ["+orilen+"] is wrong!");
            return false;
        }
        
        byte[] hash = new byte[32];
        System.arraycopy(bf, 4, hash, 0, hash.length);
        
        Utils.out.println("Length.header restored ["+SizeConv.sizeToStrSimple(orilen)+"]");
        Utils.out.println("Hash.header restored ["+Utils.toHex(hash)+"]");

        Utils.out.println("Checking hash ...");
        
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(bf, 4 + 32, orilen);
        byte[] hashc = md.digest();
        Utils.out.println("Hash calculated ["+Utils.toHex(hashc)+"]");

        boolean eq = Arrays.equals(hash, hashc);
        
        if (eq) 
            Utils.out.println("EVERYTHING OK, HASHES ARE EQUAL!"); else
            Utils.out.println("WRONG HASHES, FILE DAMAGED!");

        File fileOut = new File(settings.destinationPath);

        Utils.out.println("Writing output file ["+fileOut.getAbsolutePath()+"]");
        
        if (fileOut.exists() && !fileOut.delete()) {
            
            Utils.out.println("Wrong output file!");
            return false;
        }
        
        if (fileOut.getParent() != null && !(Files.exists(Paths.get(fileOut.getParent())))) Files.createDirectories(Paths.get(fileOut.getParent()));
        
        FileOutputStream fos = new FileOutputStream(fileOut);
        fos.write(bf, 4 + 32, orilen);
        fos.close();
        
        if (deleteInput) {
            
            boolean success = file.delete();
            Utils.out.println("Deleting input file ... "+(success?"ok":"error"));
        }
        
        return true;
    }
    
    private boolean processCheck() throws Exception {
        
        File file = new File(settings.sourcePath);
        
        if (!file.exists() || file.isDirectory()) {
            
            Utils.out.println("Wrong file!");
            return false;
        }
        
        long len = file.length();
        
        Utils.out.println("Reading file ["+file.getAbsolutePath()+"]");
        Utils.out.println("File size ["+SizeConv.sizeToStrSimple(len)+"]");
        
        int leni = (int)len;
        byte[] bf = new byte[leni];
        
        FileInputStream fis = new FileInputStream(file);
        int readBytes = fis.read(bf);
        fis.close();
        if (readBytes != leni) {
            
            Utils.out.println("file read error!");
            return false;
        }
        
        String[] algos = new String[cycleCount];
        String[] passwords = new String[cycleCount];
        boolean[] inverse = new boolean[cycleCount];
        
        for (int i = 0; i < cycleCount; i++) {
            
            getNextAlgorithmAndPassword();
            
            algos[i] = settings.cryptAlgorithm;
            passwords[i] = Utils.toHex(settings.password);
            inverse[i] = rnd.nextBoolean();
        }
        
        Utils.out.println("Starting decryption (algos: "+usedAlgo.length+" seed: "+randomSeed+" count: "+cycleCount+" ...");
        
        for (int i = cycleCount - 1; i >= 0; i--) {
            
            settings.cryptAlgorithm = algos[i];
            settings.password = Utils.fromHex(passwords[i]);
            
            Utils.out.print("Cycle ["+String.valueOf(i+1)+"] (algo: "+settings.cryptAlgorithm+" password: "+Utils.toHex(settings.password)+") ...");
            
            if (inverse[i]) {
                
                Utils.out.print(" inversing buffer ...");
                Utils.inverseBuffer(bf);
            }

            bf = initKeysGetCipher(Cipher.DECRYPT_MODE).doFinal(bf);
            
            Utils.out.println(" done");
        }
        
        byte[] lenbf = new byte[4];
        System.arraycopy(bf, 0, lenbf, 0, lenbf.length);
        int orilen = Utils.bytesToInt(lenbf);
        
        if (orilen <= 0 || orilen >= leni) {
            
            Utils.out.println("Restored length ["+orilen+"] is wrong!");
            return false;
        }
        
        byte[] hash = new byte[32];
        System.arraycopy(bf, 4, hash, 0, hash.length);
        
        Utils.out.println("Length.header restored ["+SizeConv.sizeToStrSimple(orilen)+"]");
        Utils.out.println("Hash.header restored ["+Utils.toHex(hash)+"]");

        Utils.out.println("Checking hash ...");
        
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(bf, 4 + 32, orilen);
        byte[] hashc = md.digest();
        Utils.out.println("Hash calculated ["+Utils.toHex(hashc)+"]");

        boolean eq = Arrays.equals(hash, hashc);
        
        if (eq) 
            Utils.out.println("EVERYTHING OK, HASHES ARE EQUAL!"); else
            Utils.out.println("WRONG HASHES, FILE DAMAGED!");

        return true;
    }
    
    public boolean run() throws Exception {
        
        if (!init()) return false;
        
        switch (command) {
            
            case ENCRYPT                : return processEncrypt();
            case DECRYPT                : return processDecrypt();
            case CHECK                  : return processCheck();
            default                     : return false;
        }
    }
    
    @Override
    public void processArg(String arg) throws Exception {
        
        String argSeed = "seed=";
        String argCount = "count=";
        String argAlgo = "algo=";
        String argDelete = "delete";
        if (arg.startsWith(argSeed)) randomSeed = Long.parseLong(arg.substring(argSeed.length())); else
        if (arg.startsWith(argCount)) cycleCount = Integer.parseInt(arg.substring(argCount.length())); else
        if (arg.startsWith(argAlgo)) algoFilter = arg.substring(argAlgo.length()).toLowerCase(); else
        if (arg.startsWith(argDelete)) deleteInput = true; else
        Utils.out.println("Unknown argument: "+arg);
    }

    public void setCommand(Command command) {
        this.command = command;
    }

    public void setSeed(long value) {
        randomSeed = value;
    }

    public void printInfo() {
        
        Utils.out.println("Usage: shuffle_crypt command <params> [options]");
        Utils.out.println("Commands:");
        
        Utils.out.println( 5, "encrypt - encrypt file in memory multiple times with random algorithms and passwords");
        Utils.out.println(10, "Params: <source_file> <destination_file>");
        Utils.out.println(10, "* use \"x x\" as <source_file> <destination_file> to process all files in current folder");
        Utils.out.println(10, "Available options:");
        Utils.out.println(15, "seed=long - random seed for passwords generation (default: system time in ms)");
        Utils.out.println(15, "count=int - encryption cycle count (default: "+cycleCount+")");
        Utils.out.println(15, "algo=filter - use only algorithms which passes filter");
        Utils.out.println(15, "delete - delete input file");

        Utils.out.println( 5, "decrypt - decrypt file");
        Utils.out.println(10, "Params: <source_file> <destination_file> <seed>");
        Utils.out.println(10, "Available options:");
        Utils.out.println(15, "count=int - decryption cycle count (default: "+cycleCount+")");
        Utils.out.println(15, "algo=filter - use only algorithms which passes filter");

        Utils.out.println( 5, "check - check encrypted file by decrypting it in memory");
        Utils.out.println(10, "Params: <source_file> <seed>");
        Utils.out.println(10, "Available options:");
        Utils.out.println(15, "count=int - decryption cycle count (default: "+cycleCount+")");
        Utils.out.println(15, "algo=filter - use only algorithms which passes filter");
    }

    public static void main(String[] args) {
        
        try {
            
            if (args == null || args.length == 0) {
                
                new ShuffleCrypt().printInfo();
                return;
            }
            
            ShuffleCrypt tool = new ShuffleCrypt();

            switch (args[0].toLowerCase()) {
                
                case "encrypt"  : 
                    tool.setCommand(Command.ENCRYPT); 
                    if (!Utils.checkArgs(args, 3)) return;
                    tool.setSourcePath(args[1]);
                    tool.setDestinationPath(args[2]);
                    tool.processArgs(args, 3);
                    break;
                case "decrypt"  :
                    tool.setCommand(Command.DECRYPT); 
                    if (!Utils.checkArgs(args, 4)) return;
                    tool.setSourcePath(args[1]);
                    tool.setDestinationPath(args[2]);
                    tool.setSeed(Long.parseLong(args[3]));
                    tool.processArgs(args, 4);
                    break;
                case "check"    : 
                    tool.setCommand(Command.CHECK); 
                    if (!Utils.checkArgs(args, 3)) return;
                    tool.setSourcePath(args[1]);
                    tool.setSeed(Long.parseLong(args[2]));
                    tool.processArgs(args, 3);
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