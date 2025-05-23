package ddx.hash;

import ddx.common.CommonSettigns;

/**
 *
 * @author druidman
 */
public class Settings extends CommonSettigns {

    public static final String DEFAULT_ALGORITHM = "SHA-256";
    public static final String DEFAULT_HASH_FILE = "checksum.hash";
    public static final boolean DEFAULT_HASH_NAMES = false;
    
    public String hashFile = DEFAULT_HASH_FILE;
    public String hashFile2;
    public String hashFile3;
    public String algorithm = DEFAULT_ALGORITHM;
    public String sourcePath;
    public String hash;
    public String name;
    public String csvFile;
    public String csvFileFrom;
    public boolean hashNames = DEFAULT_HASH_NAMES;
    public boolean checkSuccess;
    public boolean showFiles = true;
    public boolean showFilesFound = true;
    public boolean showFilesMissed = true;

    public String sourceString;
    
    public boolean copyResultToClipboard = false;
}