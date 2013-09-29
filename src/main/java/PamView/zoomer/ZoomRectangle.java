package PamView.zoomer;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;

public class ZoomRectangle extends ZoomShape {
	
	double x1, x2, y1, y2; 
	
	public ZoomRectangle(Zoomer zoomer, int coordindateType, double xStart, double yStart) {
		super(zoomer, coordindateType);
		x1 = x2 = xStart;
		y1 = y2 = yStart;
	}

	@Override
	public Rectangle drawShape(Graphics g, Component component, boolean beforeOther) {
//		if (beforeOther == isClosed()) {
//			return null;
//		}
		if (beforeOther) return null;
		Rectangle r = getBounds(component);
		g.drawRect(r.x, r.y, r.width, r.height);
		return r;
	}

	@Override
	public boolean containsPoint(Component component, Point pt) {
		Rectangle r = getBounds(component);
		return r.contains(pt);
	}

	@Override
	public void closeShape() {
		super.closeShape();
	}

	@Override
	public boolean removeOnZoom() {
		return true;
	}

	@Override
	public void newPoint(double x, double y) {
		x2 = x;
		y2 = y;
	}
	
	

	@Override
	public Rectangle getBounds(Component component) {
		Point p1 = getZoomer().xyValToPoint(component, x1, y1);
		Point p2 = getZoomer().xyValToPoint(component, x2, y2);
		Rectangle r = new Rectangle();
		r.x = Math.min(p1.x, p2.x);
		r.y = Math.min(p1.y, p2.y);
		r.width = Math.abs(p2.x-p1.x);
		r.height = Math.abs(p2.y-p1.y);
		return r;
	}

	@Override
	public double getXLength() {
		return Math.abs(x2-x1);
	}

	@Override
	public double getXStart() {
		return Math.min(x2,x1);
	}

	@Override
	public double getYLength() {
		return Math.abs(y2-y1);
	}

	@Override
	public double getYStart() {
		return Math.min(y2,y1);
	}

}
