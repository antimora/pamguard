package dataMap;

import java.awt.Color;
import java.awt.Graphics;

import PamView.KeyPanel;
import dataMap.DataStreamPanel.DataGraph;

/**
 * Interface for module specific data map drawing. 
 * @author Doug Gillespie.
 *
 */
public interface DataMapDrawing {

	public void drawEffort(Graphics g, DataGraph dataGraph, OfflineDataMap map,
			Color haveDataColour);

	public void drawDataRate(Graphics g, DataGraph dataGraph, OfflineDataMap map,
			Color dataStreamColour);
	
	public KeyPanel getKeyPanel();

}
