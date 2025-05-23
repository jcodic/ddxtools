package ddx.hash.types;

/**
 *
 * @author stabi
 */
public class SimpleFile {

    private String name;
    private long size;

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

    public SimpleFile() {
    }

    public SimpleFile(String name, long size) {
        this.name = name;
        this.size = size;
    }

}
