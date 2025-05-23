package ddx.hash;

import ddx.common.Const;
import ddx.common.Progress;
import ddx.common.SizeConv;
import ddx.common.Str;
import ddx.common.Utils;
import ddx.hash.types.FileHash;
import ddx.hash.types.HashIndexFile;
import ddx.hash.types.SimpleFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author druidman
 */
public class HashIndex implements Progress {
    
    public static enum Command {CREATE_INDEX, REFRESH_INDEX, CHECK, CHECK_INDEX, CHECK_INDEX_EQ, ADD, SUB, DELETE, PRINT, INFO, SCAN};
    
    private Command command;
    private final Settings settings = new Settings();

    private boolean init() {
    
        return true;
    }

    private FileHash getFileHash(File file) throws Exception {
        
        if (file.length() == 0) return null;
        
        FileHash fh = new FileHash();
        MessageDigest md = MessageDigest.getInstance("SHA-256");
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

        fh.setHash(md.digest());
        fh.setName(file.getName());
        fh.setSize(file.length());
        
        return fh;
    }
    
    private void printSettings() throws Exception {
        
        if (settings.includeFiles != null) 
            for (String item : settings.includeFiles) 
                Utils.out.println("Include: "+item);
        if (settings.excludeFiles != null) 
            for (String item : settings.excludeFiles) 
                Utils.out.println("Exclude: "+item);
        if (settings.fileMinSize != -1)
            Utils.out.println("File min size: "+settings.fileMinSize);
    }
    
    private HashIndexFile getSourcePathIndexFile(HashIndexFile hifOrig) throws Exception {
    
        Utils.out.println("Calculation hash index of path ["+settings.sourcePath+"] with settings: ");
        printSettings();
        
        boolean refreshMode = hifOrig != null && hifOrig.getLength() > 0;
        
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

        HashIndexFile hif = new HashIndexFile();
        Set<FileHash> hashes = new LinkedHashSet<FileHash>();
        hif.setHashes(hashes);
        
        for (File file : files) {

            boolean skip = false;
            
            if (file.isDirectory()) {

                Utils.out.print(" [dir] " + file.getPath() + " ");
                
                skip = true;
                
            } else {

                long fileLength = file.length();
                
                Utils.out.print("[file] " + file.getPath() + " [" + Utils.describeFileLength(fileLength) + "] ");

                if (refreshMode) {  
                    
                    String fileName = file.getName();
                    
                    for (FileHash fh : hifOrig.getHashes()) {
                        
                        if (fileLength == fh.getSize() && fileName.equals(fh.getName())) {
                            
                            skip = true;
                            break;
                        }
                    }
                } // refresh mode
    
                if (!skip) {
                    
                    FileHash fh = getFileHash(file);
                    if (fh == null) skip = true; else hashes.add(fh);
                }
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
        
        return hif;
    }

    private void checkSourcePathAgainstIndexFile(HashIndexFile hif) throws Exception {
    
        Utils.out.println("Check path ["+settings.sourcePath+"] against index ["+settings.hashFile+"] with settings: ");
        printSettings();
        
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

        if (settings.filesIncluded == 0 || settings.filesSizeIncluded == 0) {
            
            Utils.out.println("nothing to do!");
            return;
        }
        
        int totalFound = 0;
        int totalMissed = 0;
        boolean exportEnabled = !Str.isEmpty(settings.csvFile);
        StringBuilder sb = null;
        
        if (exportEnabled) {
            
            sb = new StringBuilder(1024*1024);

            sb.append("file path").append(";").
               append("file name").append(";").
               append("file length").append(";").
               append("found").append("\n");            
        }
        
        starting();

        Set<FileHash> hashes = hif.getHashes();
        
        for (File file : files) {

            boolean skip = false;
            boolean found = false;
            
            if (file.isDirectory()) {

                Utils.out.print(" [dir] " + file.getPath() + " ");
                
                skip = true;
                
            } else {

                Utils.out.print("[file] " + file.getPath() + " [" + Utils.describeFileLength(file.length()) + "]");

                FileHash fh = getFileHash(file);
                if (fh == null) skip = true; else found = hashes.contains(fh);
                
                if (exportEnabled && !skip) {
                    
                    sb.append(file.getParent()).append(";").append(file.getName()).append(";").
                       append(String.valueOf(file.length())).append(";").append(found?"yes":"no").append("\n");
                }
            }
            
            if (skip) {
                
                Utils.out.println(" skip");
            } else {
                
                if (found) totalFound++; else totalMissed++;
                Utils.out.println(found?" found":" missed");
            }
            
        }

        completed();

        long end = System.currentTimeMillis();
        long time = (end - start) / 1_000;
        
        if (time > 0) {
            
            long bytesPerSecong = settings.filesSizeProcessed / time;
            Utils.out.println("Processing speed: "+Utils.describeFileLength(bytesPerSecong)+" per second.");
        }

        Utils.out.println("Files found  : "+totalFound);
        Utils.out.println("Files missed : "+totalMissed);
        
        if (exportEnabled) {
        
            Utils.out.print("Export to csv ["+settings.csvFile+"] ... ");
            
            OutputStream os = new FileOutputStream(new File(settings.csvFile));
            os.write(sb.toString().getBytes());
            os.flush();
            os.close();

            Utils.out.println("done");
        }

    }

    private boolean processCreateIndex() throws Exception {

        if (!new File(settings.sourcePath).exists()) {
            
            Utils.out.println("Source path ["+settings.sourcePath+"] not exists!");
            return false;
        }
        
        HashIndexFile hif = getSourcePathIndexFile(null);
        HashFileUtils.writeHashIndexFile(hif, settings.hashFile);
        
        return true;
    }
    
    private boolean processRefreshIndex() throws Exception {

        if (!new File(settings.sourcePath).exists()) {
            
            Utils.out.println("Source path ["+settings.sourcePath+"] not exists!");
            return false;
        }
        
        if (!new File(settings.hashFile).exists()) {
            
            Utils.out.println("Index file ["+settings.hashFile+"] not exists!");
            return false;
        }
        
        HashIndexFile hifOrig = HashFileUtils.loadHashIndexFile(settings.hashFile);
        HashIndexFile hifNews = getSourcePathIndexFile(hifOrig);
        
        if (hifNews.getLength() == 0) {
            
            Utils.out.println("No new files found, index is actual.");
            return true;        
        }
        
        int totalAdded = 0;
        
        for (FileHash fh : hifNews.getHashes()) if (hifOrig.getHashes().add(fh)) totalAdded++;
        
        if (totalAdded > 0) {
            
            Utils.out.println("Added "+totalAdded+" files to index.");
        
            HashFileUtils.writeHashIndexFile(hifOrig, settings.hashFile);
        } else {
            
            Utils.out.println("No new files added, index is actual.");
        }
        
        return true;
    }

    private boolean processCheck() throws Exception {

        if (!new File(settings.hashFile).exists()) {
            
            Utils.out.println("Index file ["+settings.hashFile+"] not exists!");
            return false;
        }
        
        HashIndexFile hif = HashFileUtils.loadHashIndexFile(settings.hashFile);
        checkSourcePathAgainstIndexFile(hif);
        
        return true;
    }

    private boolean isFileHashInSettings(FileHash fh) throws Exception {
        
        boolean skip = false;
        
        if ((settings.fileMinSize != -1 && fh.getSize() < settings.fileMinSize) ||
            (settings.fileMaxSize != -1 && fh.getSize() > settings.fileMaxSize)) skip = true;        
        
        if (skip) return false;
            
        if (!Str.isEmpty(settings.name)) skip = !fh.getName().toLowerCase().contains(settings.name.toLowerCase());
        
        if (skip) return false;
        
        if (!Str.isEmpty(settings.hash)) skip = !Utils.toHex(fh.getHash()).toLowerCase().contains(settings.hash.toLowerCase());
           
        return !skip;
    }
    
    private boolean processCheckIndex() throws Exception {

        if (!new File(settings.hashFile).exists()) {
            
            Utils.out.println("index_A ["+settings.hashFile+"] not exists!");
            return false;
        }
        
        if (!new File(settings.hashFile2).exists()) {
            
            Utils.out.println("index_B ["+settings.hashFile2+"] not exists!");
            return false;
        }
        
        HashIndexFile indexA = HashFileUtils.loadHashIndexFile(settings.hashFile);
        
        if (indexA.getLength() == 0) {
            
            Utils.out.println("nothing to do!");
            return true;
        }
        
        HashIndexFile indexB = HashFileUtils.loadHashIndexFile(settings.hashFile2);
        
        if (indexB.getLength() == 0) {
            
            Utils.out.println("nothing to do!");
            return true;
        }
        
        int totalFound = 0;
        int totalMissed = 0; 
        int totalSkipped = 0;
        boolean exportEnabled = !Str.isEmpty(settings.csvFile);
        StringBuilder sb = null;
        
        if (exportEnabled) {
            
            sb = new StringBuilder(1024*1024);

            sb.append("file name").append(";").
               append("file length").append(";").
               append("found").append("\n");            
        }
                
        for (FileHash fh : indexA.getHashes()) {
            
            if (!isFileHashInSettings(fh)) {
                
                totalSkipped++;
                continue;
            }
            
            boolean found = indexB.getHashes().contains(fh);
            if (found) totalFound++; else totalMissed++;

            boolean showFile = (settings.showFiles && ((found && settings.showFilesFound) || (!found && settings.showFilesMissed)));
            
            if (showFile)
                Utils.out.println(fh.getName() + " [" + Utils.describeFileLength(fh.getSize()) + "] "+(found?"found":"missed"));

            if (exportEnabled && showFile) {

                sb.append(fh.getName()).append(";").
                   append(String.valueOf(fh.getSize())).append(";").append(found?"yes":"no").append("\n");
            }            
        }
        
        Utils.out.println("Files skipped : "+totalSkipped);
        Utils.out.println("Files found   : "+totalFound);
        Utils.out.println("Files missed  : "+totalMissed);
        
        if (exportEnabled) {
        
            Utils.out.print("Export to csv ["+settings.csvFile+"] ... ");
            
            OutputStream os = new FileOutputStream(new File(settings.csvFile));
            os.write(sb.toString().getBytes());
            os.flush();
            os.close();

            Utils.out.println("done");
        }

        return true;
    }

    private boolean processCheckIndexEq() throws Exception {

        if (!new File(settings.hashFile).exists()) {
            
            Utils.out.println("index_A ["+settings.hashFile+"] not exists!");
            return false;
        }
        
        if (!new File(settings.hashFile2).exists()) {
            
            Utils.out.println("index_B ["+settings.hashFile2+"] not exists!");
            return false;
        }
        
        HashIndexFile indexA = HashFileUtils.loadHashIndexFile(settings.hashFile);
        HashIndexFile indexB = HashFileUtils.loadHashIndexFile(settings.hashFile2);

        if (indexA.getLength() != indexB.getLength()) {
            
            Utils.out.println("indexes are NOT equal!");
            return true;
        }
        
        boolean eq = true;
        
        if (indexA.getLength() > 0)
            for (FileHash fh : indexA.getHashes()) {

                boolean found = indexB.getHashes().contains(fh);
                if (!found) {

                    eq = false;
                    break;
                }
            }
        
        if (eq) Utils.out.println("indexes are completely equal!"); else
            Utils.out.println("indexes are NOT equal!");
        
        return true;
    }

    private boolean processAdd() throws Exception {

        if (!new File(settings.hashFile).exists()) {
            
            Utils.out.println("index_A ["+settings.hashFile+"] not exists!");
            return false;
        }
        
        if (!new File(settings.hashFile2).exists()) {
            
            Utils.out.println("index_B ["+settings.hashFile2+"] not exists!");
            return false;
        }
        
        HashIndexFile indexA = HashFileUtils.loadHashIndexFile(settings.hashFile);
        
        if (indexA.getLength() == 0) {
            
            Utils.out.println("nothing to do!");
            return true;
        }
        
        HashIndexFile indexB = HashFileUtils.loadHashIndexFile(settings.hashFile2);
        
        if (indexB.getLength() == 0) {
            
            Utils.out.println("nothing to do!");
            return true;
        }
        
        int totalAdded = 0;
        int totalSkipped = 0;
        
        for (FileHash fh : indexA.getHashes()) {

            if (!isFileHashInSettings(fh)) {
                
                totalSkipped++;
                continue;
            }
            
            boolean found = indexB.getHashes().contains(fh);
            if (!found) {
                
                indexB.getHashes().add(fh);
                totalAdded++;
            } else totalSkipped++;
            
            Utils.out.println(fh.getName() + " [" + Utils.describeFileLength(fh.getSize()) + "] "+(!found?"added":"skipped"));
        }
        
        Utils.out.println("Files added   : "+totalAdded);
        Utils.out.println("Files skipped : "+totalSkipped);

        HashFileUtils.writeHashIndexFile(indexB, settings.hashFile3);
        
        return true;
    }

    private boolean processSub() throws Exception {

        if (!new File(settings.hashFile).exists()) {
            
            Utils.out.println("index_A ["+settings.hashFile+"] not exists!");
            return false;
        }
        
        if (!new File(settings.hashFile2).exists()) {
            
            Utils.out.println("index_B ["+settings.hashFile2+"] not exists!");
            return false;
        }
        
        HashIndexFile indexA = HashFileUtils.loadHashIndexFile(settings.hashFile);
        
        if (indexA.getLength() == 0) {
            
            Utils.out.println("nothing to do!");
            return true;
        }
        
        HashIndexFile indexB = HashFileUtils.loadHashIndexFile(settings.hashFile2);
        
        if (indexB.getLength() == 0) {
            
            Utils.out.println("nothing to do!");
            return true;
        }
        
        int totalRemoved = 0;
        int totalSkipped = 0;
        
        for (FileHash fh : indexA.getHashes()) {
            
            if (!isFileHashInSettings(fh)) {
                
                totalSkipped++;
                continue;
            }

            boolean found = indexB.getHashes().contains(fh);
            if (found) {
                
                indexB.getHashes().remove(fh);
                totalRemoved++;
            } else totalSkipped++;
            
            Utils.out.println(fh.getName() + " [" + Utils.describeFileLength(fh.getSize()) + "] "+(found?"removed":"skipped"));
        }
        
        Utils.out.println("Files removed : "+totalRemoved);
        Utils.out.println("Files skipped : "+totalSkipped);

        HashFileUtils.writeHashIndexFile(indexB, settings.hashFile3);
        
        return true;
    }

    private boolean processDelete() throws Exception {

        if (settings.hash == null || settings.hash.length() != 64) {
            
            Utils.out.println("hash code is not correct!");
            return false;
        }
        
        if (!new File(settings.hashFile).exists()) {
            
            Utils.out.println("index file ["+settings.hashFile+"] not exists!");
            return false;
        }
        
        HashIndexFile index = HashFileUtils.loadHashIndexFile(settings.hashFile);
        
        if (index.getLength() == 0) {
            
            Utils.out.println("nothing to do!");
            return true;
        }
        
        byte[] hash = Utils.fromHex(settings.hash);
        FileHash fhRemove = null;
        
        for (FileHash fh : index.getHashes()) {
        
           if (Arrays.equals(fh.getHash(), hash)) {
               
               fhRemove = fh;
               break;
           } 
        }
        
        boolean removed = fhRemove != null && index.getHashes().remove(fhRemove);
        
        if (removed) {

            Utils.out.println("Removed hash: " + Utils.toHex(hash));
            HashFileUtils.writeHashIndexFile(index, settings.hashFile);
        }
        
        return true;
    }

    private boolean processPrint() throws Exception {

        if (!new File(settings.hashFile).exists()) {
            
            Utils.out.println("Index file ["+settings.hashFile+"] not exists!");
            return false;
        }
        
        HashIndexFile hif = HashFileUtils.loadHashIndexFile(settings.hashFile);

        boolean exportEnabled = !Str.isEmpty(settings.csvFile);
        StringBuilder sb = null;
        
        if (exportEnabled) {
            
            sb = new StringBuilder(1024*1024);

            sb.append("name").append(";").
               append("length").append(";").
               append("hash").append("\n");            
        }
        
        boolean fromCsv = !Str.isEmpty(settings.csvFileFrom);
        List<SimpleFile> fromCsvList = null;
        
        if (fromCsv) {
        
            if (!new File(settings.csvFileFrom).exists()) {

                Utils.out.println("Csv file ["+settings.csvFileFrom+"] not exists!");
                return false;
            }
            
            fromCsvList = CsvFileUtils.loadNameSizeFromCsvFile(settings.csvFileFrom);
            
            if (fromCsvList.isEmpty()) {
                
                Utils.out.println("Nothing to print!");
                return true;
            }
        }        
        
        for (FileHash fh : hif.getHashes()) {
            
            boolean show = isFileHashInSettings(fh);

            if (show && fromCsv) {
                
                boolean simpleFound = false;
                
                for (SimpleFile sf : fromCsvList) {
                    
                    if (sf.getSize() == fh.getSize() && sf.getName().equals(fh.getName())) {
                        
                        simpleFound = true;
                        break;
                    }
                }
                
                show = simpleFound;
            }
            
            if (show) {
                
                Utils.out.println(fh.getName() + " " + SizeConv.sizeToStrSimple(fh.getSize()) + " " + Utils.toHex(fh.getHash()));

                if (exportEnabled) {
                    
                    sb.append(fh.getName()).append(";").
                       append(String.valueOf(fh.getSize())).append(";").append(Utils.toHex(fh.getHash())).append("\n");                    
                }
            }
        }
        
        if (exportEnabled) {
        
            Utils.out.print("Export to csv ["+settings.csvFile+"] ... ");
            
            OutputStream os = new FileOutputStream(new File(settings.csvFile));
            os.write(sb.toString().getBytes());
            os.flush();
            os.close();

            Utils.out.println("done");
        }

        return true;
    }
    
    private boolean processInfo() throws Exception {

        if (!new File(settings.hashFile).exists()) {
            
            Utils.out.println("Index file ["+settings.hashFile+"] not exists!");
            return false;
        }
        
        HashIndexFile hif = HashFileUtils.loadHashIndexFile(settings.hashFile);
        
        int totalFiles = hif.getLength();
        long totalFilesSize = 0L;
        
        Utils.out.println("Total files in index: "+totalFiles);
        
        if (totalFiles > 0) {
            
            for (FileHash fh : hif.getHashes()) {
                
                totalFilesSize += fh.getSize();
            }

            Utils.out.println("Total files size: "+SizeConv.sizeToStr(totalFilesSize));
        }

        return true;
    }

    private boolean processScan() throws Exception {

        if (!new File(settings.sourcePath).exists()) {
            
            Utils.out.println("Source path ["+settings.sourcePath+"] not exists!");
            return false;
        }
        
        Utils.out.println("Scan ["+settings.sourcePath+"] with settings: ");
        printSettings();
        
        List<File> files = Utils.scanForFiles(new File(settings.sourcePath), settings);

        Utils.out.println();

        if (settings.showFiles)
            for (File file : files) {

                Utils.out.println(file.getPath());
            }
        
        Utils.out.println("Files scanned : "+settings.filesScanned);
        Utils.out.println("Files included: "+settings.filesIncluded);
        Utils.out.println("Files size found: "+Utils.describeFileLength(settings.filesSizeFound));
        Utils.out.println("Files size included: "+Utils.describeFileLength(settings.filesSizeIncluded));
        
        return true;
    }
    
    public boolean run() throws Exception {
        
        if (!init()) return false;
        
        switch (command) {
            
            case CREATE_INDEX       : return processCreateIndex();
            case REFRESH_INDEX      : return processRefreshIndex();
            case CHECK              : return processCheck();
            case CHECK_INDEX        : return processCheckIndex();
            case CHECK_INDEX_EQ     : return processCheckIndexEq();
            case ADD                : return processAdd();
            case SUB                : return processSub();
            case DELETE             : return processDelete();
            case PRINT              : return processPrint();
            case INFO               : return processInfo();
            case SCAN               : return processScan();
            default : return false;
        }
    }
    
    public void processArg(String arg) throws Exception {
        
        String argName = "name=";
        String argHash = "hash=";
        String argCSV = "csv=";
        String argFromCSV = "fromcsv=";
        String argInc = "inc=";
        String argIncFile = "incf=";
        String argExc = "exc=";
        String argExcFile = "excf=";
        String argHashNames = "hashnames=";
        String argMinSize = "minsize=";
        String argMaxSize = "maxsize=";
        String argShowFiles = "showfiles=";
        String argShowFilesFound = "showfilesfound=";
        String argShowFilesMissed = "showfilesmissed=";
      
        String argToClip = "toclip";
        if (arg.startsWith(argName)) settings.name = arg.substring(argName.length()); else
        if (arg.startsWith(argHash)) settings.hash = arg.substring(argHash.length()); else
        if (arg.startsWith(argCSV)) settings.csvFile = arg.substring(argCSV.length()); else
        if (arg.startsWith(argFromCSV)) settings.csvFileFrom = arg.substring(argFromCSV.length()); else
        if (arg.startsWith(argInc)) settings.addToInclude(arg.substring(argInc.length())); else
        if (arg.startsWith(argIncFile)) settings.addToInclude(Utils.loadFileStrings(arg.substring(argIncFile.length()))); else
        if (arg.startsWith(argExc)) settings.addToExclude(arg.substring(argExc.length())); else
        if (arg.startsWith(argExcFile)) settings.addToExclude(Utils.loadFileStrings(arg.substring(argExcFile.length()))); else
        if (arg.startsWith(argHashNames)) settings.hashNames = Boolean.parseBoolean(arg.substring(argHashNames.length())); else
        if (arg.startsWith(argMinSize)) settings.fileMinSize = SizeConv.strToSize(arg.substring(argMinSize.length())); else
        if (arg.startsWith(argMaxSize)) settings.fileMaxSize = SizeConv.strToSize(arg.substring(argMaxSize.length())); else
        if (arg.startsWith(argShowFiles)) settings.showFiles = Boolean.parseBoolean(arg.substring(argShowFiles.length())); else
        if (arg.startsWith(argShowFilesFound)) settings.showFilesFound = Boolean.parseBoolean(arg.substring(argShowFilesFound.length())); else
        if (arg.startsWith(argShowFilesMissed)) settings.showFilesMissed = Boolean.parseBoolean(arg.substring(argShowFilesMissed.length())); else
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

    public void setHash(String hash) {
        settings.hash = hash;
    }
    
    public void setHashFile(String hashFile) {
        settings.hashFile = hashFile;
    }
    
    public void setHashFile2(String hashFile) {
        settings.hashFile2 = hashFile;
    }
    
    public void setHashFile3(String hashFile) {
        settings.hashFile3 = hashFile;
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
        
        Utils.out.println("Usage: hashindex command <params> [options]");
        Utils.out.println("Commands:");
        Utils.out.println( 5, "createindex - create hash index <index_file> of specified <source_file_or_directory>");
        Utils.out.println(10, "Params: <source_file_or_directory> <index_file>");
        Utils.out.println(10, "Available options:");
        Utils.out.println(15, "inc=regexp - files or directories to include if matching");
        Utils.out.println(15, "exc=regexp - files or directories to exclude if matching");
        Utils.out.println(15, "minsize=size - set minimum file size to include in index file (ex: 100kb)");
        Utils.out.println(15, "maxsize=size - set maximum file size to include in index file (ex: 1tb)");
        Utils.out.println( 5, "refreshindex - fast update (skip same name+size) hash index <index_file> of specified <source_file_or_directory>");
        Utils.out.println(10, "Params: <source_file_or_directory> <index_file>");
        Utils.out.println( 5, "check - check <source_file_or_directory> against <index_file>");
        Utils.out.println(10, "Params: <source_file_or_directory> <index_file>");
        Utils.out.println(10, "Available options:");
        Utils.out.println(15, "csv=file - write result to csv file");
        Utils.out.println( 5, "checkindex - check <index_A> against <index_B>");
        Utils.out.println(10, "Params: <index_A> <index_B>");
        Utils.out.println( 5, "checkindexeq - check <index_A> completely equals <index_B>");
        Utils.out.println(10, "Params: <index_A> <index_B>");
        Utils.out.println( 5, "add - add <index_A> to <index_B> write result to <index_C>");
        Utils.out.println(10, "Params: <index_A> <index_B> <index_C>");
        Utils.out.println( 5, "sub - substract <index_A> from <index_B> write result to <index_C>");
        Utils.out.println(10, "Params: <index_A> <index_B> <index_C>");
        Utils.out.println( 5, "delete - delete hash from <index_file>");
        Utils.out.println(10, "Params: <hash> <index_file>");
        Utils.out.println( 5, "print - print content of <index_file>");
        Utils.out.println(10, "Params: <index_file>");
        Utils.out.println(10, "Available options:");
        Utils.out.println(15, "name=text - name contains text");
        Utils.out.println(15, "hash=hex - hash contains hex");
        Utils.out.println(15, "fromcsv=extract exact name+size from csv");
        Utils.out.println(15, "also: minsize,maxsize,csv");
        Utils.out.println( 5, "info - brief info of <index_file>");
        Utils.out.println(10, "Params: <index_file>");
        Utils.out.println( 5, "scan - give brief info of scan <source_file_or_directory> used in createindex");
        Utils.out.println(10, "Params: <source_file_or_directory>");
        Utils.out.println(10, "Available options: same to createindex + showfiles=boolean showfilesfound/showfilesmissed");
    }

    public static void main(String[] args) {
        
        try {
            
            if (args == null || args.length == 0) {
                
                printInfo();
                return;
            }
            
            HashIndex tool = new HashIndex();

            switch (args[0].toLowerCase()) {
                
                case "createindex"  : 
                    tool.setCommand(Command.CREATE_INDEX); 
                    if (!Utils.checkArgs(args, 3)) return;
                    tool.setSourcePath(args[1]);
                    tool.setHashFile(args[2]);
                    tool.processArgs(args, 3);
                    break;
                case "refreshindex"  : 
                    tool.setCommand(Command.REFRESH_INDEX); 
                    if (!Utils.checkArgs(args, 3)) return;
                    tool.setSourcePath(args[1]);
                    tool.setHashFile(args[2]);
                    tool.processArgs(args, 3);
                    break;
                case "check"  : 
                    tool.setCommand(Command.CHECK); 
                    if (!Utils.checkArgs(args, 3)) return;
                    tool.setSourcePath(args[1]);
                    tool.setHashFile(args[2]);               
                    tool.processArgs(args, 3);
                    break;
                case "checkindex"  : 
                    tool.setCommand(Command.CHECK_INDEX); 
                    if (!Utils.checkArgs(args, 3)) return;
                    tool.setHashFile(args[1]);               
                    tool.setHashFile2(args[2]);               
                    tool.processArgs(args, 3);
                    break;
                case "checkindexeq"  : 
                    tool.setCommand(Command.CHECK_INDEX_EQ); 
                    if (!Utils.checkArgs(args, 3)) return;
                    tool.setHashFile(args[1]);               
                    tool.setHashFile2(args[2]);               
                    tool.processArgs(args, 3);
                    break;
                case "add"  : 
                    tool.setCommand(Command.ADD); 
                    if (!Utils.checkArgs(args, 4)) return;
                    tool.setHashFile(args[1]);               
                    tool.setHashFile2(args[2]);               
                    tool.setHashFile3(args[3]);               
                    tool.processArgs(args, 4);
                    break;
                case "sub"  : 
                    tool.setCommand(Command.SUB); 
                    if (!Utils.checkArgs(args, 4)) return;
                    tool.setHashFile(args[1]);               
                    tool.setHashFile2(args[2]);               
                    tool.setHashFile3(args[3]);               
                    tool.processArgs(args, 4);
                    break;
                case "delete"  : 
                    tool.setCommand(Command.DELETE); 
                    if (!Utils.checkArgs(args, 3)) return;
                    tool.setHash(args[1]);               
                    tool.setHashFile(args[2]);               
                    tool.processArgs(args, 3);
                    break;
                case "print"  : 
                    tool.setCommand(Command.PRINT); 
                    if (!Utils.checkArgs(args, 2)) return;
                    tool.setHashFile(args[1]);
                    tool.processArgs(args, 2);
                    break;
                case "info"  : 
                    tool.setCommand(Command.INFO); 
                    if (!Utils.checkArgs(args, 2)) return;
                    tool.setHashFile(args[1]);
                    tool.processArgs(args, 2);
                    break;
                case "scan"  : 
                    tool.setCommand(Command.SCAN); 
                    if (!Utils.checkArgs(args, 2)) return;
                    tool.setSourcePath(args[1]);
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