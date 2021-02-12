package ddx.common;

/**
 *
 * @author druidman
 */
public class EmptyThread extends Thread {
    
    protected boolean completed;
    protected volatile boolean stopped;
    
    public synchronized boolean isStopped() {
        return stopped;
    }
    
    public synchronized boolean isCompleted() {
        return completed;
    }
    
    public synchronized void doStop() {
        stopped = true;
    }

    public synchronized void doComplete() {
        stopped = true;
        completed = true;
    }
    
}