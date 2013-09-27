package clickDetector.ClickClassifiers;

import clickDetector.ClickAlarm;
import java.awt.Frame;

import javax.swing.JMenuItem;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import clickDetector.ClickDetection;

import PamView.PamSymbol;

public class NullClassifier implements ClickIdentifier {

	@Override
	public int codeToListIndex(int code) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ClassifyDialogPanel getDialogPanel(Frame windowFrame) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JMenuItem getMenuItem(Frame parentFrame) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSpeciesName(int code) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getSpeciesList() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PamSymbol getSymbol(ClickDetection click) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PamSymbol[] getSymbols() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ClickIdInformation identify(ClickDetection click) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean fillXMLParamaeters(Document doc, Element classEl) {
		return false;
	}

	@Override
	public String getParamsInfo(ClickDetection click) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int[] getCodeList() {
		// TODO Auto-generated method stub
		return null;
	}


    public ClickTypeCommonParams getCommonParams(int code) {
        return null;
    }



}
