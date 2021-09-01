package ddx.matches;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author druidman
 */
public class Combo {
    
    public HFile file;
    public List<HFile> duplicates;

    public Combo(HFile file, Collection<HFile> sameFiles) {
        this.file = file;
        this.duplicates = new ArrayList<>(sameFiles.size());
        this.duplicates.addAll(sameFiles);
    }
 
}