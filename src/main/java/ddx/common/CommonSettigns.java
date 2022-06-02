package ddx.common;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author druidman
 */
public class CommonSettigns {
 
    public static final String DEFAULT_CHARSET = "UTF-8";

    public List<String> includeFiles;
    public List<String> excludeFiles;

    public int filesScanned;
    public int filesIncluded;
    public long filesSizeFound;
    public long filesSizeIncluded;
    public long filesSizeProcessed;
    
    public String charset = DEFAULT_CHARSET;

    public void addToInclude(String filter) {
        
        if (includeFiles == null) includeFiles = new LinkedList<>();
        includeFiles.add(filter);
    }

    public void addToInclude(List<String> filters) {
        
        if (includeFiles == null) includeFiles = new LinkedList<>();
        includeFiles.addAll(filters);
    }

    public void addToExclude(String filter) {
        
        if (excludeFiles == null) excludeFiles = new LinkedList<>();
        excludeFiles.add(filter);
    }
    
    public void addToExclude(List<String> filters) {
        
        if (excludeFiles == null) excludeFiles = new LinkedList<>();
        excludeFiles.addAll(filters);
    }

}