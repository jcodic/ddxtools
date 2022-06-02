package ddx.matches;

import ddx.common.Const;
import ddx.common.Progress;
import ddx.common.SizeConv;
import ddx.common.Utils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 *
 * @author druidman
 */
public class FindMatches implements Progress {
    
    private final Settings settings = new Settings();
    
    @Override
    public void starting() {
    }
    
    @Override
    public void completed() {
    }

    private Set<HFile> getSameName(HFile oriFile, Set<HFile> files) throws Exception {
        
        String oriName = oriFile.name;
        Set<HFile> sameFiles = new LinkedHashSet<>();
        
        for (HFile file : files) if (file.name.equals(oriName)) sameFiles.add(file);
        
        return sameFiles;
    }

    private Set<HFile> getSameSize(HFile oriFile, Set<HFile> files) throws Exception {
        
        long oriSize = oriFile.length;
        Set<HFile> sameFiles = new LinkedHashSet<>();
        
        for (HFile file : files) if (file.length == oriSize) sameFiles.add(file);
        
        return sameFiles;
    }

    private byte[] getFileContentHash(HFile file) throws Exception {
        
        if (file.length == 0) return new byte[1];
        if (file.hashCache != null) return file.hashCache;
        
        if (settings.maxInfo) Utils.out.print("Calculating hash of "+file.path+" ");
        
        MessageDigest md = MessageDigest.getInstance(Settings.DEFAULT_HASH_ALGORITHM);
        
        FileInputStream streamIn = new FileInputStream(file.file);
        
        int loaded;
        int loadedTotal = 0;
        byte[] bf = new byte[Const.FILE_BUFFER_SIZE];
        
        while ((loaded = streamIn.read(bf)) > 0) {
        
            md.update(bf, 0, loaded);
            
            loadedTotal += loaded;
            settings.filesSizeProcessed += loaded;
            if (loadedTotal >= Const.PRINT_STR_AT_EVERY_BYTES) {

                if (settings.maxInfo) Utils.out.print(Const.PRINT_STR);
                loadedTotal -= Const.PRINT_STR_AT_EVERY_BYTES;
            }
        }
        
        streamIn.close();
        
        if (settings.maxInfo) Utils.out.println("done");
        
        return file.hashCache = md.digest();
    }

    private byte[] getFileContentFast(HFile file) throws Exception {
        
        if (file.length == 0) return new byte[1];
        if (file.hashCache != null) return file.hashCache;
        
        if (settings.maxInfo) Utils.out.print("Calculating hash of first "+SizeConv.sizeToStrSimple(Const.FILE_BUFFER_SIZE)+" of "+file.path+" ");
        
        MessageDigest md = MessageDigest.getInstance(Settings.DEFAULT_HASH_ALGORITHM);
        
        FileInputStream streamIn = new FileInputStream(file.file);
        
        int loaded;
        int loadedTotal = 0;
        byte[] bf = new byte[Const.FILE_BUFFER_SIZE];
        
        if ((loaded = streamIn.read(bf)) > 0) {
        
            md.update(bf, 0, loaded);
            
            loadedTotal += loaded;
            settings.filesSizeProcessed += loaded;
            if (loadedTotal >= Const.PRINT_STR_AT_EVERY_BYTES) {

                if (settings.maxInfo) Utils.out.print(Const.PRINT_STR);
                loadedTotal -= Const.PRINT_STR_AT_EVERY_BYTES;
            }
        }
        
        streamIn.close();
        
        if (settings.maxInfo) Utils.out.println("done");
        
        return file.hashCache = md.digest();
    }

    private boolean sameContent(HFile file1, HFile file2) throws IOException {
        
        if (file1.length != file2.length) return false;
        if (settings.sameContentMinSize != -1 && settings.sameContentMinSize > file1.length) return false;
        if (file1.length == 0) return true;
        
        if (settings.maxInfo) Utils.out.print("Comparing bytes ["+file1.toString()+"] & ["+file2.toString()+"] ");

        if (file1.length < Const.FILE_BUFFER_SIZE) {
            
            boolean eq = Arrays.equals(Files.readAllBytes(file1.file.toPath()), Files.readAllBytes(file2.file.toPath()));
            if (settings.maxInfo) Utils.out.println(eq?"eq":"not eq");
            return eq;
        }

        try (InputStream is1 = Files.newInputStream(file1.file.toPath());
             InputStream is2 = Files.newInputStream(file2.file.toPath())) {
            byte[] bf = new byte[Const.FILE_BUFFER_SIZE];
            byte[] bf2 = new byte[Const.FILE_BUFFER_SIZE];
            int read, read2;
            int loadedTotal = 0;
            while ((read = is1.read(bf)) != -1) {
                
                loadedTotal += read;
                int left = read;
                int index = 0;
                while (left > 0) {
                    
                    read2 = is2.read(bf2, 0, left);
                    loadedTotal += read2;
                    if (read2 == -1) throw new IOException("Unexpected eof!");
                    for (int i = 0; i < read2; i++) if (bf2[i] != bf[index + i]) {
                        
                        if (settings.maxInfo) Utils.out.println("not eq");
                        return false;
                    }
                    index += read2;
                    left -= read2;
                }
                
                if (loadedTotal >= Const.PRINT_STR_AT_EVERY_BYTES) {

                    if (settings.maxInfo) Utils.out.print(Const.PRINT_STR);
                    loadedTotal -= Const.PRINT_STR_AT_EVERY_BYTES;
                }
            }
        }

        if (settings.maxInfo) Utils.out.println("eq");
        return true;
    }
    
    private boolean sameContentHash(HFile file1, HFile file2) throws Exception {
        
        if (file1.length != file2.length) return false;
        if (settings.sameContentMinSize != -1 && settings.sameContentMinSize > file1.length) return false;
        if (file1.length == 0) return true;
        
        return Arrays.equals(getFileContentHash(file1), getFileContentHash(file2));
    }
    
    private boolean sameContentFast(HFile file1, HFile file2) throws Exception {
        
        if (file1.length != file2.length) return false;
        if (settings.sameContentMinSize != -1 && settings.sameContentMinSize > file1.length) return false;
        if (file1.length == 0) return true;
        
        return Arrays.equals(getFileContentFast(file1), getFileContentFast(file2));
    }
    
    private Set<HFile> getSameContentBytes(HFile oriFile, Set<HFile> files) throws Exception {
        
        Set<HFile> sameFiles = new LinkedHashSet<>();
        
        for (HFile file : files) if (sameContent(oriFile, file)) sameFiles.add(file);
        
        return sameFiles;
    }

    private Set<HFile> getSameContentHash(HFile oriFile, Set<HFile> files) throws Exception {
        
        Set<HFile> sameFiles = new LinkedHashSet<>();
        
        for (HFile file : files) if (sameContentHash(oriFile, file)) sameFiles.add(file);
        
        return sameFiles;
    }

    private Set<HFile> getSameContentFast(HFile oriFile, Set<HFile> files) throws Exception {
        
        Set<HFile> sameFiles = new LinkedHashSet<>();
        
        for (HFile file : files) if (sameContentFast(oriFile, file)) sameFiles.add(file);
        
        return sameFiles;
    }

    private Set<HFile> getSameContent(HFile oriFile, Set<HFile> files) throws Exception {
        
        switch (settings.contentCompare) {
            
            case BYTES : return getSameContentBytes(oriFile, files);
            case HASH  : return getSameContentHash(oriFile, files);
            case FAST  : return getSameContentFast(oriFile, files);
            default    : return null;
        }
    }

    private void exportResults() throws Exception {
        
        StringBuilder sb = new StringBuilder(128*1024);

        sb.append("").append(";").append("file path").append(";").
           append("file name").append(";").append("file length").append(";").
           append("deleted").append("\n");
        
        settings.exportList.sort(new Comparator<Combo>() {
            @Override
            public int compare(Combo item1, Combo item2) {
                return item1.file.path.compareTo(item2.file.path);
            }                
        });
        
        for (Combo combo : settings.exportList) {
            
            combo.duplicates.sort(new Comparator<HFile>() {
                @Override
                public int compare(HFile item1, HFile item2) {
                    return item1.path.compareTo(item2.path);
                }                
            });
            
            sb.append("[file]").append(";").append(combo.file.path).append(";").
               append(combo.file.name).append(";").append(combo.file.length).append(";").
               append(combo.file.deleted?"yes;":"no").append("\n");
            
            int c = 1;
            
            for (HFile duplicate : combo.duplicates) {
            
                sb.append(String.valueOf(c++)).append(";").append(duplicate.path).append(";").
                   append(duplicate.name).append(";").append(duplicate.length).append(";").
                   append(duplicate.deleted?"yes;":"no").append("\n");
            }
            
            sb.append("\n");
        }
        
        OutputStream os = new FileOutputStream(new File(settings.exportFile));
        os.write(sb.toString().getBytes());
        os.flush();
        os.close();
    }

    private boolean init() throws Exception {
        
        if (settings.exportFile != null && settings.exportFile.length() > 0) {
            
            settings.exportEnabled = true;
            settings.exportList = new LinkedList<>();
        }
        
        return true;
    }
    
    private boolean run() throws Exception {

        Utils.out.println("Finding matching files with settings: ");
        Utils.out.println("Same name: "+settings.sameName);
        Utils.out.println("Same size: "+settings.sameSize);
        Utils.out.println("Same content: "+settings.sameContent);
        if (settings.sameContentMinSize != -1)
            Utils.out.println("Same content min size: "+settings.sameContentMinSize);
        if (settings.sameContent) 
            Utils.out.println("Same content compare method: "+settings.contentCompare.name());
        if (settings.includeFiles != null) 
            for (String item : settings.includeFiles) 
                Utils.out.println("Include: "+item);
        if (settings.excludeFiles != null) 
            for (String item : settings.excludeFiles) 
                Utils.out.println("Exclude: "+item);

        if (settings.paths.isEmpty()) {
            
            Utils.out.println("No paths defined!");
            return false;
        }
        
        if (!settings.sameName && !settings.sameSize) {
            
            Utils.out.println("Same name or same size must be enabled!");
            return false;
        }
        
        if (settings.sameContent && !settings.sameSize) {
            
            Utils.out.println("Same content only used with same size!");
            return false;
        }

        long start = System.currentTimeMillis();

        starting();

        Set<HFile> files = new LinkedHashSet<>();
        
        for (String path : settings.paths) {
            
            File filePath = new File(path);
            if (!filePath.exists()) {
                
                Utils.out.println("Path ["+path+"] is not exists!");
                return false;
            }
            
            Utils.out.print("Scanning "+path+" ");
            
            for (File file : Utils.scanForFiles(filePath, settings)) if (file.isFile()) files.add(new HFile(file));
            
            Utils.out.println("done");
        }
        
        if (files.isEmpty()) {
            
            Utils.out.println("No files found, nothing todo!");
            return false;
        }
        
        Utils.out.println();
        Utils.out.println("Files found: "+files.size());

        int filesUnMatched = 0;
        int filesCombos = 0;
        int filesDeleted = 0;
        
        while (!files.isEmpty()) {

            HFile file = files.iterator().next();
            files.remove(file);
            Set<HFile> sameFiles = files;
            
            if (!sameFiles.isEmpty() && settings.sameName) sameFiles = getSameName(file, sameFiles);
            if (!sameFiles.isEmpty() && settings.sameSize) sameFiles = getSameSize(file, sameFiles);
            if (!sameFiles.isEmpty() && settings.sameContent) sameFiles = getSameContent(file, sameFiles);
            
            if (sameFiles.isEmpty()) {

                filesUnMatched++;
                
                if (!settings.showMatchesOnly) {
                    
                    Utils.out.println("[file] " + file.path + " [" + Utils.describeFileLength(file.length) + "] no matches");
                }
            } else {
                
                filesCombos++;
                files.removeAll(sameFiles);
                
                if (settings.exportEnabled) settings.exportList.add(new Combo(file, sameFiles));

                Utils.out.println("[file] " + file.path + " [" + Utils.describeFileLength(file.length) + "] matches list:");
                
                int c = 1;
                for (HFile sameFile : sameFiles) {
                
                    String deleteTeg = "";
                    
                    if (settings.deleteDuplicates) {
                        
                        if (sameFile.deleted = sameFile.file.delete()) {
                            
                            deleteTeg = " * deleted";
                            filesDeleted++;
                        }
                    }
                    
                    String s = String.valueOf(c++);
                    while (s.length() < 4) s = "0" + s;
                    
                    Utils.out.println("["+s+"] " + sameFile.path + " [" + Utils.describeFileLength(sameFile.length) + "]"+deleteTeg);
                }

                Utils.out.println();
            }
        }

        if (settings.exportEnabled) exportResults();
        
        completed();

        Utils.out.println("Files unmatched: "+filesUnMatched);
        Utils.out.println("Files combos   : "+filesCombos);
        if (settings.deleteDuplicates) Utils.out.println("Files deleted  : "+filesDeleted);
        
        long end = System.currentTimeMillis();
        long time = (end - start) / 1_000;
        
        if (time > 0) {
            
            Utils.out.println("Processing time: "+time+" second(s).");
        }
        
        return true;
    }

    public void processArg(String arg) throws Exception {
        
        String argPath = "path=";
        String argName = "name=";
        String argSize = "size=";
        String argContent = "content=";
        String argContentMinSize = "contentminsize=";
        String argContentComp = "contentcompare=";
        String argMatchesOnly = "matchesonly=";
        String argInc = "inc=";
        String argExc = "exc=";
        String argExport = "export=";
        String argMaxInfo = "maxinfo";
        String argAutoDelete = "autodelete";
        if (arg.startsWith(argPath)) settings.paths.add(arg.substring(argPath.length())); else
        if (arg.startsWith(argName)) settings.sameName = Boolean.parseBoolean(arg.substring(argName.length())); else
        if (arg.startsWith(argSize)) settings.sameSize = Boolean.parseBoolean(arg.substring(argSize.length())); else
        if (arg.startsWith(argContent)) settings.sameContent = Boolean.parseBoolean(arg.substring(argContent.length())); else
        if (arg.startsWith(argContentMinSize)) settings.sameContentMinSize = SizeConv.strToSize(arg.substring(argContentMinSize.length())); else
        if (arg.startsWith(argContentComp)) settings.contentCompare = Settings.getContentCompare(arg.substring(argContentComp.length())); else
        if (arg.startsWith(argMatchesOnly)) settings.showMatchesOnly = Boolean.parseBoolean(arg.substring(argMatchesOnly.length())); else
        if (arg.startsWith(argInc)) settings.addToInclude(arg.substring(argInc.length())); else
        if (arg.startsWith(argExc)) settings.addToExclude(arg.substring(argExc.length())); else
        if (arg.startsWith(argExport)) settings.exportFile = arg.substring(argExport.length()); else
        if (arg.equals(argMaxInfo)) settings.maxInfo = true; else
        if (arg.equals(argAutoDelete)) settings.deleteDuplicates = true; else
        Utils.out.println("Unknown argument: "+arg);
    }

    public void processArgs(String[] args, int startIndex) throws Exception {
        
        for (int i = startIndex; i < args.length; i++) processArg(args[i]);
    }

    private static void printInfo() {
        
        Utils.out.println("Usage: matches <params> [options]");
        Utils.out.println("Params:");
        Utils.out.println( 5, "path=path_to_file_or_directory - add file or directory in search, may add multiple paths using multiple params");
        Utils.out.println("Available options:");
        Utils.out.println( 5, "name=true/false - compare files by name (default: "+Settings.DEFAULT_SAME_NAME+")");
        Utils.out.println( 5, "size=true/false - compare files by size (default: "+Settings.DEFAULT_SAME_SIZE+")");
        Utils.out.println( 5, "content=true/false - compare files by content (default: "+Settings.DEFAULT_SAME_CONTENT+")");
        Utils.out.println( 5, "contentminsize=size - set minimum file size when comparing by content (ex: 10mb)");
        Utils.out.println( 5, "contentcompare=bytes/hash/fast - compare method (default: "+Settings.DEFAULT_CONTENT_COMPARE.name()+")");
        Utils.out.println( 5, "matchesonly=true/false - show only files with matches (default: "+Settings.DEFAULT_SHOW_MATCHES_ONLY+")");
        Utils.out.println( 5, "inc=regexp - files or directories to include if matching");
        Utils.out.println( 5, "exc=regexp - files or directories to exclude if matching");
        Utils.out.println( 5, "export=path_to_csv_file - export matched files list to CSV file");
        Utils.out.println( 5, "maxinfo - give max possible info to console");
        Utils.out.println( 5, "autodelete - auto delete duplicate files");
    }

    
    public static void main(String[] args) {
        
        try {

            if (args == null || args.length == 0) {
                
                printInfo();
                return;
            }
            
            FindMatches tool = new FindMatches();
            tool.processArgs(args, 0);
            if (tool.init()) tool.run();
            
        } catch (Exception ex) { ex.printStackTrace(); }
    }        
    
}