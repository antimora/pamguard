package ipiDemo;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import PamView.PamBorderPanel;
import PamView.PamColors;
import PamView.PamLabel;
import PamView.PamSidePanel;
import PamView.PamColors.PamColor;

/**
 * Displays summary information about the IPI modules.
 * @author Brian Miller
 */
public class IpiSidePanel implements PamSidePanel{

	IpiController ipiController;

	SidePanel sidePanel;

	TitledBorder titledBorder;

	JTextField ensembleIpi, ensembleMinus, ensemblePlus, meanIpi, clickCount;

	public IpiSidePanel(IpiController ipiController) {

		this.ipiController = ipiController;

		sidePanel = new SidePanel();

	}

	private class SidePanel extends PamBorderPanel {

		private static final long serialVersionUID = 1L;

		public SidePanel() {
			super();
			setBorder(titledBorder = new TitledBorder(ipiController
					.getUnitName()));
			titledBorder.setTitleColor(PamColors.getInstance().getColor(
					PamColor.AXIS));

			GridBagLayout gb = new GridBagLayout();
			GridBagConstraints c = new GridBagConstraints();
			setLayout(gb);

			ensembleIpi = new JTextField(5);
			ensembleMinus = new JTextField(3);
			ensemblePlus = new JTextField(3);
			clickCount = new JTextField(5);
			meanIpi = new JTextField(5);
			ensembleIpi.setEditable(false);
			meanIpi.setEditable(false);
			clickCount.setEditable(false);

			c.anchor = GridBagConstraints.EAST;
			c.ipadx = 5;
			c.gridx = c.gridy = 0;
			c.gridwidth = 2;
			c.gridwidth = 1;
			addComponent(this, new PamLabel("Clicks"), c);
			c.gridx++;
			addComponent(this, clickCount, c);
			c.gridx = 0;
			c.gridy++;
			addComponent(this, new PamLabel("Ensemble IPI (ms)"), c);
			c.gridx++;
			addComponent(this, ensembleIpi, c);
			c.gridx = 0;
			c.gridy++;
			addComponent(this, new PamLabel("-"), c);
			c.gridx++;
			addComponent(this, ensembleMinus, c);
			c.gridx = 0;
			c.gridy++;
			addComponent(this, new PamLabel("+"), c);
			c.gridx++;
			addComponent(this, ensemblePlus, c);
			c.gridx = 0;
			c.gridy++;
			addComponent(this, new PamLabel("Mean IPI (ms)"), c);
			c.gridx++;
			addComponent(this, meanIpi, c);

		}

		@Override
		public void setBackground(Color bg) {
			super.setBackground(bg);
			if (titledBorder != null) {
				titledBorder.setTitleColor(PamColors.getInstance().getColor(
						PamColor.AXIS));
			}
		}

		private void fillData() {
			clickCount.setText(String.format("%d",
					ipiController.ipiProcess.numClicks));
			ensembleIpi.setText(String.format("%1.3f", ipiController.ipiProcess
					.getIpiDelayEnsembleAvg() * 1e3));
			ensembleMinus.setText(String.format("%1.3f",
					ipiController.ipiProcess.getIpiDelayLowerLimit() * 1e3));
			ensemblePlus.setText(String.format("%1.3f",
					ipiController.ipiProcess.getIpiDelayUpperLimit() * 1e3));
			meanIpi.setText(String.format("%1.3f", ipiController.ipiProcess
					.getIpiDelayMean() * 1e3));
		}
	}

	public JComponent getPanel() {
		return sidePanel;
	}

	public void fillData(){
		sidePanel.fillData();
	}
	
	public void rename(String newName) {
		titledBorder.setTitle(newName);
		sidePanel.repaint();
	}
}
