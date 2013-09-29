package portcomms;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import serialComms.SerialPortCom;

/**
 * Choose a port, any port!
 *
 * Java Communications is a "standard extension" and must be downloaded
 * and installed separately from the JDK before you can even compile this 
 * program.
 *
 */
// USED RXTX instead!!!!
public class PortChooser extends JDialog implements ItemListener {
    /** A mapping from names to CommPortIdentifiers. */
    protected HashMap map = new HashMap(  );
    /** The name of the choice the user made. */
    protected String selectedPortName;
    /** The CommPortIdentifier the user chose. */
    protected CommPortIdentifier selectedPortIdentifier;
    /** The JComboBox for serial ports */
    protected JComboBox serialPortsChoice;
    
    protected SerialPort ttya;
    /** To display the chosen */
    protected JLabel choice;
    /** Padding in the GUI */
    protected final int PAD = 5;

    /** This will be called from either of the JComboBoxes when the
     * user selects any given item.
     */
    public void itemStateChanged(ItemEvent e) {
        // Get the name
        selectedPortName = (String)((JComboBox)e.getSource()).getSelectedItem(  );
        // Get the given CommPortIdentifier
        selectedPortIdentifier = (CommPortIdentifier)map.get(selectedPortName);
        // Display the name.
        choice.setText(selectedPortName);
    }

    /* The public "getter" to retrieve the chosen port by name. */
    public String getSelectedName() {
        return selectedPortName;
    }

    /* The public "getter" to retrieve the selection by CommPortIdentifier. */
    public CommPortIdentifier getSelectedIdentifier(  ) {
        return selectedPortIdentifier;
    }

    /** A test program to show up this chooser. */
    public static void main(String[] ap) {
        PortChooser c = new PortChooser(null);
        c.setVisible(true);    // blocking wait
        System.out.println("You chose " + c.getSelectedName(  ) +
            " (known by " + c.getSelectedIdentifier(  ) + ").");
        System.exit(0);
    }

    /** Construct a PortChooser --make the GUI and populate the ComboBoxes.
     */
    public PortChooser(JFrame parent) {
        super(parent, "Port Chooser", true);

        makeGUI(  );
        populate(  );
        finishGUI(  );
    }

    /** Build the GUI. You can ignore this for now if you have not
     * yet worked through the GUI chapter. Your mileage may vary.
     */
    protected void makeGUI(  ) {
        Container cp = getContentPane(  );

        JPanel centerPanel = new JPanel(  );
        cp.add(BorderLayout.CENTER, centerPanel);

        centerPanel.setLayout(new GridLayout(0,2, PAD, PAD));

        centerPanel.add(new JLabel("Serial Ports", SwingConstants.RIGHT));
        serialPortsChoice = new JComboBox(  );
        centerPanel.add(serialPortsChoice);
        serialPortsChoice.setEnabled(false);

       

        

        centerPanel.add(new JLabel("Your choice:", SwingConstants.RIGHT));
        centerPanel.add(choice = new JLabel(  ));

        JButton okButton;
        cp.add(BorderLayout.SOUTH, okButton = new JButton("OK"));
        okButton.addActionListener(new ActionListener(  ) {
            public void actionPerformed(ActionEvent e) {
                PortChooser.this.dispose(  );
            }
        });

    }

    /** Populate the ComboBoxes by asking the Java Communications API
     * what ports it has.  Since the initial information comes from
     * a Properties file, it may not exactly reflect your hardware.
     */
    protected void populate(  ) {
        // get list of ports available on this particular computer,
        // by calling static method in CommPortIdentifier.
    	ArrayList<CommPortIdentifier> commPortIds = SerialPortCom.getPortArrayList();
    	for (int i = 0; i < commPortIds.size(); i++) {
    		serialPortsChoice.addItem(commPortIds.get(i).getName());
    	}
    	serialPortsChoice.setEnabled(true);
    	
    }

    
     public static CommPortIdentifier commPortNameToID(String portName) {
    	CommPortIdentifier commPortId = null;
    	ArrayList<CommPortIdentifier> portIds = SerialPortCom.getPortArrayList();
    	for (int i = 0; i < portIds.size(); i++) {
    		if (portIds.get(i).getName().equals(portName)) {
    			return portIds.get(i);
    		}
    	}
    	return null;
    }
    
    
    
    protected void finishGUI(  ) {
        serialPortsChoice.addItemListener(this);
        pack(  );
      //  addWindowListener(new WindowCloser(this, true));
    }
}