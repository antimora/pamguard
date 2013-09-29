package PamView;

import java.awt.GridBagConstraints;
import java.awt.Insets;

/**
 * Standard grid bag contraints to use as a default in 
 * dialogs. 
 * 
 * @author Douglas Gillespie
 *
 */
public class PamGridBagContraints extends GridBagConstraints {

	public PamGridBagContraints() {
		super();
		gridx = gridy = 0;
		fill = HORIZONTAL;
		anchor = WEST;
		insets = new Insets(2,2,2,2);
	}

}
