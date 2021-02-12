package ddx.hash;

import ddx.common.Const;
import ddx.common.Progress;
import ddx.common.Utils;
import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 *
 * @author druidman
 */
public class Hash implements Progress {
    
    public static enum Command {HASH, HASH_STRING, CHECK, PRINT};
    
    private MessageDigest md;
    
    private Command command;
    private final Settings settings = new Settings();

    private boolean init() {
    
        return initMessageDigest();
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

    private void hashString(String source) throws Exception {
        
        md.update(source.getBytes("UTF-8"));
    }

    private void hashFileName(File file) throws Exception {
        
        hashString(file.getName());
    }

    private boolean hashFileContent(File file) throws Exception {
        
        if (file.length() == 0) return false;
        
        FileInputStream streamIn = new FileInputStream(file);
        
        int loaded;
        int loadedTotal = 0;
        byte[] bf = new byte[Const.FILE_BUFFER_SIZE];
        
        while ((loaded = streamIn.read(bf)) > 0) {
        
            md.update(bf, 0, loaded);
            
            loadedTotal += loaded;
            settings.filesSizeProcessed += loaded;
            if (loadedTotal >= Const.PRINT_STR_AT_EVERY_BYTES) {

                Utils.out.print(Const.PRINT_STR);
                loadedTotal -= Const.PRINT_STR_AT_EVERY_BYTES;
            }
        }
        
        streamIn.close();

        return true;
    }
    
    private String getHash() {
        
        return Utils.printHexBinary(md.digest()).toLowerCase();
    }
    
    private void printAlgo() {
        
        Utils.out.println(5, "Algorithm: "+settings.algorithm);
    }
    
    private String getSourcePathHash() throws Exception {
    
        Utils.out.println("Calculating hash of path ["+settings.sourcePath+"] with settings: ");
        printAlgo();
        Utils.out.println(5, "Hash names: "+settings.hashNames);
        if (settings.includeFiles != null) 
            for (String item : settings.includeFiles) 
                Utils.out.println("Include: "+item);
        if (settings.excludeFiles != null) 
            for (String item : settings.excludeFiles) 
                Utils.out.println("Exclude: "+item);
        
        long start = System.currentTimeMillis();
        
        Utils.out.print("Scanning "+settings.sourcePath+" ");
        
        List<File> files = Utils.scanForFiles(new File(settings.sourcePath), settings);

        files.sort(new Comparator<File>() {
            @Override
            public int compare(File item1, File item2) {
                return item1.getAbsolutePath().compareTo(item2.getAbsolutePath());
            }                
        });

        Utils.out.println();
        Utils.out.println("Files scanned : "+settings.filesScanned);
        Utils.out.println("Files included: "+settings.filesIncluded);
        Utils.out.println("Files size found: "+Utils.describeFileLength(settings.filesSizeFound));
        Utils.out.println("Files size included: "+Utils.describeFileLength(settings.filesSizeIncluded));

        starting();

        for (File file : files) {

            boolean skip = false;
            
            if (file.isDirectory()) {

                Utils.out.print(" [dir] " + file.getPath() + " ");
                
                if (settings.hashNames) hashFileName(file); else skip = true;
                
            } else {

                Utils.out.print("[file] " + file.getPath() + " [" + Utils.describeFileLength(file.length()) + "] ");

                if (settings.hashNames) hashFileName(file); else skip = true;
                if (hashFileContent(file)) skip = false;
            }
            
            Utils.out.println(skip?" skip":" done");
        }

        completed();

        long end = System.currentTimeMillis();
        long time = (end - start) / 1_000;
        
        if (time > 0) {
            
            long bytesPerSecong = settings.filesSizeProcessed / time;
            Utils.out.println("Processing speed: "+Utils.describeFileLength(bytesPerSecong)+" per second.");
        }
        
        return getHash();
    }

    private void printSourcePath() throws Exception {
    
        Utils.out.println("Printing content of path ["+settings.sourcePath+"] with settings: ");
        if (settings.includeFiles != null) 
            for (String item : settings.includeFiles) 
                Utils.out.println("Include: "+item);
        if (settings.excludeFiles != null) 
            for (String item : settings.excludeFiles) 
                Utils.out.println("Exclude: "+item);
        
        Utils.out.print("Scanning "+settings.sourcePath+" ");
        
        List<File> files = Utils.scanForFiles(new File(settings.sourcePath), settings);

        files.sort(new Comparator<File>() {
            @Override
            public int compare(File item1, File item2) {
                return item1.getAbsolutePath().compareTo(item2.getAbsolutePath());
            }                
        });

        Utils.out.println();
        Utils.out.println("Files scanned : "+settings.filesScanned);
        Utils.out.println("Files included: "+settings.filesIncluded);
        Utils.out.println("Files size found: "+Utils.describeFileLength(settings.filesSizeFound));
        Utils.out.println("Files size included: "+Utils.describeFileLength(settings.filesSizeIncluded));

        for (File file : files) {

            if (file.isDirectory()) {

                Utils.out.println(" [dir] " + file.getPath() + " ");
            } else {

                Utils.out.println("[file] " + file.getPath() + " [" + Utils.describeFileLength(file.length()) + "] ");
            }
        }
    }

    private boolean processHash() throws Exception {

        if (!new File(settings.sourcePath).exists()) {
            
            Utils.out.println("Source path ["+settings.sourcePath+"] not exists!");
            return false;
        }
        
        settings.hash = getSourcePathHash();
        HashFileUtils.writeHashFile(settings);
        
        return true;
    }
    
    private boolean processHashString() throws Exception {

        Utils.out.println("Hashing string ["+settings.sourceString+"] with settings: ");
        printAlgo();

        if (settings.sourceString == null || settings.sourceString.length() == 0) {
            
            Utils.out.println("String is empty, nothing todo, exit.");
            return false;
        }

        hashString(settings.sourceString);
        settings.hash = getHash();
        Utils.out.println("Hash code ["+settings.hash+"]");
        
        if (settings.copyResultToClipboard) Utils.sendStringToClipboard(settings.hash);

        return true;
    }
    
    private boolean processPrint() throws Exception {

        if (!new File(settings.sourcePath).exists()) {
            
            Utils.out.println("Source path ["+settings.sourcePath+"] not exists!");
            return false;
        }
        
        printSourcePath();
        
        return true;
    }
    
    private boolean processCheck() throws Exception {
    
        if (!new File(settings.sourcePath).exists()) {
            
            Utils.out.println("Source path ["+settings.sourcePath+"] not exists!");
            return false;
        }
        
        File file = new File(settings.hashFile);
        if (!file.exists()) {
            
            Utils.out.println("Hash file ["+settings.hashFile+"] not exists!");
            return false;
        }
        if (!file.isFile()) {
            
            Utils.out.println("Hash ["+settings.hashFile+"] is not a file!");
            return false;
        }

        HashFileUtils.loadHashFile(settings);
        if (settings.hash == null) {
            
            Utils.out.println("Hash not found in hash file ["+settings.hashFile+"]!");
            return false;
        }
        
        String hashCalc = getSourcePathHash();
        settings.checkSuccess = settings.hash.equals(hashCalc);
        
        Utils.out.println("Hash code from hash-file: "+settings.hash);
        Utils.out.println("Hash code calculated    : "+hashCalc);
        Utils.out.println("Hash from path ["+settings.sourcePath+"] "+(settings.checkSuccess?"IS EQUALS":"IS NOT EQUALS")+" to hash file ["+settings.hashFile+"]");
        
        return true;
    }
    
    public boolean run() throws Exception {
        
        if (!init()) return false;
        
        switch (command) {
            
            case HASH               : return processHash();
            case HASH_STRING        : return processHashString();
            case PRINT              : return processPrint();
            case CHECK              : return processCheck();
            default : return false;
        }
    }
    
    public void processArg(String arg) throws Exception {
        
        String argHash = "hash=";
        String argAlgo = "algo=";
        String argInc = "inc=";
        String argIncFile = "incf=";
        String argExc = "exc=";
        String argExcFile = "excf=";
        String argHashNames = "hashnames=";
        String argToClip = "toclip";
        if (arg.startsWith(argHash)) settings.hashFile = arg.substring(argHash.length()); else
        if (arg.startsWith(argAlgo)) settings.algorithm = arg.substring(argAlgo.length()); else
        if (arg.startsWith(argInc)) settings.addToInclude(arg.substring(argInc.length())); else
        if (arg.startsWith(argIncFile)) settings.addToInclude(Utils.loadFileStrings(arg.substring(argIncFile.length()))); else
        if (arg.startsWith(argExc)) settings.addToExclude(arg.substring(argExc.length())); else
        if (arg.startsWith(argExcFile)) settings.addToExclude(Utils.loadFileStrings(arg.substring(argExcFile.length()))); else
        if (arg.startsWith(argHashNames)) settings.hashNames = Boolean.parseBoolean(arg.substring(argHashNames.length())); else
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

    public void setSourceString(String sourceString) {
        settings.sourceString = sourceString;
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
        
        Utils.out.println("Usage: hash command <params> [options]");
        Utils.out.println("Commands:");
        Utils.out.println( 5, "hash - calculate hash of specified file_or_directory and write result file");
        Utils.out.println(10, "Params: <source_file_or_directory>");
        Utils.out.println(10, "Available options:");
        Utils.out.println(15, "algo=algorithm - specify hash algorithm (default: "+Settings.DEFAULT_ALGORITHM+")");
        Utils.out.println(15, "hash=path - specify path_and_name of result hash file (default: "+Settings.DEFAULT_HASH_FILE+")");
        Utils.out.println(15, "hashnames=true/false - hash files and directories names also with files content (default: "+Settings.DEFAULT_HASH_NAMES+")");
        Utils.out.println(15, "inc=regexp - files or directories to include if matching");
        Utils.out.println(15, "exc=regexp - files or directories to exclude if matching");
        Utils.out.println( 5, "check - check hash-file against specified file_or_directory");
        Utils.out.println(10, "Params: <source_file_or_directory>");
        Utils.out.println(10, "Available options:");
        Utils.out.println(15, "hash=path - specify path_and_name of input hash file (default: "+Settings.DEFAULT_HASH_FILE+")");
        Utils.out.println( 5, "hash_string - calculate hash of specified string and print output");
        Utils.out.println(10, "Params: <source_string> = \"fromclip\" for source string being taken from clipboard");
        Utils.out.println(10, "Available options:");
        Utils.out.println(15, "algo=algorithm - specify hash algorithm (default: "+Settings.DEFAULT_ALGORITHM+")");
        Utils.out.println(15, "toclip - copy result string to clipboard");
        Utils.out.println( 5, "print - print all files of file_or_directory matching conditions");
        Utils.out.println(10, "Params: <source_file_or_directory>");
        Utils.out.println(10, "Available options:");
        Utils.out.println(15, "inc=regexp - files or directories to include if matching");
        Utils.out.println(15, "exc=regexp - files or directories to exclude if matching");
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
    
    public static void main(String[] args) {
        
        try {
            
            if (args == null || args.length == 0) {
                
                printInfo();
                return;
            }
            
            Hash tool = new Hash();

            switch (args[0].toLowerCase()) {
                
                case "hash"  : 
                    tool.setCommand(Command.HASH); 
                    if (!Utils.checkArgs(args, 2)) return;
                    tool.setSourcePath(args[1]);
                    tool.processArgs(args, 2);
                    break;
                case "hash_string"  : 
                    tool.setCommand(Command.HASH_STRING); 
                    if (!Utils.checkArgs(args, 2)) return;
                    String source = args[1];
                    if (source.equalsIgnoreCase("fromclip")) source = Utils.receiveStringFromClipboard();
                    tool.setSourceString(source);
                    tool.processArgs(args, 2);
                    break;
                case "check"  : 
                    tool.setCommand(Command.CHECK); 
                    if (!Utils.checkArgs(args, 2)) return;
                    tool.setSourcePath(args[1]);
                    tool.processArgs(args, 2);
                    break;
                case "print"  : 
                    tool.setCommand(Command.PRINT); 
                    if (!Utils.checkArgs(args, 2)) return;
                    tool.setSourcePath(args[1]);
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