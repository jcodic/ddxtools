package ddx.tools;

import ddx.common.Const;
import ddx.common.Utils;

/**
 *
 * @author druidman
 */
public class PerformanceTest {

    public static enum Command {RUN};
    
    private Command command;
    private final Settings settings = new Settings();
    
    private String getPossibleFileSizes() throws Exception {
        String s = "";
        for (int size : ProcessThread.POSSIBLE_FILE_SIZES) s += (s.length()==0?"":", ")+size/ProcessThread.MB;
        return s;
    }
    
    private boolean processPerformanceTest() throws Exception {
    
        if (settings.threadsNum < 1) {
            
            Utils.out.println("Wrong threads number ["+settings.threadsNum+"]");
            return false;
        }
        
        int c = 1;
        Utils.out.println("Running performance test with following settings:");
        Utils.out.println(5, "Number of threads: "+settings.threadsNum);
        Utils.out.println(5, "Run for: "+settings.runTime+" seconds");
        Utils.out.println("Processing task has following stages (in loop):");
        Utils.out.println(5, String.valueOf(c++)+". Generate buffer (virtual file) with random length of ["+getPossibleFileSizes()+"] Mb.");
        if (settings.fillBufferWithRandom)
            Utils.out.println(5, String.valueOf(c++)+". Fill buffer with random numbers");
        if (settings.compressionEnabled)
            Utils.out.println(5, String.valueOf(c++)+". Put buffer in ZIP archive (virtual) with compression level "+ProcessThread.DEFLATE_LEVEL); else
            Utils.out.println(5, String.valueOf(c++)+". Put buffer in ZIP archive (virtual) with no compression");
        if (settings.encryptionEnabled)
            Utils.out.println(5, String.valueOf(c++)+". Encrypt ZIP archive with "+Settings.DEFAULT_CRYPT_ALGORITHM);
        Utils.out.println("Running, please wait "+settings.runTime+" seconds...");

        ProcessThread[] threads = new ProcessThread[settings.threadsNum];
        
        for (int i = 0; i < settings.threadsNum; i++) {
            
            threads[i] = new ProcessThread(settings);
            threads[i].start();
        }
        
        Thread.sleep(settings.runTime * 1_000L);
        
        for (ProcessThread thread : threads) thread.doStop();
        
        boolean allCompleted = false;
        
        while (!allCompleted) {
            
            allCompleted = true;

            for (ProcessThread thread : threads) {

                if (!thread.isCompleted()) {
                    
                    allCompleted = false;
                    break;
                }
            }
            
            Thread.sleep(500L);
        }
        
        // collect stats
        long totalProcessed = 0; // in bytes
        long totalWritten = 0; // in bytes
        long totalProcessingSpeed = 0; // in bytes per second
        
        for (int i = 0; i < settings.threadsNum; i++) {
            
            totalProcessed += threads[i].getTotalProcessed();
            totalWritten += threads[i].getTotalWritten();
            totalProcessingSpeed += threads[i].getProcessingSpeed();
        }
        
        long avgProcessingSpeed = totalProcessingSpeed / settings.threadsNum;
        
        Utils.out.println("Total processed: "+Utils.describeFileLength(totalProcessed));
        Utils.out.println("Total written (virtually): "+Utils.describeFileLength(totalWritten));
        Utils.out.println("Processing speed: "+Utils.describeFileLength(totalProcessingSpeed)+" per second.");
        if (settings.threadsNum > 1) 
            Utils.out.println("Average processing speed per thread: "+Utils.describeFileLength(avgProcessingSpeed)+" per second.");
        
        return true;
    }

    public boolean run() throws Exception {
        
        switch (command) {
            
            case RUN                : return processPerformanceTest();
            default                 : return false;
        }
    }
    
    public void processArg(String arg) {
        
        String argThreads = "threads=";
        String argTime = "time=";
        String argFillBuffer = "fill=";
        String argCompression = "compress=";
        String argEncryption = "encrypt=";
        if (arg.startsWith(argThreads)) settings.threadsNum = Integer.parseInt(arg.substring(argThreads.length())); else
        if (arg.startsWith(argTime)) settings.runTime = Integer.parseInt(arg.substring(argTime.length())); else
        if (arg.startsWith(argFillBuffer)) settings.fillBufferWithRandom = Boolean.parseBoolean(arg.substring(argFillBuffer.length())); else
        if (arg.startsWith(argCompression)) settings.compressionEnabled = Boolean.parseBoolean(arg.substring(argCompression.length())); else
        if (arg.startsWith(argEncryption)) settings.encryptionEnabled = Boolean.parseBoolean(arg.substring(argEncryption.length())); else
        Utils.out.println("Unknown argument: "+arg);
    }

    public void processArgs(String[] args, int startIndex) {
        
        for (int i = startIndex; i < args.length; i++) processArg(args[i]);
    }

    public void setCommand(Command command) {
        this.command = command;
    }

    public static void printInfo() {
        
        Utils.out.println("Usage: performance command [options]");
        Utils.out.println("Commands:");
        Utils.out.println( 5, "run - run performance test");
        Utils.out.println(10, "Available options:");
        Utils.out.println(15, "threads=N - number of threads to create (default: "+Settings.DEFAULT_THREAD_NUM+")");
        Utils.out.println(15, "time=N - run performance test for N seconds (default: "+Settings.DEFAULT_RUN_TIME+")");
        Utils.out.println(15, "fill=true/false - fill generated buffer with random numbers (default: "+Settings.DEFAULT_FILL_BUFFER+")");
        Utils.out.println(15, "compress=true/false - compress generated buffer after filling with random numbers (default: "+Settings.DEFAULT_COMPRESSION_ENABLED+")");
        Utils.out.println(15, "encrypt=true/false - encrypt generated buffer after compression (default: "+Settings.DEFAULT_ENCRYPTION_ENABLED+")");
    }

    public static void main(String[] args) {
        
        try {

            if (args == null || args.length == 0) {
                
                printInfo();
                return;
            }
            
            PerformanceTest tool = new PerformanceTest();
            
            switch (args[0].toLowerCase()) {
                
                case "run"      : 
                    tool.setCommand(Command.RUN); 
                    tool.processArgs(args, 1);
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
