package ddx.hash.types;

import java.util.Arrays;

/**
 *
 * @author stabi
 */
public class FileHash {

    private String name;
    private long size;
    private byte[] hash;
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public byte[] getHash() {
        return hash;
    }

    public void setHash(byte[] hash) {
        this.hash = hash;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + (int) (this.size ^ (this.size >>> 32));
        hash = 97 * hash + Arrays.hashCode(this.hash);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final FileHash other = (FileHash) obj;
        if (this.size != other.size) {
            return false;
        }
        if (!Arrays.equals(this.hash, other.hash)) {
            return false;
        }
        return true;
    }

}