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

import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamSettingManager;
import PamController.PamSettings;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import javax.swing.JMenuItem;

/**
 *
 * @author Michael Oswald
 */
public class WILDControl extends PamControlledUnit implements PamSettings {

    private WILDControl wildControl;
    private WILDSidePanel wildSidePanel;
    private WILDParameters wildParameters = new WILDParameters();

    public WILDControl(String name){
        super("WILD-ArcGIS interface", name);

        wildControl = this;
        PamSettingManager.getInstance().registerSettings(this);
		setSidePanel(wildSidePanel = new WILDSidePanel(this));
    }

    /**
     * Add WILDParameters to the Detection menu
     *
     * @param parentFrame
     * @return
     */
    @Override
	public JMenuItem createDetectionMenu(Frame parentFrame) {
		JMenuItem menuItem = new JMenuItem(getUnitName());
		menuItem.addActionListener(new SetParameters(parentFrame));
		return menuItem;
	}

	class SetParameters implements ActionListener {

		Frame parentFrame;

		public SetParameters(Frame parentFrame) {
			this.parentFrame = parentFrame;
		}

		public void actionPerformed(ActionEvent e) {
			WILDParameters newParams = WILDParametersDialog.showDialog
                    (parentFrame, wildParameters, wildControl);
			/*
			 * The dialog returns null if the cancel button was set. If it's
			 * not null, then clone the parameters onto the main parameters reference
			 * and call prepareProcess to make sure they get used !
			 */
			if (newParams != null) {
				wildParameters = newParams.clone();
				wildSidePanel.prepareProcess();
			}
		}
	}

    public WILDControl getWildControl() {
        return wildControl;
    }

    public WILDSidePanel getWildSidePanel() {
        return wildSidePanel;
    }

    /**
     * returns the WildParameters object.  Note that since this object reference
     * changes whenever the user open the parameters dialog, other classes should
     * always use wildControl.getWildParameters when referring to this object
     * rather than creating a local reference which may become outdated.
     * 
     * @return
     */
    public WILDParameters getWildParameters() {
        return wildParameters;
    }

    public Serializable getSettingsReference() {
        return wildParameters;
    }

    public long getSettingsVersion() {
        return WILDParameters.getSerialVersionUID();
    }

    public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {
		wildParameters = ((WILDParameters) pamControlledUnitSettings
				.getSettings()).clone();
 		return true;
    }
}
