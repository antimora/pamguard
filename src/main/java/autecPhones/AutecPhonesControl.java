package autecPhones;

import java.awt.Frame;

import PamController.PamControlledUnit;
import PamView.PamSymbolManager;
import PamguardMVC.PamProcess;

public class AutecPhonesControl extends PamControlledUnit {

	AutecProcess autecProcess;
	
	AutecDataBlock autecDataBlock;
	
	AutecGraphics autecGraphics;
	
	public AutecPhonesControl(String unitName) {
		super("AUTEC Phones", unitName);
		addPamProcess(autecProcess = new AutecProcess(this));
	}

	public Frame getGuiFrame() {
		return super.getPamView().getGuiFrame();
	}
	class AutecProcess extends PamProcess {

		public AutecProcess(PamControlledUnit pamControlledUnit) {
			super(pamControlledUnit, null);
			addOutputDataBlock(autecDataBlock = new AutecDataBlock(this));
			autecDataBlock.setOverlayDraw(autecGraphics = new AutecGraphics());
			PamSymbolManager.getInstance().addManagesSymbol(autecGraphics);
		}

		@Override
		public void pamStart() {
		}

		@Override
		public void pamStop() {
		}
		
	}
}
