/*	PAMGUARD - Passive Acoustic Monitoring GUARDianship.
 * To assist in the Detection Classification and Localisation 
 * of marine mammals (cetaceans).
 *  
 * Copyright (C) 2006 
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
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
package UserInput;

import PamguardMVC.PamDataBlock;
import PamguardMVC.PamObservable;
import PamguardMVC.PamProcess;

public class UserInputProcess extends PamProcess {

	PamDataBlock<UserInputDataUnit> uiDataBlock;

	public UserInputProcess(UserInputController pamControlledUnit,
			PamDataBlock data) {
		super(pamControlledUnit, data);

		addOutputDataBlock((uiDataBlock = new PamDataBlock<UserInputDataUnit>(UserInputDataUnit.class,
				"User Input Data", this, 0)));
		uiDataBlock.setNaturalLifetime(3600*24); // natural lifetime 24 hours. 
		uiDataBlock.setMixedDirection(PamDataBlock.MIX_INTODATABASE);

		// PamModel.getPamModel().setGpsDataBlock(uiDataBlock);

		uiDataBlock.SetLogging(new UserInputLogger(uiDataBlock));
	}

	// PamProcess Overidden Methods
	@Override
	public void pamStart() {
	}

	@Override
	public void pamStop() {
	}


	@Override
	public long getRequiredDataHistory(PamObservable o, Object arg) {
		return 0;
	}
}
