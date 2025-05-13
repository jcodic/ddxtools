package ddx.hash.types;

import java.util.Set;

/**
 *
 * @author stabi
 */
public class HashIndexFile {

    public static final byte PREFIX = 1; // start byte of index file
    private byte[] checkHash;
    private Set<FileHash> hashes;

    public int getLength() {
        return hashes==null?0:hashes.size();
    }

    public byte[] getCheckHash() {
        return checkHash;
    }

    public void setCheckHash(byte[] checkHash) {
        this.checkHash = checkHash;
    }

    public Set<FileHash> getHashes() {
        return hashes;
    }

    public void setHashes(Set<FileHash> hashes) {
        this.hashes = hashes;
    }

}