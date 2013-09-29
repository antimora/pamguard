package PamView;

import PamGraph3D.PamPanel3D;

public class ClipboardCopier3D extends ClipboardCopier {

	private PamPanel3D pamPanel3D;

	public ClipboardCopier3D(PamPanel3D pamPanel3D) {
		super(pamPanel3D);
		this.pamPanel3D=pamPanel3D;
	}
	
	@Override
	protected void createTransferImage() {
		setImage(pamPanel3D.getBufferedImage());
	}
	

}
