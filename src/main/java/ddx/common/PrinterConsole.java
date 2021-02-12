package ddx.common;

import java.util.Arrays;

/**
 *
 * @author druidman
 */
public class PrinterConsole implements Printer {
 
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
        
        System.out.print(s);
    }
    
}