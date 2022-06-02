package ddx.matches;

import java.io.File;

/**
 *
 * @author druidman
 */
public class HFile {

    public final File file;
    public final long length;
    public final String path;
    public final String name;
    public byte[] hashCache;
    private final int hashCode;
    public boolean deleted = false;

    public HFile(File file) {
        this.file = file;
        this.length = file.length();
        this.path = file.getPath();
        this.name = file.getName();
        this.hashCache = null; // file content hash code
        this.hashCode = file.hashCode();
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        final HFile other = (HFile) obj;
        if (this.hashCode != other.hashCode) return false;
        return this.path.equals(other.path);
    }

}