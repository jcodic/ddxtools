package ddx.common;

import java.util.Arrays;

/**
 *
 * @author druidman
 */
public class PrinterConsole implements Printer {
 
    private boolean printAllowed = true;

    public boolean isPrintAllowed() {
        return printAllowed;
    }

    public void setPrintAllowed(boolean printAllowed) {
        this.printAllowed = printAllowed;
    }
    
    @Override
    public void println() {
    
        println("");
    }
    
    @Override
    public void println(String str) {
        
        print(str + "\n");
    }

    @Override
    public void print(String str) {
    
        print(0, str);
    }

    @Override
    public void println(int space, String str) {
    
        print(space, str + "\n");
    }
    
    @Override
    public void print(int space, String str) {
        
        String s = null;
        if (space > 0) {
            
            char[] bf = new char[space];
            Arrays.fill(bf, ' ');
            s = new String(bf) + str;
        } else s = str;
        
        if (printAllowed) System.out.print(s);
    }
    
}