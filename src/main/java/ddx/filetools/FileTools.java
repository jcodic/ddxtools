package ddx.filetools;

import ddx.common.CommonSettigns;
import ddx.common.Const;
import ddx.common.Progress;
import ddx.common.SizeConv;
import ddx.common.Str;
import ddx.common.Utils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author druidman
 */
public class FileTools implements Progress {
    
    public static enum Command {SEARCH, PATCH, PATCH_FILE, PATCH_TEXT, INVERSE, BYTE_VALUE, SHA256, SHA384, SHA512, SHA1, SHA3, MD5, PRINT, CUT, RESTORE, ENTROPY, RENAME};
    public static enum PrintOutput {HEX,BYTE,UBYTE,BITS};
    
    public static int DEFAULT_PRINT_WIDTH = 40;
    public static PrintOutput DEFAULT_PRINT_OUTPUT = PrintOutput.HEX;
            
    private Command command;
    private final Settings settings = new Settings();
    
    private class Settings extends CommonSettigns {
        
        public String filePath;
        public String filePath2;
        public String filePath3;
        public long start;
        public long length;
        public int max;
        public byte[] workBf;
        public int shuffle;
        public int width = DEFAULT_PRINT_WIDTH;
        public PrintOutput output = DEFAULT_PRINT_OUTPUT;
        public long portion;
        public long position;
        public int fileBfSize;
        public String prefix;
        public String postfix;
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
    
    private boolean processPatchFile() throws Exception {

        File file = new File(settings.filePath);
        
        if (!file.exists() || file.isDirectory()) {

            Utils.out.println("File ["+settings.filePath+"] isn't correct!");
            return false;
        }
        
        File filePatch = new File(settings.filePath2);
        
        if (!filePatch.exists() || filePatch.isDirectory() || filePatch.length() == 0) {

            Utils.out.println("File ["+settings.filePath2+"] isn't correct!");
            return false;
        }
        
        if (settings.position + filePatch.length() > file.length()) {
            
            Utils.out.println("Patching exceed file length!");
            return false;
        }
        
        byte[] patch = new byte[(int)filePatch.length()];
        
        FileInputStream fis = new FileInputStream(filePatch);
        int totalRead = fis.read(patch);
        fis.close();
        
        if (totalRead != patch.length) {
            
            Utils.out.println("Patch file read error!");
            return false;
        }
        
        int SP = 30;

        Utils.out.println(Str.getStringWPrefix("Patching file", SP, " ", false)+" : "+file.getAbsolutePath());
        Utils.out.println(Str.getStringWPrefix("File length", SP, " ", false)+" : "+Utils.describeFileLength(file.length()));
        Utils.out.println(Str.getStringWPrefix("Patching position", SP, " ", false)+" : "+Const.NUM_FORMATTER.format(settings.position));
        Utils.out.println(Str.getStringWPrefix("Patching with file", SP, " ", false)+" : "+filePatch.getAbsolutePath());
        Utils.out.println(Str.getStringWPrefix("Patching length", SP, " ", false)+" : "+Utils.describeFileLength(patch.length));
        
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        raf.seek(settings.position);
        raf.write(patch);
        raf.close();

        Utils.out.println("Done.");
        
        return true;
    }
    
    private boolean processPatchText() throws Exception {

        if (Str.isEmpty(settings.filePath)) {
            
            Utils.out.println("Wrong source file!");
            return false;
        }
        
        File file = new File(settings.filePath);
        boolean processBulk = file.isDirectory();
        
        if (processBulk) {
            
            List<File> toProcess = new LinkedList<>();
            
            for (File localFile : file.listFiles()) {
                
                if (!localFile.isDirectory()) toProcess.add(localFile);
            }
            
            if (toProcess.isEmpty()) {
                
                Utils.out.println("No files to process!");
                return false;
            }
            
            List<File> toProcessSorted = new ArrayList<>(toProcess.size());
            for (File localFile : toProcess) toProcessSorted.add(localFile);
            Collections.sort(toProcessSorted);
            
            boolean allOk = true;
            
            for (File localFile : toProcessSorted) allOk &= processPatchText(localFile);
            
            return allOk;
            
        } else {
            
            return processPatchText(file);
        }
    }
    
    private boolean processPatchText(File file) throws Exception {

        if (!file.exists() || file.isDirectory()) {

            Utils.out.println("File ["+settings.filePath+"] isn't correct!");
            return false;
        }
        
        if (Str.isEmpty(settings.filePath2)) {

            Utils.out.println("Text to patch is wrong!");
            return false;
        }
        
        if (Str.isEmpty(settings.filePath3)) {

            Utils.out.println("Text to patch with is wrong!");
            return false;
        }

        byte[] patchBf = settings.filePath2.getBytes(settings.charset);
        byte[] patchWithBf = settings.filePath3.getBytes(settings.charset);
        
        if (patchBf.length != patchWithBf.length) {

            Utils.out.println("Patch and patch with text must be same length!");
            return false;
        }
        
        long len = file.length();
        
        if (len > Integer.MAX_VALUE - 2) {
            
            Utils.out.println("File is too long!");
            return false;
        }
        
        int SP = 30;

        Utils.out.println(Str.getStringWPrefix("Patching file", SP, " ", false)+" : "+file.getAbsolutePath());
        Utils.out.println(Str.getStringWPrefix("File length", SP, " ", false)+" : "+Utils.describeFileLength(len));
        Utils.out.println(Str.getStringWPrefix("Patching text", SP, " ", false)+" : "+settings.filePath2);
        Utils.out.println(Str.getStringWPrefix("Patching with text", SP, " ", false)+" : "+settings.filePath3);
        
        byte[] bf = new byte[(int)len];
        
        FileInputStream fis = new FileInputStream(file);
        int read = fis.read(bf);
        fis.close();
        
        if (read != len) {
            
            Utils.out.println("File read error!");
            return false;
        }
        
        int totalPatches = 0;
        
        for (int i = 0; i < bf.length - patchBf.length; i++) {
            
            boolean match = true;
            
            for (int j = 0; j < patchBf.length; j++) {
                
                if (bf[i+j] != patchBf[j]) {
                    
                    match = false;
                    break;
                }
            }
            
            if (!match) continue;
            
            for (int j = 0; j < patchBf.length; j++) bf[i+j] = patchWithBf[j];
            
            totalPatches++;
        }
        
        Utils.out.println(Str.getStringWPrefix("Total patches", SP, " ", false)+" : "+Const.NUM_FORMATTER.format(totalPatches));
        
        if (totalPatches > 0) {
            
            Utils.out.println("Deleting original file ...");
            
            if (!file.delete()) {
                
                Utils.out.println("Can't delete original file!!");
                return false;
            }

            Utils.out.println("Writing patched file ...");

            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bf);
            fos.close();
        }

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
        Utils.out.println(Str.getStringWPrefix("Output", SP, " ", false)+" : "+settings.output.name());
        
        FileInputStream fis = new FileInputStream(file);
        if (settings.start > 0) fis.getChannel().position(settings.start);
        byte[] bf = new byte[length];
        int totalReadSize = fis.read(bf);
        fis.close();
        
        if (totalReadSize != length) {
            
            Utils.out.print("Wrong total read size ["+totalReadSize+"]");
            return false;
        }
        
        boolean formatted = settings.width > 0;
        int valueWidth = 0;

        switch (settings.output) {

            case HEX    : valueWidth = 2; break;
            case BYTE   : valueWidth = 4; break;
            case UBYTE  : valueWidth = 3; break;
            case BITS   : valueWidth = 8; break;
        }
        
        if (formatted) {
            
            String header = "";

            for (int i = 0; i < Math.min(settings.width, bf.length); i++) {

                header += (i>0?" ":"") + Str.getStringWPrefix(String.valueOf(i), valueWidth, "0", true);
            }

            Utils.out.println(header);
            Utils.out.println(Str.getStringWPrefix("", header.length(), "-"));
        }
        
        int pos = 0;
        while (pos < bf.length) {

            String line = "";

            for (int i = 0; i < (formatted?settings.width:DEFAULT_PRINT_WIDTH); i++) {

                String s = "";
                String space = " ";
                switch (settings.output) {

                    case HEX    : s = Utils.toHex(new byte[]{bf[pos]}); space = formatted?" ":""; break;
                    case BYTE   : s = formatted?Str.getStringWPrefix(String.valueOf(bf[pos]), valueWidth, " ", true):String.valueOf(bf[pos]); break;
                    case UBYTE  : s = formatted?Str.getStringWPrefix(String.valueOf(Byte.toUnsignedInt(bf[pos])), valueWidth, " ", true):String.valueOf(Byte.toUnsignedInt(bf[pos])); break;
                    case BITS   : s = Utils.toString(Utils.toBitset(bf[pos])); space = formatted?" ":""; break;
                }

                line += ((i>0 || (!formatted && pos > 0))?space:"") + s;
                if (++pos == bf.length) break;
            }

            if (formatted) Utils.out.println(line); else Utils.out.print(line);
        }
        
        if (!formatted) Utils.out.println();
        
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
    
    private boolean processFileHash(String algo) throws Exception {

        File file = new File(settings.filePath);
        
        if (!file.exists() || file.isDirectory()) {

            Utils.out.println("File ["+settings.filePath+"] isn't correct!");
            return false;
        }
        
        int SP = 30;

        Utils.out.println(Str.getStringWPrefix("Calc "+algo+" of file", SP, " ", false)+" : "+file.getAbsolutePath());
        Utils.out.println(Str.getStringWPrefix("File length", SP, " ", false)+" : "+Utils.describeFileLength(file.length()));
        
        FileHash tool = new FileHash();
        if (settings.fileBfSize > 0) tool.setFileBfSize(settings.fileBfSize);
        tool.init(file, algo);
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

    private boolean processRename() throws Exception {

        File file = new File(settings.filePath);
        
        if (!file.exists() || !file.isDirectory()) {

            Utils.out.println("File ["+settings.filePath+"] isn't correct!");
            return false;
        }

        List<File> files = new LinkedList<>();
        
        for (File item : file.listFiles()) {
            
            if (item.isFile()) files.add(item);
        }
        
        if (files.isEmpty()) {
            
            Utils.out.println("No files to rename!");
            return false;
        }
    
        int startIndex = (int)settings.start;
        if (startIndex < 1) startIndex = 1;
        int numFiles = files.size();
        int numFilesSymLen = String.valueOf(startIndex + numFiles).length();

        List<File> filesSorted = new ArrayList<>(numFiles);
        filesSorted.addAll(files);

        filesSorted.sort(new Comparator<File>() {
            @Override
            public int compare(File item1, File item2) {
                return item1.getAbsolutePath().compareTo(item2.getAbsolutePath());
            }                
        });
        
        
        int SP = 30;

        Utils.out.println(Str.getStringWPrefix("Renaming files in dir", SP, " ", false)+" : "+file.getAbsolutePath());
        Utils.out.println(Str.getStringWPrefix("Number of files", SP, " ", false)+" : "+numFiles);

        int counter = startIndex;
        int successTotal = 0;
        int failedTotal = 0;
        
        for (File item : filesSorted) {
            
            String counterStr = String.valueOf(counter++);
            while (counterStr.length() < numFilesSymLen) counterStr = "0" + counterStr;
            
            String postFix = "";
            if (!Str.isEmpty(settings.postfix)) {
                
                if (settings.postfix.equals("ext")) {
                    
                    String name = item.getName();
                    if (!Str.isEmpty(name)) {
                        
                        int index = name.lastIndexOf(".");
                        if (index > 0) {
                            
                            postFix = name.substring(index);
                        }
                    }
                } else {
                    
                    postFix = settings.postfix;
                }
            }
            
            String renameTo = 
                    (Str.isEmpty(settings.prefix)?"":settings.prefix) +
                    counterStr +
                    postFix;
            
            File renameToFile = new File(Utils.completePath(item.getParentFile().getAbsolutePath())+renameTo);
            
            Utils.out.print("Rename ["+item.getName()+"] -> ["+renameToFile.getName()+"] ");
            
            boolean success = item.renameTo(renameToFile);
            
            Utils.out.println(success?"ok":"failed!");
            
            if (success) successTotal++; else failedTotal++;
        }
        
        Utils.out.println(Str.getStringWPrefix("Total renamed", SP, " ", false)+" : "+Const.FORMATTER_INT.format(successTotal));
        Utils.out.println(Str.getStringWPrefix("Total failed", SP, " ", false)+" : "+Const.FORMATTER_INT.format(failedTotal));
        
        return true;
    }

    private long parseFilePos(String value) throws Exception {
        
        if (Str.isEmpty(value)) throw new Exception("Wrong file position value!");
        File baseFile = new File(settings.filePath);
        if (!baseFile.exists() || !baseFile.isFile()) throw new Exception("Can't find file position, no base file!");
        long baseFileLen = baseFile.length();
        boolean fromEnd = value.charAt(0) == '-';
        long pos = SizeConv.strToSize(fromEnd?value.substring(1):value);
        if (fromEnd) {
            
            pos = baseFileLen-pos;
        }
        if (pos < 0 || pos >= baseFileLen) throw new Exception("Wrong file position value!");
        return pos;
    }
    
    public boolean run() throws Exception {
        
        if (!init()) return false;
        
        switch (command) {
            
            case SEARCH               : return processSearch();
            case PATCH                : return processPatch();
            case PATCH_FILE           : return processPatchFile();
            case PATCH_TEXT           : return processPatchText();
            case PRINT                : return processPrint();
            case INVERSE              : return processInverse();
            case BYTE_VALUE           : return processByteValue();
            case SHA256               : return processFileHash("SHA-256");
            case SHA384               : return processFileHash("SHA-384");
            case SHA512               : return processFileHash("SHA-512");
            case SHA1                 : return processFileHash("SHA-1");
            case SHA3                 : return processFileHash("SHA3-256");
            case MD5                  : return processFileHash("MD5");
            case ENTROPY              : return processEntropy();
            case CUT                  : return processCut();
            case RESTORE              : return processRestore();
            case RENAME               : return processRename();
            default : return false;
        }
    }
    
    public void processArg(String arg) throws Exception {
        
        String argPos = "pos=";
        String argStart = "start=";
        String argLength = "len=";
        String argMax = "max=";
        String argShuffle = "shuffle=";
        String argFileBf = "filebf=";
        String argPrefix = "prefix=";
        String argPostfix = "postfix=";
        String argCharset = "charset=";
        String argWidth = "width=";
        String argOutput = "output=";
        if (arg.startsWith(argPos)) settings.start = parseFilePos(arg.substring(argPos.length())); else
        if (arg.startsWith(argStart)) settings.start = Long.parseLong(arg.substring(argStart.length())); else
        if (arg.startsWith(argLength)) settings.length = Long.parseLong(arg.substring(argLength.length())); else
        if (arg.startsWith(argMax)) settings.max = Integer.parseInt(arg.substring(argMax.length())); else
        if (arg.startsWith(argShuffle)) settings.shuffle = Integer.parseInt(arg.substring(argShuffle.length())); else
        if (arg.startsWith(argFileBf)) settings.fileBfSize = (int)SizeConv.strToSize(arg.substring(argFileBf.length())); else
        if (arg.startsWith(argPrefix)) settings.prefix = arg.substring(argPrefix.length()); else
        if (arg.startsWith(argPostfix)) settings.postfix = arg.substring(argPostfix.length()); else
        if (arg.startsWith(argCharset)) settings.charset = arg.substring(argCharset.length()); else
        if (arg.startsWith(argWidth)) settings.width = Integer.parseInt(arg.substring(argWidth.length())); else
        if (arg.startsWith(argOutput)) settings.output = PrintOutput.valueOf(arg.substring(argOutput.length()).toUpperCase()); else
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
        Utils.out.println(15, "pos=value - starting position (ex: 100, 1mb, -10kb (from the end of file)");

        Utils.out.println( 5, "patch - patch file by replacing bytes in file at defined position");
        Utils.out.println(10, "Params: <filepath> <hex_string> <position>");

        Utils.out.println( 5, "patch_file - patch file by replacing bytes in file at defined position");
        Utils.out.println(10, "Params: <filepath_to_patch> <filepath_to_patch_with> <position>");

        Utils.out.println( 5, "patch_text - patch file by replacing text in file at any place");
        Utils.out.println(10, "Params: <filepath_to_patch> <text_to_replace> <text_to_replace_with>");

        Utils.out.println( 5, "print - print part of bin-file to console");
        Utils.out.println(10, "Params <filepath>");
        Utils.out.println(10, "Available options:");
        Utils.out.println(15, "pos=value - starting position");
        Utils.out.println(15, "len=long - number of bytes");
        Utils.out.println(15, "width=int - number of bytes per line (DEFAULT: "+DEFAULT_PRINT_WIDTH+"; use 0 for unformatted)");
        Utils.out.println(15, "output="+Arrays.toString(PrintOutput.values())+" - choose output (DEFAULT: "+DEFAULT_PRINT_OUTPUT+")");

        Utils.out.println( 5, "inverse - inverse file bytes");
        Utils.out.println(10, "Params: <filepath_src> <filepath_dest>");
        Utils.out.println(10, "Available options:");
        Utils.out.println(15, "shuffle=int - set cycle count value > 0 to enable shuffling");
        Utils.out.println(15, "filebf=int - size of file buffer for loading & shuffling (DEFAULT: "+Utils.describeFileLength(Const.FILE_BUFFER_SIZE)+")");

        Utils.out.println( 5, "byte - calculate all bytes xor and add values of file");
        Utils.out.println(10, "Params: <filepath>");

        Utils.out.println( 5, "sha256, sha384, sha512, sha1, sha3, md5 - calculate hash value of file");
        Utils.out.println(10, "Params: <filepath>");

        Utils.out.println( 5, "entropy - calculate file entropy");
        Utils.out.println(10, "Params: <filepath>");
        
        Utils.out.println( 5, "cut - cut file to pieces");
        Utils.out.println(10, "Params: <filepath> <out_directory> <portion>");
        Utils.out.println(10, "Available options:");
        Utils.out.println(15, "pos=value - starting position");
        Utils.out.println(15, "max=int - max number of portions");
        
        Utils.out.println( 5, "restore - restore file from pieces");
        Utils.out.println(10, "Params: <filepath_first_piece> <filepath_restored>");
        
        Utils.out.println( 5, "rename - rename files in folder with format [prefix]<counter>[postfix]");
        Utils.out.println(10, "Params: <filepath_src> - source folder with files");
        Utils.out.println(10, "Available options: prefix=str, postfix=str (ext to keep current ext)");
        Utils.out.println(15, "start=int - starting index");
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
    
    public void setFilePath3(String value) {
        settings.filePath3 = value;
    }
    
    public void setWorkBf(String value) {
        settings.workBf = Utils.fromHex(value);
    }

    public void setPortion(String value) throws Exception {
        settings.portion = SizeConv.strToSize(value);
    }

    public void setPosition(String value) throws Exception {
        settings.position = parseFilePos(value);
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
                case "patch_file"  : 
                    tool.setCommand(Command.PATCH_FILE); 
                    if (!Utils.checkArgs(args, 4)) return;
                    tool.setFilePath(args[1]);
                    tool.setFilePath2(args[2]);
                    tool.setPosition(args[3]);
                    break;
                case "patch_text"  : 
                    tool.setCommand(Command.PATCH_TEXT); 
                    if (!Utils.checkArgs(args, 4)) return;
                    tool.setFilePath(args[1]);
                    tool.setFilePath2(args[2]);
                    tool.setFilePath3(args[3]);
                    tool.processArgs(args, 4);
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
                case "sha384"  : 
                    tool.setCommand(Command.SHA384); 
                    if (!Utils.checkArgs(args, 2)) return;
                    tool.setFilePath(args[1]);
                    tool.processArgs(args, 2);
                    break;
                case "sha512"  : 
                    tool.setCommand(Command.SHA512); 
                    if (!Utils.checkArgs(args, 2)) return;
                    tool.setFilePath(args[1]);
                    tool.processArgs(args, 2);
                    break;
                case "sha1"  : 
                    tool.setCommand(Command.SHA1); 
                    if (!Utils.checkArgs(args, 2)) return;
                    tool.setFilePath(args[1]);
                    tool.processArgs(args, 2);
                    break;
                case "sha3"  : 
                    tool.setCommand(Command.SHA3); 
                    if (!Utils.checkArgs(args, 2)) return;
                    tool.setFilePath(args[1]);
                    tool.processArgs(args, 2);
                    break;
                case "md5"  : 
                    tool.setCommand(Command.MD5); 
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
                case "rename"  : 
                    tool.setCommand(Command.RENAME); 
                    if (!Utils.checkArgs(args, 2)) return;
                    tool.setFilePath(args[1]);
                    tool.processArgs(args, 2);
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