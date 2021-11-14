package ddx.filetools;

import ddx.common.Const;
import ddx.common.Progress;
import ddx.common.SizeConv;
import ddx.common.Str;
import ddx.common.Utils;
import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 *
 * @author druidman
 */
public class FileTools implements Progress {
    
    public static enum Command {SEARCH, PATCH, INVERSE, BYTE_VALUE, SHA256, PRINT, CUT, RESTORE, ENTROPY};

    private Command command;
    private final Settings settings = new Settings();
    
    private class Settings {
        
        public String filePath;
        public String filePath2;
        public long start;
        public long length;
        public int max;
        public byte[] workBf;
        public int shuffle;
        public long portion;
        public long position;
        public int fileBfSize;
    }
    
    @Override
    public void starting() {
    }
    
    @Override
    public void completed() {
    }

    private boolean init() throws Exception {
        
        return true;
    }

    private boolean processSearch() throws Exception {

        File file = new File(settings.filePath);
        
        if (!file.exists() || file.isDirectory()) {

            Utils.out.println("File ["+settings.filePath+"] isn't correct!");
            return false;
        }
        
        if (Utils.isEmptyBuffer(settings.workBf)) {
            
            Utils.out.println("Search is not defined!");
            return false;
        }
        
        int SP = 30;

        Utils.out.println(Str.getStringWPrefix("Searching in file", SP, " ", false)+" : "+file.getAbsolutePath());
        Utils.out.println(Str.getStringWPrefix("File length", SP, " ", false)+" : "+Utils.describeFileLength(file.length()));
        Utils.out.println(Str.getStringWPrefix("Start position", SP, " ", false)+" : "+Const.NUM_FORMATTER.format(settings.start));
        Utils.out.println(Str.getStringWPrefix("Searching for bytes", SP, " ", false)+" : "+Utils.toHex(settings.workBf)+" length "+settings.workBf.length);
        
        FileSearch tool = new FileSearch();
        if (settings.fileBfSize > 0) tool.setFileBfSize(settings.fileBfSize);
        tool.init(file, settings.start, SP);
        tool.start();
        tool.process(settings.workBf);
        tool.end();
        tool.printSpeed();
        
        return true;
    }
    
    private boolean processPatch() throws Exception {

        File file = new File(settings.filePath);
        
        if (!file.exists() || file.isDirectory()) {

            Utils.out.println("File ["+settings.filePath+"] isn't correct!");
            return false;
        }
        
        if (Utils.isEmptyBuffer(settings.workBf)) {
            
            Utils.out.println("Patch is not defined!");
            return false;
        }
        
        if (settings.position + settings.workBf.length > file.length()) {
            
            Utils.out.println("Patching exceed file length!");
            return false;
        }
        
        int SP = 30;

        Utils.out.println(Str.getStringWPrefix("Patching file", SP, " ", false)+" : "+file.getAbsolutePath());
        Utils.out.println(Str.getStringWPrefix("File length", SP, " ", false)+" : "+Utils.describeFileLength(file.length()));
        Utils.out.println(Str.getStringWPrefix("Patching position", SP, " ", false)+" : "+Const.NUM_FORMATTER.format(settings.position));
        Utils.out.println(Str.getStringWPrefix("Patching with bytes", SP, " ", false)+" : "+Utils.toHex(settings.workBf)+" length "+settings.workBf.length);
        
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        raf.seek(settings.position);
        raf.write(settings.workBf);
        raf.close();

        Utils.out.println("Done.");
        
        return true;
    }
    
    private boolean processPrint() throws Exception {
        
        File file = new File(settings.filePath);
        
        if (!file.exists() || file.isDirectory()) {

            Utils.out.println("File ["+settings.filePath+"] isn't correct!");
            return false;
        }
        
        int SP = 30;

        Utils.out.println(Str.getStringWPrefix("Printing part of file", SP, " ", false)+" : "+file.getAbsolutePath());
        Utils.out.println(Str.getStringWPrefix("File length", SP, " ", false)+" : "+Utils.describeFileLength(file.length()));
        if (settings.start > 0)
            Utils.out.println(Str.getStringWPrefix("Start position", SP, " ", false)+" : "+Const.NUM_FORMATTER.format(settings.start));
        
        int length = settings.length>0?(int)settings.length:(int)(file.length()-settings.start);
        
        Utils.out.println(Str.getStringWPrefix("Length, bytes", SP, " ", false)+" : "+Const.NUM_FORMATTER.format(length));
        Utils.out.println(Str.getStringWPrefix("Length, bits", SP, " ", false)+" : "+Const.NUM_FORMATTER.format(length * 8));
        
        FileInputStream fis = new FileInputStream(file);
        if (settings.start > 0) fis.getChannel().position(settings.start);
        byte[] bf = new byte[length];
        int totalReadSize = fis.read(bf);
        fis.close();
        
        if (totalReadSize != length) {
            
            Utils.out.print("Wrong total read size ["+totalReadSize+"]");
            return false;
        }
        
        Utils.out.println("Output, hex ["+Utils.toHex(bf)+"]");
        Utils.out.println("Output, raw "+Arrays.toString(bf));
        
        return true;
    }
    
    private boolean processInverse() throws Exception {

        File file = new File(settings.filePath);
        
        if (!file.exists() || file.isDirectory()) {

            Utils.out.println("File ["+settings.filePath+"] isn't correct!");
            return false;
        }
        
        File fileDst = new File(settings.filePath2);
        
        if (fileDst.exists() && !fileDst.delete()) {

            Utils.out.println("File ["+settings.filePath2+"] exists!");
            return false;
        }
        
        int SP = 30;

        Utils.out.println(Str.getStringWPrefix("Inversing file", SP, " ", false)+" : "+file.getAbsolutePath());
        Utils.out.println(Str.getStringWPrefix("File length", SP, " ", false)+" : "+Utils.describeFileLength(file.length()));
        Utils.out.println(Str.getStringWPrefix("To file", SP, " ", false)+" : "+fileDst.getAbsolutePath());
        
        FileInverse tool = new FileInverse();
        if (settings.fileBfSize > 0) tool.setFileBfSize(settings.fileBfSize);
        tool.init(file, fileDst, settings.shuffle);

        if (settings.shuffle > 0)
        Utils.out.println(Str.getStringWPrefix("With shuffling", SP, " ", false)+" : "+Const.NUM_FORMATTER.format(settings.shuffle)+
                " times of each buffer of "+Const.NUM_FORMATTER.format(tool.getFileBfSize()));

        tool.start();
        tool.process();
        tool.end();
        tool.printSpeed();
        
        return true;
    }

    private boolean processByteValue() throws Exception {

        File file = new File(settings.filePath);
        
        if (!file.exists() || file.isDirectory()) {

            Utils.out.println("File ["+settings.filePath+"] isn't correct!");
            return false;
        }
        
        int SP = 30;

        Utils.out.println(Str.getStringWPrefix("Calc byte value of file", SP, " ", false)+" : "+file.getAbsolutePath());
        Utils.out.println(Str.getStringWPrefix("File length", SP, " ", false)+" : "+Utils.describeFileLength(file.length()));
        
        FileByteValue tool = new FileByteValue();
        if (settings.fileBfSize > 0) tool.setFileBfSize(settings.fileBfSize);
        tool.init(file);
        tool.start();
        tool.process();
        tool.end();
        tool.printSpeed();
        
        return true;
    }
    
    private boolean processSha256() throws Exception {

        File file = new File(settings.filePath);
        
        if (!file.exists() || file.isDirectory()) {

            Utils.out.println("File ["+settings.filePath+"] isn't correct!");
            return false;
        }
        
        int SP = 30;

        Utils.out.println(Str.getStringWPrefix("Calc sha256 of file", SP, " ", false)+" : "+file.getAbsolutePath());
        Utils.out.println(Str.getStringWPrefix("File length", SP, " ", false)+" : "+Utils.describeFileLength(file.length()));
        
        FileSha256 tool = new FileSha256();
        if (settings.fileBfSize > 0) tool.setFileBfSize(settings.fileBfSize);
        tool.init(file);
        tool.start();
        tool.process();
        tool.end();
        tool.printSpeed();
        
        return true;
    }
    
    private boolean processEntropy() throws Exception {

        File file = new File(settings.filePath);
        
        if (!file.exists() || file.isDirectory()) {

            Utils.out.println("File ["+settings.filePath+"] isn't correct!");
            return false;
        }
        
        int SP = 30;

        Utils.out.println(Str.getStringWPrefix("Calc entropy of file", SP, " ", false)+" : "+file.getAbsolutePath());
        Utils.out.println(Str.getStringWPrefix("File length", SP, " ", false)+" : "+Utils.describeFileLength(file.length()));
        
        FileEntropy tool = new FileEntropy();
        if (settings.fileBfSize > 0) tool.setFileBfSize(settings.fileBfSize);
        tool.init(file);
        tool.start();
        tool.process();
        tool.end();
        tool.printSpeed();
        
        return true;
    }
    
    private boolean processCut() throws Exception {

        File file = new File(settings.filePath);
        
        if (!file.exists() || file.isDirectory()) {

            Utils.out.println("File ["+settings.filePath+"] isn't correct!");
            return false;
        }
        
        File fileDst = new File(Utils.completePath(settings.filePath2));

        if (fileDst.exists() && !fileDst.isDirectory()) {

            Utils.out.println("File ["+settings.filePath2+"] exists and not directory!");
            return false;
        }
        
        Path dstd = Paths.get(Utils.completePath(settings.filePath2));
        if (dstd != null && !Files.exists(dstd)) Files.createDirectories(dstd);
        
        int SP = 30;

        Utils.out.println(Str.getStringWPrefix("Cutting file", SP, " ", false)+" : "+file.getAbsolutePath());
        Utils.out.println(Str.getStringWPrefix("Cutting file size", SP, " ", false)+" : "+SizeConv.sizeToStrSimple(file.length()));
        Utils.out.println(Str.getStringWPrefix("Output directory", SP, " ", false)+" : "+fileDst.getAbsolutePath());
        if (settings.start > 0)
            Utils.out.println(Str.getStringWPrefix("Start position", SP, " ", false)+" : "+Const.NUM_FORMATTER.format(settings.start));
        Utils.out.println(Str.getStringWPrefix("Into portions of", SP, " ", false)+" : "+SizeConv.sizeToStrSimple(settings.portion));
        if (settings.max > 0)
            Utils.out.println(Str.getStringWPrefix("Max portions", SP, " ", false)+" : "+Const.NUM_FORMATTER.format(settings.max));
        
        FileCut tool = new FileCut();
        if (settings.fileBfSize > 0) tool.setFileBfSize(settings.fileBfSize);
        tool.init(file, fileDst, settings.max, settings.start, settings.portion);
        tool.start();
        tool.process();
        tool.end();
        tool.printSpeed();
        
        return true;
    }

    private boolean processRestore() throws Exception {

        File file = new File(settings.filePath);
        
        if (!file.exists() || file.isDirectory()) {

            Utils.out.println("File ["+settings.filePath+"] isn't correct!");
            return false;
        }
        
        File fileDst = new File(settings.filePath2);

        if (fileDst.exists() && !fileDst.delete()) {

            Utils.out.println("File ["+settings.filePath2+"] exists!");
            return false;
        }
        
        Path dstd = Paths.get(settings.filePath2).getParent();
        if (dstd != null && !Files.exists(dstd)) Files.createDirectories(dstd);
        
        int SP = 30;

        Utils.out.println(Str.getStringWPrefix("Restoring to file", SP, " ", false)+" : "+fileDst.getAbsolutePath());
        Utils.out.println(Str.getStringWPrefix("First piece", SP, " ", false)+" : "+file.getAbsolutePath());
        
        FileRestore tool = new FileRestore();
        if (settings.fileBfSize > 0) tool.setFileBfSize(settings.fileBfSize);
        tool.init(file, fileDst);
        tool.start();
        tool.process();
        tool.end();
        tool.printSpeed();
        
        return true;
    }

    public boolean run() throws Exception {
        
        if (!init()) return false;
        
        switch (command) {
            
            case SEARCH               : return processSearch();
            case PATCH                : return processPatch();
            case PRINT                : return processPrint();
            case INVERSE              : return processInverse();
            case BYTE_VALUE           : return processByteValue();
            case SHA256               : return processSha256();
            case ENTROPY              : return processEntropy();
            case CUT                  : return processCut();
            case RESTORE              : return processRestore();
            default : return false;
        }
    }
    
    public void processArg(String arg) throws Exception {
        
        String argStart = "start=";
        String argLength = "len=";
        String argMax = "max=";
        String argShuffle = "shuffle=";
        String argFileBf = "filebf=";
        if (arg.startsWith(argStart)) settings.start = Long.parseLong(arg.substring(argStart.length())); else
        if (arg.startsWith(argLength)) settings.length = Long.parseLong(arg.substring(argLength.length())); else
        if (arg.startsWith(argMax)) settings.max = Integer.parseInt(arg.substring(argMax.length())); else
        if (arg.startsWith(argShuffle)) settings.shuffle = Integer.parseInt(arg.substring(argShuffle.length())); else
        if (arg.startsWith(argFileBf)) settings.fileBfSize = (int)SizeConv.strToSize(arg.substring(argFileBf.length())); else
        Utils.out.println("Unknown argument: "+arg);
    }

    public void processArgs(String[] args, int startIndex) throws Exception {
        
        for (int i = startIndex; i < args.length; i++) processArg(args[i]);
    }

    private static void printInfo() {
        
        Utils.out.println("Usage: filetools command <params> [options]");
        Utils.out.println("Commands:");
        Utils.out.println( 5, "search - search exact bytes position in file");
        Utils.out.println(10, "Params: <filepath> <hex_string>");
        Utils.out.println(10, "Available options:");
        Utils.out.println(15, "start=long - starting position");

        Utils.out.println( 5, "patch - patch file by replacing bytes in file at defined position");
        Utils.out.println(10, "Params: <filepath> <hex_string> <position>");

        Utils.out.println( 5, "print - print part of bin-file to console");
        Utils.out.println(10, "Params <filepath>");
        Utils.out.println(10, "Available options:");
        Utils.out.println(15, "start=long - starting position");
        Utils.out.println(15, "len=long - number of bytes");

        Utils.out.println( 5, "inverse - inverse file bytes");
        Utils.out.println(10, "Params: <filepath_src> <filepath_dest>");
        Utils.out.println(10, "Available options:");
        Utils.out.println(15, "shuffle=int - set cycle count value > 0 to enable shuffling");
        Utils.out.println(15, "filebf=int - size of file buffer for loading & shuffling (DEFAULT: "+Utils.describeFileLength(Const.FILE_BUFFER_SIZE)+")");

        Utils.out.println( 5, "byte - calculate all bytes xor and add values of file");
        Utils.out.println(10, "Params: <filepath>");

        Utils.out.println( 5, "sha256 - calculate sha256 value of file");
        Utils.out.println(10, "Params: <filepath>");

        Utils.out.println( 5, "entropy - calculate file entropy");
        Utils.out.println(10, "Params: <filepath>");
        
        Utils.out.println( 5, "cut - cut file to pieces");
        Utils.out.println(10, "Params: <filepath> <out_directory> <portion>");
        Utils.out.println(10, "Available options:");
        Utils.out.println(15, "start=long - starting position");
        Utils.out.println(15, "max=int - max number of portions");
        
        Utils.out.println( 5, "restore - restore file from pieces");
        Utils.out.println(10, "Params: <filepath_first_piece> <filepath_restored>");
    }

    public void setCommand(Command command) {
        this.command = command;
    }

    public void setFilePath(String value) {
        settings.filePath = value;
    }
    
    public void setFilePath2(String value) {
        settings.filePath2 = value;
    }
    
    public void setWorkBf(String value) {
        settings.workBf = Utils.fromHex(value);
    }

    public void setPortion(String value) throws Exception {
        settings.portion = SizeConv.strToSize(value);
    }

    public void setPosition(String value) throws Exception {
        settings.position = Long.parseLong(value);
    }
    
    public static void main(String[] args) {
        
        try {

            if (args == null || args.length == 0) {
                
                printInfo();
                return;
            }

            FileTools tool = new FileTools();
            
            switch (args[0].toLowerCase()) {
                
                case "search"  : 
                    tool.setCommand(Command.SEARCH); 
                    if (!Utils.checkArgs(args, 3)) return;
                    tool.setFilePath(args[1]);
                    tool.setWorkBf(args[2]);
                    tool.processArgs(args, 3);
                    break;
                case "patch"  : 
                    tool.setCommand(Command.PATCH); 
                    if (!Utils.checkArgs(args, 4)) return;
                    tool.setFilePath(args[1]);
                    tool.setWorkBf(args[2]);
                    tool.setPosition(args[3]);
                    break;
                case "print"  : 
                    tool.setCommand(Command.PRINT); 
                    if (!Utils.checkArgs(args, 2)) return;
                    tool.setFilePath(args[1]);
                    tool.processArgs(args, 2);
                    break;
                case "inverse"  : 
                    tool.setCommand(Command.INVERSE); 
                    if (!Utils.checkArgs(args, 3)) return;
                    tool.setFilePath(args[1]);
                    tool.setFilePath2(args[2]);
                    tool.processArgs(args, 3);
                    break;
                case "byte"  : 
                    tool.setCommand(Command.BYTE_VALUE); 
                    if (!Utils.checkArgs(args, 2)) return;
                    tool.setFilePath(args[1]);
                    tool.processArgs(args, 2);
                    break;
                case "sha256"  : 
                    tool.setCommand(Command.SHA256); 
                    if (!Utils.checkArgs(args, 2)) return;
                    tool.setFilePath(args[1]);
                    tool.processArgs(args, 2);
                    break;
                case "entropy"  : 
                    tool.setCommand(Command.ENTROPY); 
                    if (!Utils.checkArgs(args, 2)) return;
                    tool.setFilePath(args[1]);
                    tool.processArgs(args, 2);
                    break;
                case "cut"  : 
                    tool.setCommand(Command.CUT); 
                    if (!Utils.checkArgs(args, 4)) return;
                    tool.setFilePath(args[1]);
                    tool.setFilePath2(args[2]);
                    tool.setPortion(args[3]);
                    tool.processArgs(args, 4);
                    break;
                case "restore"  : 
                    tool.setCommand(Command.RESTORE); 
                    if (!Utils.checkArgs(args, 3)) return;
                    tool.setFilePath(args[1]);
                    tool.setFilePath2(args[2]);
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