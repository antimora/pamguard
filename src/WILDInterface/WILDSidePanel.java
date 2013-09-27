/*
 *  PAMGUARD - Passive Acoustic Monitoring GUARDianship.
 * To assist in the Detection Classification and Localisation
 * of marine mammals (cetaceans).
 *
 * Copyright (C) 2006
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */


package WILDInterface;

import GPS.GPSDataBlock;
import GPS.GpsData;
import GPS.GpsDataUnit;
import NMEA.AcquireNmeaData;
import PamUtils.LatLong;
import PamUtils.PamCalendar;
import PamView.PamBorderPanel;
import PamView.PamColors;
import PamView.PamColors.PamColor;
import PamView.PamSidePanel;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamObserver;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

/**
 *
 * @author Michael Oswald
 */
public class WILDSidePanel implements PamSidePanel, PamObserver {

    private WILDControl wildControl;
    private WILDSerialInterface wildCom;
    private SidePanel sidePanel;
	private TitledBorder titledBorder;
	private GPSDataBlock gpsDataBlock;
    private boolean haveGPS = false;
    private boolean haveSerialOut = false;
    private GpsData gpsData = null;


    private JTextField bearing = new JTextField(15);
    private JTextField azimuth = new JTextField(15);
    private JTextField distance = new JTextField(15);
    private JTextField animalLat = new JTextField(15);
    private JTextField animalLong = new JTextField(15);
    private JTextField animalDepth = new JTextField(15);
    private JTextField species = new JTextField(15);
    private JLabel latitude, longitude, dateLbl, timeLbl, heading;
    private JButton sendData;
    private JButton toggleLoc;

    final static String BEARAZDIST = "BearingAzimuthDistance";
    final static String LATLONGDEPTH = "LatLongDepth";
    String currentLoc = BEARAZDIST;
    
    /**
     * Main constructor
     *
     */
    public WILDSidePanel(WILDControl wildControl) {
        this.wildControl = wildControl;
        this.sidePanel = new SidePanel();

        /* try to subscribe to the GPS data block */
        haveGPS = subscribeToGPS();

        /* set up the serial output connection */
        haveSerialOut = setUpSerialCom();
    }

    /**
     * Called by WILDControl after user has accessed the WILDParameters dialog.
     * Initializes important parameters.
     */
    public void prepareProcess() {
        /* try to subscribe to the GPS data block */
        haveGPS = subscribeToGPS();
        haveSerialOut = setUpSerialCom();
    }

    /**
     * subscribe to the GPS data block, if one has been selected
     */
    public boolean subscribeToGPS() {
        gpsDataBlock = wildControl.getWildParameters().getGpsSource();
        if (gpsDataBlock != null) {
            gpsDataBlock.addObserver(this);
            return true;
        } else {
            latitude.setText("No GPS data");
            longitude.setText("No GPS data");
            heading.setText("No GPS data");
            return false;
        }
    }

    public boolean setUpSerialCom() {
		closeSerialCom();
		CommPortIdentifier comPortIdentifier;
		try {
			comPortIdentifier = CommPortIdentifier.getPortIdentifier
                    (wildControl.getWildParameters().getSerialPortName());
		}
		catch (NoSuchPortException ex) {
			System.out.println(String.format("Serial port %s does not exist", 
                    wildControl.getWildParameters().getSerialPortName()));
			return false;
		}

		wildCom = new WILDSerialInterface
                (wildControl.getWildParameters().getSerialPortName(),
                wildControl.getWildParameters().getBaud(),
                comPortIdentifier,
                wildControl);
		return true;
	}

	public void closeSerialCom() {
		if (wildCom == null) {
			return;
		}
		wildCom.close();
		wildCom = null;
	}

    public String getObserverName() {
        return "WILD ArcGIS interface";
    }

    public PamObserver getObserverObject() {
        return this;
    }

    public void masterClockUpdate(long milliSeconds, long sampleNumber) {}

    public void noteNewSettings() {}

    /**
     * If the GPS Data object gets deleted, reset the sidePanel so that we
     * don't keep listening for something that isn't there
     *
     * @param o
     */
    public void removeObservable(PamObservable o) {
        dateLbl.setText("No GPS Data");
        timeLbl.setText("No GPS Data");
        latitude.setText("No GPS Data");
        longitude.setText("No GPS Data");
        heading.setText("No GPS Data");
        haveGPS = false;
        gpsData = null;
    }

    public void setSampleRate(float sampleRate, boolean notify) {}

    /** 
     * Update labels with new GPS data
     */
    public void update(PamObservable o, PamDataUnit arg) {
		PamDataBlock block = (PamDataBlock) o;

        /* if we've gotten new GPS data, get the latitude and longitude and
         * update the labels on the SidePanel
         */
		if (block.getUnitClass() == GpsDataUnit.class) {
            gpsData = ((GpsDataUnit) arg).getGpsData();

            /* update the labels in the SidePanel */
//            latitude.setText(String.format("%8.5f", gpsData.getLatitude()));
//            longitude.setText(String.format("%9.5f", gpsData.getLongitude()));
            latitude.setText(LatLong.formatLatitude(gpsData.getLatitude()));
            longitude.setText(LatLong.formatLongitude(gpsData.getLongitude()));
            dateLbl.setText(PamCalendar.formatDate(gpsData.getTimeInMillis()));
            timeLbl.setText(PamCalendar.formatTime(gpsData.getTimeInMillis()));
            
            /* copied from GpsTextDisplay.java...
             * will have to try various options to find out the heading - may be none
             * or may be true or may be magnetic.
             */
            if (gpsData.getTrueHeading() != null) {
                heading.setText(String.format("%.1f \u00B0T", gpsData.getTrueHeading()));
            }
            else if (gpsData.getMagneticHeading() != null) {
                heading.setText(String.format("%.1f \u00B0M", gpsData.getMagneticHeading()));
            }
            else {
                heading.setText("-");
            }
        }
    }

    public JComponent getPanel() {
        return sidePanel;
    }

	public void rename(String newName) {
		titledBorder.setTitle(newName);
		sidePanel.repaint();
    }

    @Override
    public long getRequiredDataHistory(PamObservable o, Object arg) {
		return 60000;
    }

    /**
     * Inner class containing the panel components
     */
	public class SidePanel extends PamBorderPanel implements FocusListener {

        GPSDataPanel gpsDataPanel;
        JPanel animalLocPanel;

		public SidePanel() {
			super();
			setBorder(titledBorder = new TitledBorder(wildControl.getUnitName()));
			titledBorder.setTitleColor(PamColors.getInstance().getColor(PamColor.AXIS));
            gpsDataPanel = new GPSDataPanel();

            /* use the GridBagLayout manager */
            GridBagLayout gb = new GridBagLayout();
			setLayout(gb);
            drawThePanel();
		}


		@Override
		public void setBackground(Color bg) {
			super.setBackground(bg);
			if (titledBorder != null) {
				titledBorder.setTitleColor(PamColors.getInstance().getColor(PamColor.AXIS));
			}
		}

        public class GPSDataPanel extends PamBorderPanel {

            TitledBorder gpsTitle;

            public GPSDataPanel() {
                super();
                setBorder(gpsTitle = new TitledBorder("GPS Data"));
                gpsTitle.setTitleColor(PamColors.getInstance().getColor(PamColor.AXIS));
                GridBagLayout gb = new GridBagLayout();
                setLayout(gb);
                GridBagConstraints c = new GridBagConstraints();
                c.anchor = GridBagConstraints.PAGE_START;

                /* add the GPS-related data in a subpanel */
                c.gridx = c.gridy = 0;
                c.gridwidth = 2;
                c.insets = new Insets(0,0,0,0);
                addComponent(this, dateLbl = new JLabel("No GPS Data"), c);
                c.gridy++;
                addComponent(this, timeLbl = new JLabel(), c);
                c.gridwidth=1;
                c.gridy++;
                c.insets = new Insets(5,0,0,0);
                addComponent(this, new JLabel("Lat: "), c);
                c.gridx++;
                addComponent(this, latitude = new JLabel("No GPS Data"), c);
                c.gridx=0;
                c.gridy++;
                c.insets = new Insets(0,0,0,0);
                addComponent(this, new JLabel("Long: "), c);
                c.gridx++;
                addComponent(this, longitude = new JLabel("No GPS Data"), c);
                c.gridx=0;
                c.gridy++;
                c.insets = new Insets(0,0,0,0);
                addComponent(this, new JLabel("Heading: "), c);
                c.gridx++;
                addComponent(this, heading = new JLabel("No GPS Data"), c);
            }
        }

        /**
         * Add components to the SidePanel
         */
        public void drawThePanel() {

            /* set the constraints */
			GridBagConstraints c = new GridBagConstraints();
			c.anchor = GridBagConstraints.PAGE_START;
            c.fill = GridBagConstraints.HORIZONTAL;

            /* add the GPS subpanel to the SidePanel */
            c.gridx = c.gridy = 0;
            c.gridwidth = 1;
            c.insets = new Insets(0,0,0,0);
            addComponent(this, gpsDataPanel,c);

            /* create the first animal location panel, with bearing/azimuth/distance */
            JPanel set1 = new JPanel();
            set1.setBackground(this.getBackground());
            GridBagLayout gb = new GridBagLayout();
            set1.setLayout(gb);

            /* add the bearing angle text field, and register an actionlistener
             * to check the format and a focus listener to watch for tabs */
            c.gridwidth = 1;
            c.insets = new Insets(0,0,0,0);
            c.gridx=0;
            c.gridy++;
//			addComponent(this, new JLabel("Bearing Angle (degrees)"), c);
            set1.add(new JLabel("Bearing Angle (degrees)", JLabel.CENTER),c);
            c.insets = new Insets(0,0,0,0);
			c.gridy ++;
//            addComponent(this, bearing, c);
            set1.add(bearing,c);
            bearing.setEditable(true);
            bearing.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    bearingHitEnter();
                }
            });
            bearing.addFocusListener(this);

            /* add the azimuth text field, and register an actionlistener
             * to check the format and a focus listener to watch for tabs */
            c.insets = new Insets(5,0,0,0);
            c.gridy++;
//			addComponent(this, new JLabel("Azimuth (degrees)"), c);
			set1.add(new JLabel("Azimuth (degrees)", JLabel.CENTER), c);
            c.insets = new Insets(0,0,0,0);
			c.gridy ++;
//            addComponent(this, azimuth, c);
            set1.add(azimuth,c);
            azimuth.setEditable(true);
            azimuth.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    azimuthHitEnter();
                }
            });
            azimuth.addFocusListener(this);

            /* add the distance text field, and register an actionlistener
             * to check the format and a focus listener to watch for tabs */
            c.gridy++;
            c.insets = new Insets(5,0,0,0);
//			addComponent(this, new JLabel("Distance (m)"), c);
			set1.add(new JLabel("Distance (m)", JLabel.CENTER), c);
            c.insets = new Insets(0,0,0,0);
			c.gridy ++;
//            addComponent(this, distance, c);
            set1.add(distance,c);
            distance.setEditable(true);
            distance.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    distanceHitEnter();
                }
            });
            distance.addFocusListener(this);

            /* create the second animal location panel, with lat/long/depth */
            JPanel set2 = new JPanel();
            set2.setBackground(this.getBackground());
            GridBagLayout gb2 = new GridBagLayout();
            set2.setLayout(gb2);

            /* add the animal latitude text field, and register an actionlistener
             * to check the format and a focus listener to watch for tabs */
            c.insets = new Insets(0,0,0,0);
            c.gridy++;
            set2.add(new JLabel("Animal Latitude (degrees)", JLabel.CENTER),c);
            c.insets = new Insets(0,0,0,0);
			c.gridy ++;
            set2.add(animalLat,c);
            animalLat.setEditable(true);
            animalLat.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    animalLatHitEnter();
                }
            });
            animalLat.addFocusListener(this);

            /* add the animal longitude text field, and register an actionlistener
             * to check the format and a focus listener to watch for tabs */
            c.insets = new Insets(5,0,0,0);
            c.gridy++;
			set2.add(new JLabel("Animal Longitude (degrees)", JLabel.CENTER), c);
            c.insets = new Insets(0,0,0,0);
			c.gridy ++;
            set2.add(animalLong,c);
            animalLong.setEditable(true);
            animalLong.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    animalLongHitEnter();
                }
            });
            animalLong.addFocusListener(this);

            /* add the animal depth text field, and register an actionlistener
             * to check the format and a focus listener to watch for tabs */
            c.gridy++;
            c.insets = new Insets(5,0,0,0);
			set2.add(new JLabel("Animal Depth (m)", JLabel.CENTER), c);
            c.insets = new Insets(0,0,0,0);
			c.gridy ++;
            set2.add(animalDepth,c);
            animalDepth.setEditable(true);
            animalDepth.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    animalDepthHitEnter();
                }
            });
            animalDepth.addFocusListener(this);

            animalLocPanel = new JPanel(new CardLayout());
            animalLocPanel.add(set1, BEARAZDIST);
            animalLocPanel.add(set2, LATLONGDEPTH);

            /* add the species location panel to the sidebar */
            c.insets = new Insets(10,0,0,0);
            c.gridx = 0;
            c.gridy++;
            c.gridwidth = 1;
            addComponent(this, animalLocPanel,c);

            /* add the species ID */
            c.gridy++;
            c.insets = new Insets(5,0,0,0);
			addComponent(this, new JLabel("Species ID", JLabel.CENTER), c);
            c.insets = new Insets(0,5,0,0);
			c.gridy ++;
            addComponent(this, species, c);
            distance.setEditable(true);
            c.gridy++;

            /* create a toggle button to select which method of animal location
             * to use
             */
            toggleLoc = new JButton("Switch Parameters");
            toggleLoc.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    toggleParams();
                }
            });
            c.gridwidth = 1;
            c.insets = new Insets(15,0,0,0);
            c.gridx = 0;
            c.gridy++;
            addComponent(this,toggleLoc,c);
            toggleLoc.setBackground(this.getBackground());

            /* add the send data button */
            c.insets = new Insets(5,0,0,0);
            c.gridy++;
            sendData = new JButton("Send Data");
            addComponent(this, sendData, c);
            sendData.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    sendDataString();
                }
            });

            /* if sidePanel already exists, clean up the display */
            if (sidePanel != null) {
                sidePanel.revalidate();
                sidePanel.repaint();
            }
        }
        
        private void clearThePanel() {
            bearing.setText("");
            azimuth.setText("");
            distance.setText("");
            animalLat.setText("");
            animalLong.setText("");
            animalDepth.setText("");
            species.setText("");
        }

        /** user hit enter in the bearing textfield - call transferFocus method
         * to move the cursor to the next component and check the format
         */
        private void bearingHitEnter() {
            bearing.transferFocus();
        }
        
        /** user hit enter in the bearing textfield - call transferFocus method
         * to move the cursor to the next component and check the format
         */
        private void azimuthHitEnter() {
            azimuth.transferFocus();
        }

        /** user hit enter in the distance textfield - call transferFocus method
         * to move the cursor to the next component and check the format
         */
        private void distanceHitEnter() {
            distance.transferFocus();
        }

        /** user hit enter in the distance textfield - call transferFocus method
         * to move the cursor to the next component and check the format
         */
        private void animalLatHitEnter() {
            animalLat.transferFocus();
        }

        /** user hit enter in the distance textfield - call transferFocus method
         * to move the cursor to the next component and check the format
         */
        private void animalLongHitEnter() {
            animalLong.transferFocus();
        }

        /** user hit enter in the distance textfield - call transferFocus method
         * to move the cursor to the next component and check the format
         */
        private void animalDepthHitEnter() {
            animalDepth.transferFocus();
        }

        public void focusGained(FocusEvent e) {}

        /** component lost focus - check the format */
        public void focusLost(FocusEvent e) {
            if (e.getComponent() == bearing) {
                checkAngleFullCircle(bearing, 0, 359.99);
            } else if (e.getComponent() == azimuth) {
                checkAngleFullCircle(azimuth, -90.00, 90.00);
            } else if (e.getComponent() == distance) {
                checkDistanceFormat(distance);
            } else if (e.getComponent() == animalLat) {
                checkAngleFullCircle(animalLat, -90.00, 90.00);
            } else if (e.getComponent() == animalLong) {
                checkAngleFullCircle(animalLong, -180.00, 180.00);
            } else if (e.getComponent() == animalDepth) {
                checkDistanceFormat(animalDepth);
            }
        }

        /**
         * Toggle the animal location parameters display
         */
        public void toggleParams() {
            CardLayout cl = (CardLayout)(animalLocPanel.getLayout());
            if (currentLoc.equals(BEARAZDIST)) {
                cl.show(animalLocPanel, LATLONGDEPTH);
                currentLoc = LATLONGDEPTH;
            } else {
                cl.show(animalLocPanel, BEARAZDIST);
                currentLoc = BEARAZDIST;
            }
        }



        /**
         * Send the data string, using the following parameters:
         * bearing angle, distance, species ID, GPS latitude, GPS longitude,
         * UTC date and UTC time.
         * 
         * NMEA string to use the CPAM format specified by Christopher
         * Kyburg, 05/03/11:
         * 
         * $PSOCALPAM, date,time,lat,long,gyro,speed,dx0,dy0,dz0,animal,
         * signal strength,signal duration,bearing,azimuth,distance, animal lat,
         * animal long, animal depth*CS
         *
         * Date – ddmmyy = Date of the observation leading zero padded e.g. 020408 represents 2nd day of April 2008.
         * Time - hhmmss.uuuuu represents UTC time of observation leading zero padded with fractional seconds to necessary accuracy  based on the 24 hour clock
         * Lat – aa.aaaaa – estimated latitude of ship in decimal degrees WGS-84 Negative number represents Southern hemisphere. Domain = -90.00000 to 90.00000
         * Long - ooo.ooooo – longitude of ship decimal degrees WGS-84.  Negative number represents Western Hemisphere. Domain = -180.00000 to 180.00000
         * Gyro – nnn.nn – true bearing in degrees of ship. Domain =  0 – 359.99 Degrees
         * Speed: ship speed through the water (Knots)
         * dx0,dy0,dz0: array offset (m)relative to GPS (ship) position; if empty assume 0
         * animal: animal name (I would prefer name if that is OK, less chance of a problem).  domain = UNK (unknown), PM (Physeter macrocephalus), ZC (Ziphius cavirostris), GME (Globicephalus melas),  GMA Globicephala Macrohynchus) GG Grampus Griseus, DSP (Delphinidea), a null value is stored as “Not Specified” in the feature class.
         * Signal strength:  domain = 1 – 3; 1 weak, 2 moderate, 3 strong.
         * Signal duration: domain = 1 short, 2 intermediate, 3 continuous.
         * Animal bearing: bearing of animal detection relative to ship bearing in degrees. Domain = 0 TO 359.99
         * Animal Elevation (Azimuth):  domain = -90.00 degrees (directly under array) to 90.00 degrees (directly over the array).
         * Distance: distance from animal to sensor (meters) (or ship if dx0,dy0, dz0 are zero or not reported)
         * Animal lat: Latitude of animal WGS-84 (decimal degrees) domain: 90.000 to -90.000
         * Animal Lon: Longitude of animal WGS-84 (decimal degrees) domain: 180.000 to -180.000
         * Animal Depth: Depth of the animal below local sea level (meters).  Domain: 0 to depth of water column, positive number.

         * CS check sum
         *
         */
        public void sendDataString() {
            String outputString = "$PSOCALPAM,";

            /* if we've got GPS data, use it */
            if (haveGPS) {

                /* Add the current date, based on the GPS input */
                outputString = outputString +
                        PamCalendar.formatDate2(gpsData.getTimeInMillis()) +
                        ",";
                /* Add the current time, based on the GPS input */
                outputString = outputString +
                        PamCalendar.formatTime2(gpsData.getTimeInMillis(), 5) +
                        ",";

                /* Add the latitude */
                outputString = outputString +
                        String.format("%8.5f", gpsData.getLatitude()) +
                        ",";

                /* Add the longitude */
                outputString = outputString +
                        String.format("%9.5f", gpsData.getLongitude()) +
                        ",";

                /* Add the heading */
                String headingTxt = "";
                /* copied from GpsTextDisplay.java...
                 * will have to try various options to find out the heading - may be none
                 * or may be true or may be magnetic.
                 */
                if (gpsData.getTrueHeading() != null) {
                    headingTxt = String.format("%6.2f", gpsData.getTrueHeading());
                } else if (gpsData.getMagneticHeading() != null) {
                    headingTxt = String.format("%6.2f", gpsData.getMagneticHeading());
                }
                outputString = outputString + headingTxt + ",";

            /* if we don't have GPS, just insert null characters */
            } else {
                outputString += ",,,,,";
            }

            /* blank values for the intermediate (unused) parameters */
            outputString += ",,,,";

            /* add the species */
            if (species.getText()!=null) {
                outputString += species.getText();
            }
            outputString += ",";

            /* more blanks for signal strength, signal duration */
            outputString += ",,";

            /* if the user entered bearing-azimuth-distance, add those params
             * to the string and add blanks afterwards for the animal lat, long
             * and depth.
             */
            if (currentLoc.equals(BEARAZDIST)) {

                /* add bearing.  Check first that the bearing is a number.  If not,
                 * replace the text with 0.  Once we know it's a number, make sure
                 * it's within the domain 0 - 359.99.  If not, set it to 0.
                 */
                Double bearingVal=checkAngleFullCircle(bearing, 0, 359.99);
                outputString = outputString + String.format("%6.2f", bearingVal) + ",";

                /* add azimuth.  Check first that the azimuth is a number.  If not,
                 * replace the text with 0.  Once we know it's a number, make sure
                 * it's within the domain -90.00 - 90.00.  If not, set it to 0.
                 */
                Double azimuthVal=checkAngleFullCircle(azimuth, -90.00, 90.00);
                outputString = outputString + String.format("%6.2f", azimuthVal) + ",";

                /* add distance.  Check first that the distance is a number.  If not,
                 * replace the text with 0.
                 */
                int distanceVal=checkDistanceFormat(distance);
                outputString += distanceVal;

                /* add blanks for animal lat, long and depth */
                outputString += ",,,";

            /* if the user entered bearing-azimuth-distance, add those params
             * to the string and add blanks afterwards for the animal lat, long
             * and depth.
             */
            } else {
                /* add blanks for bearing, azimuth and distance */
                outputString += ",,,";

                /* add latitude.  Check first that the latitude is a number.  If not,
                 * replace the text with 0.  Once we know it's a number, make sure
                 * it's within the domain -90.00 - 90.00.  If not, set it to 0.
                 */
                Double latVal=checkAngleFullCircle(animalLat, -90.00, 90.00);
                outputString = outputString + String.format("%6.2f", latVal) + ",";

                /* add longitude.  Check first that the longitude is a number.  If not,
                 * replace the text with 0.  Once we know it's a number, make sure
                 * it's within the domain -180.00 - 180.00.  If not, set it to 0.
                 */
                Double longVal=checkAngleFullCircle(animalLong, -180.00, 180.00);
                outputString = outputString + String.format("%6.2f", longVal) + ",";

                /* add depth.  Check first that the depth is a number.  If not,
                 * replace the text with 0.
                 */
                int depthVal=checkDistanceFormat(animalDepth);
                outputString += depthVal;
            }

            /* add checksum */
            int checkSum = AcquireNmeaData.createStringChecksum(new StringBuffer(outputString));
            outputString += String.format("*%02X", checkSum);

            /* add <CR><LF> at end */
            outputString += "\r\n";

            /* troubleshooting - print the sentence in a dialog box */
            //JOptionPane.showMessageDialog(null, outputString);

            /* send the outputString to the serial port */
            wildCom.writeToPort(outputString);

            /*
            /* clear anything in the existing sidePanel*/
            clearThePanel();

            /* save the data, if the user selected that option */
            if (wildControl.getWildParameters().isOutputSaved()) {
                boolean success = saveOutput(outputString);
            }
        }
    }

    /**
     * Save the NMEA data sentence
     *
     * @return boolean indicating success or failure
     */
    private boolean saveOutput(String outputString) {

        /* try opening the file */
        try {
            File fName = wildControl.getWildParameters().getOutputFilename();
//            System.out.println(String.format("writing data to ... %s", fName.getAbsolutePath()));

            /* create a String containing the histogram data we
             * want to save, and save it to the file.
             */
            BufferedWriter writer = new BufferedWriter(new FileWriter(fName, true));
//            String contourStats = roccaContourDataBlock.createContourStatsString();
            writer.write(outputString);
            writer.newLine();
            writer.close();

        } catch(FileNotFoundException ex) {
            System.out.println("Could not find file");
            ex.printStackTrace();
            return false;
        } catch(IOException ex) {
            System.out.println("Could not write NMEA string to file");
            ex.printStackTrace();
            return false;
        }
        return true;
    }


    /**
     * Make sure the angle (bearing,  is between 0 and 359.99 degrees
     */
    public Double checkAngleFullCircle(JTextField angleTxt, double minVal, double maxVal) {
        Double angle=0.0;
        try{
            angle = Double.parseDouble(angleTxt.getText());
        } catch (NumberFormatException nfe) {
            angleTxt.setText("0");
            angle=0.0;
        }
        if (angle < minVal || angle > maxVal) {
            angle = 0.0;
        }
        angleTxt.setText(String.format("%-6.2f", angle));
        return angle;
    }

//    /**
//     * Make sure the azimuth is between -90.00 and 90.00 degrees
//     */
//    public Double checkAzimuthFormat() {
//        Double azimuthVal=0.0;
//        try{
//            azimuthVal = Double.parseDouble(azimuth.getText());
//        } catch (NumberFormatException nfe) {
//            azimuth.setText("0");
//            azimuthVal=0.0;
//        }
//        if (azimuthVal < -90.00 || azimuthVal > 90.00) {
//            azimuthVal = 0.0;
//        }
//        azimuth.setText(String.format("%-6.2f", azimuthVal));
//        return azimuthVal;
//    }
//
    /**
     * Make sure the distance is in an integer format (no decimal places - round
     * off if needed)
     */
    public int checkDistanceFormat(JTextField distanceTxt) {
        Double distanceVal=0.0;
        try{
            distanceVal = Double.parseDouble(distanceTxt.getText());
        } catch (NumberFormatException nfe) {
            distanceTxt.setText("0");
            distanceVal=0.0;
        }
        if (distanceVal < 0) {
            distanceVal=0.0;
        }
        int roundedDistVal = (int) Math.round(distanceVal);
        distanceTxt.setText(""+roundedDistVal);
        return roundedDistVal;
    }
}

