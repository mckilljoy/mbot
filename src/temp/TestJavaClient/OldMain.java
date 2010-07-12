/*
 * Main.java
 *
 */
package TestJavaClient;

import scc.*;

// Java imports

import java.awt.Component;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;


public class OldMain {

    // This method is called to start the application
    public static void main ( SimpleCassandraClient scc ) {
        SampleFrame sampleFrame = new SampleFrame();
        sampleFrame.cassandraClient = scc;
        sampleFrame.setVisible(true);
    }

    static public void inform( final Component parent, final String str) {
        if( SwingUtilities.isEventDispatchThread() ) {
        	showMsg( parent, str, JOptionPane.INFORMATION_MESSAGE);
        }
        else {
            SwingUtilities.invokeLater( new Runnable() {
				public void run() {
					showMsg( parent, str, JOptionPane.INFORMATION_MESSAGE);
				}
			});
        }
    }

    static private void showMsg( Component parent, String str, int type) {    	
        // this function pops up a dlg box displaying a message
        JOptionPane.showMessageDialog( parent, str, "IB Java Test Client", type);
    }
}