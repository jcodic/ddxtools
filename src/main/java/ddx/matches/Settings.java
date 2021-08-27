package ddx.matches;

import ddx.common.CommonSettigns;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author druidman
 */
public class Settings extends CommonSettigns {

    public static enum ContentCompare {BYTES, HASH};
    
    public static final boolean DEFAULT_SAME_NAME = true;
    public static final boolean DEFAULT_SAME_SIZE = true;
    public static final boolean DEFAULT_SAME_CONTENT = false;
    public static final boolean DEFAULT_SHOW_MATCHES_ONLY = false;
    public static final String DEFAULT_HASH_ALGORITHM = "SHA-256";
    public static final ContentCompare DEFAULT_CONTENT_COMPARE = ContentCompare.HASH;
    
    public List<String> paths = new ArrayList<>(2);
    public boolean sameName = DEFAULT_SAME_NAME;
    public boolean sameSize = DEFAULT_SAME_SIZE;
    public boolean sameContent = DEFAULT_SAME_CONTENT;
    public boolean showMatchesOnly = DEFAULT_SHOW_MATCHES_ONLY;
    public boolean exportEnabled = false;
    public String exportFile;
    public List<Combo> exportList;
    public boolean maxInfo = false;
    public long sameContentMinSize = -1;
    public ContentCompare contentCompare = DEFAULT_CONTENT_COMPARE;
    
    
    
    public static ContentCompare getContentCompare(String s) {
    
        if (s == null || s.length() == 0) return DEFAULT_CONTENT_COMPARE;
        
        switch (s.toLowerCase()) {
            
            case "bytes" : return ContentCompare.BYTES;
            case "hash" : return ContentCompare.HASH;
            default : return DEFAULT_CONTENT_COMPARE;
        }
    }
    
}