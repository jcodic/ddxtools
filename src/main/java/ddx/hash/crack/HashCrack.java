package ddx.hash.crack;

import ddx.common.Const;
import ddx.common.Progress;
import ddx.common.Utils;
import ddx.common.brute.BruteCommon;
import ddx.common.brute.BruteMultithread;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author druidman
 */
public class HashCrack implements Progress {
    
    public static enum Command {CRACK, PERFORMANCE};

    private static final DecimalFormat FORMATTER_INT = Const.FORMATTER_INT;
    private static final DecimalFormat FORMATTER_PR = new DecimalFormat("##0.00");
    private static final SimpleDateFormat FORMATTER_TIME = new SimpleDateFormat("HH:mm:ss");
    private static final double HASH_SWITCH = 10.0d; // switch to higher level if reached N hashes (kH,mH,...)
    
    private Command command;
    private final Settings settings = new Settings();
    private ProcessingStatus status = new ProcessingStatus();
        
    
    private class ProcessingStatus {
        
        long totalCombinations;
        boolean timePassed;
        boolean allStopped;
        boolean allCompleted;
        boolean wordFound;
        String wordFoundStr;
        boolean timerUsed;
        long useMaxTime;
        long statusLastUpdated;
        long statusTime;
        long start, now, passed;
        long totalVariantsProcessed;
        long totalTimeConsumed;
        double totalProcessingSpeed;
        String totalVariantsProcessedStr;
        boolean speedStats;
        long avgTimeConsumed;
        double avgProcessingSpeed;
    }
    
    private boolean init() {
    
        return initMessageDigest();
    }

    private boolean initMessageDigest() {

        try {
            
            MessageDigest.getInstance(settings.algorithm);
        } catch (NoSuchAlgorithmException ex) {
            
            Utils.out.println("Hash algorithm ["+settings.algorithm+"] is not found!");
            return false;
        }
        
        return true;
    }

    private Character[] getCharsetByCode(char code) throws Exception {
        
        final String latinLo = "abcdefghijklmnopqrstuvwxyz";
        final String latinUp = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        final String digi = "0123456789";
        final String commonLo = "0123456789abcdef";
        final String commonUp = "0123456789ABCDEF";
        final String symb = " !\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";
        
        String charset = null;
        
        switch (code) {
            
            case 'l' : charset = latinLo; break;
            case 'u' : charset = latinUp; break;
            case 'd' : charset = digi; break;
            case 'h' : charset = commonLo; break;
            case 'H' : charset = commonUp; break;
            case 's' : charset = symb; break;
            case 'a' : charset = latinLo+latinUp+digi+symb; break;
            default  : throw new Exception("Unknown charset code ["+code+"]");
        }
        
        return Utils.getCharArr(charset);
    }
    
    private void addCharset(Object[] charset, boolean isDict, String mask) throws Exception {
        
        settings.charsets.add(charset);
        settings.charsetIsDict.add(isDict);
        settings.actualMask += mask;
    }

    private void parseMask() throws Exception {
        
        char[] mask = new char[settings.mask.length()];
        settings.mask.getChars(0, mask.length, mask, 0);
        
        int i = 0;
        
        while (i < mask.length) {
            
            if (settings.maxChars != null && settings.maxChars == settings.charsets.size()) break;
            
            char c = mask[i++];
            
            if (c == '?') {
                
                char code = mask[i++];
                
                if (code >= '0' && code <= '9') addCharset(settings.userCharsets[code-'0'], settings.userCharsetIsDict[code-'0'], c + "" + code);
                else addCharset(getCharsetByCode(code), false, c + "" + code);
                
            } else {
                
                addCharset(new Character[]{c}, false, "" + c);
            }
        }
    }

    private boolean charsetFilled() throws Exception {
        
        if (settings.minChars != null && settings.minChars > settings.charsets.size()) return false;
        if (settings.maxChars != null && settings.maxChars > settings.charsets.size()) return false;
        return true;
    }
    
    private BruteCommon getBrute(int minLength, int maxLength) {

        BruteCommon task = new BruteCommon(maxLength);
        for (int i = 0; i < maxLength; i++) task.setVariants(i, settings.charsets.get(i));
        for (int i = 1; i < minLength; i++) task.setStartIndex(i, 0);
        task.init();
        return task;
    }
    
    private void printStatus(BruteThread[] threads, boolean complete) throws Exception {
    
        Utils.out.println("-- status @ "+FORMATTER_TIME.format(status.now)+" --");
        
        status.totalVariantsProcessed = 0;
        status.totalTimeConsumed = 0; // in ms
        status.totalProcessingSpeed = 0; // in variants per second
        status.totalVariantsProcessedStr = "";
        status.speedStats = complete;
        
        for (BruteThread thread : threads) {
            
            long timeConsumed = thread.getTotalTimeConsumed();
            if (timeConsumed == 0) status.speedStats = false;
            long variantsProcessed = thread.getTotalVariantsProcessed();
            status.totalVariantsProcessed += variantsProcessed;
            status.totalVariantsProcessedStr += (status.totalVariantsProcessedStr.length()>0?", ":"")+Utils.describeHashTotal(variantsProcessed, HASH_SWITCH);
            status.totalTimeConsumed += timeConsumed;
            if (status.speedStats) {
                status.totalProcessingSpeed += variantsProcessed / (timeConsumed / 1_000.0d);
            }
        }
        
        status.avgTimeConsumed = status.totalTimeConsumed / threads.length;
        status.avgProcessingSpeed = status.totalProcessingSpeed / threads.length;
        double totalVariantsProcessedPr = status.totalVariantsProcessed * 100.0d / status.totalCombinations;
        
        if (complete) {
            Utils.out.println("Word is "+(status.wordFound?"FOUND":"NOT FOUND"));
            if (status.wordFound) {

                Utils.out.println("Source word: "+status.wordFoundStr);
                Utils.out.println("Source word length: "+status.wordFoundStr.length());
            }
        }
        Utils.out.println("totalVariantsProcessed: "+FORMATTER_INT.format(status.totalVariantsProcessed)+" ("+
                Utils.describeHashTotal(status.totalVariantsProcessed, HASH_SWITCH)+") "+
                FORMATTER_PR.format(totalVariantsProcessedPr)+" %");
        if (complete) {
            Utils.out.println("totalVariantsProcessed by each thread: "+status.totalVariantsProcessedStr);
            Utils.out.println("avgTimeConsumed: "+FORMATTER_INT.format(status.avgTimeConsumed / 1_000.0d)+" seconds per thread");
            if (status.speedStats) {

                if (threads.length > 1)
                    Utils.out.println("avgProcessingSpeed: "+Utils.describeHashTotal(status.totalProcessingSpeed, HASH_SWITCH)+"/s for "+threads.length+" threads");
                Utils.out.println("avgProcessingSpeed: "+Utils.describeHashTotal(status.avgProcessingSpeed, HASH_SWITCH)+"/s per thread");
            } else {

                Utils.out.println("Speed stats unavailable as some threads consumed zero time.");
            }
        } else {
            
            if (status.passed > 10 * 1_000) {
                
                status.totalProcessingSpeed = status.totalVariantsProcessed / (status.passed / 1_000.0d);
                Utils.out.println("avgProcessingSpeed: "+Utils.describeHashTotal(status.totalProcessingSpeed, HASH_SWITCH)+"/s");
                BigDecimal big = new BigDecimal(status.passed / 1_000L);
                big = big.multiply(new BigDecimal(status.totalCombinations));
                big = big.divide(new BigDecimal(status.totalVariantsProcessed), 2, RoundingMode.HALF_UP);
                double timeEstimated = big.doubleValue();
                Utils.out.println("estTimeLeft: "+Utils.describeTimeTotal(timeEstimated, 10.0d));
            }
        }
    }
    
    private boolean processThreads(BruteThread[] threads) throws Exception {

        status.timePassed = false;
        status.allStopped = false;
        status.allCompleted = true;
        status.wordFound = false;
        status.wordFoundStr = null;
        status.timerUsed = settings.runTime != -1 || settings.statusTime != -1;
        status.useMaxTime = settings.runTime * 1_000;
        status.statusTime = settings.statusTime * 1_000;
        status.start = System.currentTimeMillis();
        status.statusLastUpdated = status.start;
        
        for (BruteThread thread : threads) thread.start();
        
        while (!status.allStopped) {
            
            status.allStopped = true;
            
            for (BruteThread thread : threads) {
                if (!thread.isStopped()) {
                    
                    status.allStopped = false;
                    break;
                }
            }
            
            for (BruteThread thread : threads) {
                if (thread.isFound()) {
                    
                    status.wordFound = true;
                    break;
                }
            }
            
            if (status.timerUsed) {
                
                status.now = System.currentTimeMillis();
                status.passed = status.now - status.start;
                if (settings.runTime != -1 && status.passed > status.useMaxTime) status.timePassed = true;
                if (settings.statusTime != -1 && status.now - status.statusLastUpdated >= status.statusTime) {
                    
                    printStatus(threads, false);
                    status.statusLastUpdated = status.now;
                }
            }
            
            if (status.timePassed || status.wordFound) for (BruteThread thread : threads) thread.doComplete();
            
            Thread.sleep(500L);
        }

        for (BruteThread thread : threads) {

            if (!thread.isCompleted()) {

                status.allCompleted = false;
            }

            if (thread.isFound()) {

                status.wordFound = true;
                status.wordFoundStr = thread.getCurrentWord();
            }
        }

        if (status.timePassed) {
            
            Utils.out.println("All time has been used!");
        }
        
        if (!status.allCompleted) {
            
            Utils.out.println("Not all threads are being completed!");
            return false;
        }

        printStatus(threads, true);
        
        return true;
    }
    
    private boolean processCrack() throws Exception {

        if (settings.hash == null) {
            
            Utils.out.println("Hash is not defined!");
            return false;
        }

        byte[] hashBytes = Utils.parseHexBinary(settings.hash);
        
        Utils.out.println("Cracking hash ["+settings.hash+"] with settings: ");
        Utils.out.println(5, "Algorithm: "+settings.algorithm);
        Utils.out.println(5, "Mask defined: "+settings.mask);
        Utils.out.println(5, "CPU threads used: "+settings.threadsNum);
        Utils.out.println(5, "Max run time: "+(settings.runTime==-1?"infinite":settings.runTime)+" seconds");
        
        settings.charsets = new ArrayList<>(10);
        settings.charsetIsDict = new ArrayList<>(10);
        settings.actualMask = "";
        
        do {
            
            parseMask();
        
        } while (!charsetFilled());
        
        if (settings.minChars == null) settings.minChars = settings.charsets.size();
        if (settings.maxChars == null) settings.maxChars = settings.charsets.size();

        Utils.out.println(5, "Number of chars (words) min: "+settings.minChars);
        Utils.out.println(5, "Number of chars (words) max: "+settings.maxChars);
        Utils.out.println(5, "Mask used: "+settings.actualMask);
        
        boolean[] isDictIndex = null;
        boolean hasDictCharset = settings.charsetIsDict.contains(true);
        
        Utils.out.println(5, "Dictionary charsets present: "+hasDictCharset);

        if (hasDictCharset) {

            long wordsUsed = 0;
            
            isDictIndex = new boolean[settings.charsets.size()];
            for (int i = 0; i < isDictIndex.length; i++) {
                
                if (isDictIndex[i] = settings.charsetIsDict.get(i)) {
                    
                    wordsUsed += settings.charsets.get(i).length;
                }
            }

            Utils.out.println(5, "Dictionary words used: "+FORMATTER_INT.format(wordsUsed));
        }

        BruteCommon task = getBrute(settings.minChars, settings.maxChars);
        
        status.totalCombinations = task.getVariantsTotal();
        Utils.out.println(5, "Total combinations: "+FORMATTER_INT.format(status.totalCombinations)+" ("+Utils.describeHashTotal(status.totalCombinations, HASH_SWITCH)+")");
        
        List<BruteCommon> tasks = BruteMultithread.divideTask(task, settings.threadsNum);
        
        BruteThread[] threads = new BruteThread[settings.threadsNum];

        if (hasDictCharset) 
            for (int i = 0; i < threads.length; i++) threads[i] = new BruteThreadDict(settings, tasks.get(i), hashBytes, isDictIndex);
            else for (int i = 0; i < threads.length; i++) threads[i] = new BruteThread(settings, tasks.get(i), hashBytes);
        
        return processThreads(threads);
    }
    
    private boolean processPerformance() throws Exception {
        
        if (settings.runTime <= 0) {
            
            Utils.out.println("In performance mode you should set time!");
            return false;
        }

        Utils.out.println("Estimating max hash performance with settings: ");
        Utils.out.println(5, "Algorithm: "+settings.algorithm);
        Utils.out.println(5, "CPU threads used: "+settings.threadsNum);
        Utils.out.println(5, "Run time: "+settings.runTime+" seconds");
        
        int chars = 6;
        
        settings.charsets = new ArrayList<>(chars);
        
        for (int i = 0; i < chars; i++) settings.charsets.add(getCharsetByCode('l'));
        
        BruteCommon task = getBrute(chars, chars);
        
        List<BruteCommon> tasks = BruteMultithread.divideTask(task, settings.threadsNum);
        
        BruteThread[] threads = new BruteThread[settings.threadsNum];
        
        for (int i = 0; i < threads.length; i++) threads[i] = new BruteThreadPerf(settings, tasks.get(i));
        
        return processThreads(threads);
    }
    
    public boolean run() throws Exception {
        
        if (!init()) return false;
        
        switch (command) {
            
            case CRACK              : return processCrack();
            case PERFORMANCE        : return processPerformance();
            default : return false;
        }
    }
    
    private void defineUserCharset(int index, String charset) throws Exception {
        
        if (charset.length() == 0) {
            
            Utils.out.println("Invalid user charset @ index = "+index);
            return;
        }
        
        settings.userCharsets[index] = Utils.getCharArr(charset);
    }
    
    private void defineUserDictionary(int index, String path) throws Exception {

        List<byte[]> dict = DictUtils.loadDictionary(path);
        
        if (dict == null || dict.isEmpty()) throw new Exception("Dictionary ["+path+"] not loaded or empty!");
        
        settings.userCharsets[index] = dict.toArray();
        settings.userCharsetIsDict[index] = true;
    }
    
    public void processArg(String arg) throws Exception {
        
        String argHash = "hash=";
        String argAlgo = "algo=";
        String argThreads = "threads=";
        String argTime = "time=";
        String argStatus = "status=";
        String argMin = "min=";
        String argMax = "max=";
        String argMask = "mask=";
        
        for (int i = 0; i < 10; i++) {
            
            String s = String.valueOf(i);
            
            if (arg.startsWith(s+"=")) {
                
                defineUserCharset(i, arg.substring(2));
                return;
            } else if (arg.startsWith(s+"d=")) {
                
                defineUserDictionary(i, arg.substring(3));
                return;
            }
        }
        
        if (arg.startsWith(argHash)) settings.hash = arg.substring(argHash.length()); else
        if (arg.startsWith(argAlgo)) settings.algorithm = arg.substring(argAlgo.length()); else
        if (arg.startsWith(argThreads)) settings.threadsNum = Integer.parseInt(arg.substring(argThreads.length())); else
        if (arg.startsWith(argTime)) settings.runTime = Integer.parseInt(arg.substring(argTime.length())); else
        if (arg.startsWith(argStatus)) settings.statusTime = Integer.parseInt(arg.substring(argStatus.length())); else
        if (arg.startsWith(argMin)) settings.minChars = Integer.parseInt(arg.substring(argMin.length())); else
        if (arg.startsWith(argMax)) settings.maxChars = Integer.parseInt(arg.substring(argMax.length())); else
        if (arg.startsWith(argMask)) settings.mask = arg.substring(argMask.length()); else
        Utils.out.println("Unknown argument: "+arg);
    }

    public void processArgs(String[] args, int startIndex) throws Exception {
        
        for (int i = startIndex; i < args.length; i++) processArg(args[i]);
    }

    public void setCommand(Command command) {
        this.command = command;
    }

    public void setHash(String hash) {
        settings.hash = hash;
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
        
        Utils.out.println("Usage: hashcrack command <params> [options]");
        Utils.out.println("Commands:");
        Utils.out.println( 5, "crack - crack specified hash");
        Utils.out.println(10, "Params: <hash>");
        Utils.out.println(10, "Available options:");
        Utils.out.println(15, "algo=algorithm - specify hash algorithm (default: "+Settings.DEFAULT_ALGORITHM+")");
        Utils.out.println(15, "mask=mask - specify mask for brute force attack (default: "+Settings.DEFAULT_MASK+")");
        Utils.out.println(15, "N=charset - specify user defined charset for usage in mask, where N (0..9)");
        Utils.out.println(15, "Nd=path_to_dictionary_file - specify user defined dictionary file for mask, where N (0..9)");
        Utils.out.println(15, "min=N - minimum number of chars(words) for brute force attack (default: "+(Settings.DEFAULT_MIN_CHARS==null?"up to mask":Settings.DEFAULT_MIN_CHARS)+")");
        Utils.out.println(15, "max=N - maximum number of chars(words) for brute force attack (default: "+(Settings.DEFAULT_MAX_CHARS==null?"up to mask":Settings.DEFAULT_MAX_CHARS)+")");
        Utils.out.println(15, "threads=N - number of threads to create (default: "+Settings.DEFAULT_THREAD_NUM+")");
        Utils.out.println(15, "time=N - max run time N seconds (default: "+(Settings.DEFAULT_RUN_TIME==-1?"infinite":Settings.DEFAULT_RUN_TIME)+")");
        Utils.out.println(15, "status=N - print status every N seconds (default: "+(Settings.DEFAULT_STATUS_TIME==-1?"not used":Settings.DEFAULT_STATUS_TIME)+")");
        Utils.out.println( 5, "perf - test max performance on specified algorithm");
        Utils.out.println(10, "Available options:");
        Utils.out.println(15, "algo=algorithm - specify hash algorithm (default: "+Settings.DEFAULT_ALGORITHM+")");
        Utils.out.println(15, "threads=N - number of threads to create (default: "+Settings.DEFAULT_THREAD_NUM+")");
        Utils.out.println(15, "time=N - run for N seconds (default: "+(Settings.DEFAULT_RUN_TIME==-1?"undefined":Settings.DEFAULT_RUN_TIME)+")");
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
            
            HashCrack tool = new HashCrack();

            switch (args[0].toLowerCase()) {
                
                case "crack"  : 
                    tool.setCommand(Command.CRACK); 
                    if (!Utils.checkArgs(args, 2)) return;
                    tool.setHash(args[1]);
                    tool.processArgs(args, 2);
                    break;
                case "perf"  : 
                case "performance"  : 
                    tool.setCommand(Command.PERFORMANCE); 
                    tool.processArgs(args, 1);
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