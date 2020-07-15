package ddx.common;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author druidman
 */
public class CommonSettigns {
 
    public List<String> includeFiles;
    public List<String> excludeFiles;

    public int filesScanned;
    public int filesIncluded;
    public long filesSizeFound;
    public long filesSizeIncluded;
    public long filesSizeProcessed;
    
    public void addToInclude(String filter) {
        
        if (includeFiles == null) includeFiles = new LinkedList<>();
        includeFiles.add(filter);
    }

    public void addToExclude(String filter) {
        
        if (excludeFiles == null) excludeFiles = new LinkedList<>();
        excludeFiles.add(filter);
    }
    
}