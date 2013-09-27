package Map;

import PamguardMVC.PamDataBlock;



/**
 * The map needs a process since it now produces data units to go into a data block.
 * @author Doug
 *
 */
public class MapProcess extends PamguardMVC.PamProcess {
	
	private MapController mapController;
	
	private MapCommentDataBlock mapCommentDataBlock;

	public MapProcess(MapController mapController) {
		super(mapController, null);
		this.mapController = mapController;
		mapCommentDataBlock = new MapCommentDataBlock("Map Comments", this);
		addOutputDataBlock(mapCommentDataBlock);
		mapCommentDataBlock.setOverlayDraw(new MapCommentOverlayGraphics(mapCommentDataBlock));
		mapCommentDataBlock.SetLogging(new MapCommentSQLLogging(mapCommentDataBlock));
		mapCommentDataBlock.setMixedDirection(PamDataBlock.MIX_INTODATABASE);
	}

	@Override
	public void pamStart() {
	}

	@Override
	public void pamStop() {
	}

	public PamDataBlock<MapComment> getMapCommentDataBlock() {
		return mapCommentDataBlock;
	}
	
	
	
}
