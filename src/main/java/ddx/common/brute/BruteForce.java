package ddx.common.brute;

/**
 *
 * @author druidman
 */
public interface BruteForce {
    
    public static final int NOT_INIT_VALUE = -1;
    
    public void init();
    public void reset();
    public void reset(int nfirst);
    public boolean hasNext();
    public boolean next();
    public int getLength();
    public long getVariantsTotal();
    public long getVariantsLeft();
    public long getVariantsProcessed();
    public String[] getArgs();
    public Object[] getCurrent();
    public BruteForce getClone();
    
}