/*	PAMGUARD - Passive Acoustic Monitoring GUARDianship.
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

package rocca;

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
 * Main controller for Rocca
 *
 * @author Michael Oswald
 */
public class RoccaControl extends PamControlledUnit implements PamSettings {

    protected RoccaControl roccaControl;
    protected RoccaParameters roccaParameters = new RoccaParameters();
    protected RoccaProcess roccaProcess;
    protected RoccaWhistleSelect roccaWhistleSelect;
    protected RoccaSidePanel roccaSidePanel;


    public RoccaControl(String name){
        super("Rocca", name);

        roccaControl = this;
        PamSettingManager.getInstance().registerSettings(this);
        addPamProcess(roccaProcess = new RoccaProcess(this));
        addPamProcess(roccaWhistleSelect = new RoccaWhistleSelect(this));
		setSidePanel(roccaSidePanel = new RoccaSidePanel(this));
        /*

		addPamProcess(clickDetector = new ClickDetector(this));

		addPamProcess(clickTrainDetector = new ClickTrainDetector(this, clickDetector.getClickDataBlock()));

		addPamProcess(trackedClickLocaliser = new TrackedClickLocaliser(this, clickDetector.getTrackedClicks()));

		setTabPanel(tabPanelControl = new ClickTabPanelControl(this));

		clickPanel = tabPanelControl.getClickPanel();

		setSidePanel(clickSidePanel = new ClickSidePanel(this));

		rightMouseMenu = createDisplayMenu(null);

		new ClickSpectrogramPlugin(this);
*/
    }

    @Override
	public JMenuItem createDetectionMenu(Frame parentFrame) {
		JMenuItem menuItem = new JMenuItem(getUnitName() + " Parameters");
		menuItem.addActionListener(new SetParameters(parentFrame));
		return menuItem;
	}

	class SetParameters implements ActionListener {

		Frame parentFrame;

		public SetParameters(Frame parentFrame) {
			this.parentFrame = parentFrame;
		}

		public void actionPerformed(ActionEvent e) {
			RoccaParameters newParams = RoccaParametersDialog.showDialog
                    (parentFrame, roccaParameters, roccaControl);
			/*
			 * The dialog returns null if the cancel button was set. If it's
			 * not null, then clone the parameters onto the main parameters reference
			 * and call preparePRocess to make sure they get used !
			 */
			if (newParams != null) {
				roccaParameters = newParams.clone();

                // if the classifier model isn't loaded, load it now...
                if (!roccaProcess.isClassifierLoaded()) {
                    roccaProcess.setClassifierLoaded(
                            roccaProcess.roccaClassifier.setUpClassifier());
                }

                // run the remaining prep code
				roccaProcess.prepareProcess();
			}
		}
	}

    // Serializable methods
    public Serializable getSettingsReference() {
        return roccaParameters;
    }

    public long getSettingsVersion() {
		return RoccaParameters.serialVersionUID;
    }

    public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {
		this.roccaParameters = ((RoccaParameters) pamControlledUnitSettings
				.getSettings()).clone();
 		return true;
    }


}
