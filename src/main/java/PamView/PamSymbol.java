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
package PamView;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.io.Serializable;

import javax.swing.Icon;
import javax.swing.JPanel;

import PamView.PamColors.PamColor;

/**
 * 
 * Standard symbols for Pamguard graphics. A number of shapes
 * are available, most of which can have a fill colour and a line
 * colour. They may be created anywhere in Pamguard code using
 * new PamSymbol(...) and can also be created and configured using
 * a PamSymbolDialog to chose the symbol type, colours, line thicknesses, etc.
 * A selection of drawing routines allow you to draw them on plots with
 * varying sizes at any location.
 * 
 * @author Doug Gillespie
 * @see PamSymbolDialog
 * @see PanelOverlayDraw
 *
 */
public class PamSymbol implements Serializable, Icon, Cloneable {

	static public final long serialVersionUID = -5212611766032085395L;

	static public final int SYMBOL_NONE = 0;

	static public final int SYMBOL_REGIONSTART = 1;

	static public final int SYMBOL_LINESTART = 2;

	static public final int SYMBOL_LINESEGMENT = 3;

	/*
	 * Definitions of symbol types
	 */
	static public final int SYMBOL_CROSS = 4;

	static public final int SYMBOL_CROSS2 = 5;

	static public final int SYMBOL_SQUARE = 6;

	static public final int SYMBOL_TRIANGLEU = 7;

	static public final int SYMBOL_CIRCLE = 8;

	static public final int SYMBOL_DIAMOND = 9;

	static public final int SYMBOL_POINT = 10;

	static public final int SYMBOL_STAR = 11;

	static public final int SYMBOL_TRIANGLED = 12;

	static public final int SYMBOL_TRIANGLEL = 13;

	static public final int SYMBOL_TRIANGLER = 14;

	static public final int SYMBOL_PENTAGRAM = 15;

	static public final int SYMBOL_HEXAGRAM = 16;

	static public final int SYMBOL_CUSTOMPOLYGON = 17;

	static public final int SYMBOL_DOUBLETRIANGLEL = 18;

	static public final int SYMBOL_DOUBLETRIANGLER = 19;

	static public final int ICON_STYLE_SYMBOL = 0x1;

	static public final int ICON_STYLE_LINE = 0x2;

	private int iconStyle = ICON_STYLE_SYMBOL;

	/**
	 * Convert a single character text code into 
	 * a symbol type more or less following the Matlab symbol 
	 * definitions. 
	 * @param textCode text code
	 * @return symbol type
	 */
	static public int interpretTextCode(String textCode) {
		if (textCode == null) {
			return 0;
		}
		if (textCode.equalsIgnoreCase("x")) {
			return SYMBOL_CROSS;
		} else if (textCode.equalsIgnoreCase("+")) {
			return SYMBOL_CROSS2;
		} else if (textCode.equalsIgnoreCase("s")) {
			return SYMBOL_SQUARE;
		} else if (textCode.equalsIgnoreCase("^")) {
			return SYMBOL_TRIANGLEU;
		} else if (textCode.equalsIgnoreCase("o")) {
			return SYMBOL_CIRCLE;
		} else if (textCode.equalsIgnoreCase("d")) {
			return SYMBOL_DIAMOND;
		} else if (textCode.equalsIgnoreCase(".")) {
			return SYMBOL_POINT;
		} else if (textCode.equalsIgnoreCase("*")) {
			return SYMBOL_STAR;
		} else if (textCode.equalsIgnoreCase("v")) {
			return SYMBOL_TRIANGLED;
		} else if (textCode.equalsIgnoreCase("<")) {
			return SYMBOL_TRIANGLEL;
		} else if (textCode.equalsIgnoreCase(">")) {
			return SYMBOL_TRIANGLER;
		} else if (textCode.equalsIgnoreCase("p")) {
			return SYMBOL_PENTAGRAM;
		} else if (textCode.equalsIgnoreCase("h")) {
			return SYMBOL_HEXAGRAM;
		} else if (textCode.equalsIgnoreCase("Cross")) {
			return SYMBOL_CROSS;
		} else if (textCode.equalsIgnoreCase("Cross2")) {
			return SYMBOL_CROSS2;
		} else if (textCode.equalsIgnoreCase("Square")) {
			return SYMBOL_SQUARE;
		} else if (textCode.equalsIgnoreCase("Traingle")) {
			return SYMBOL_TRIANGLEU;
		} else if (textCode.equalsIgnoreCase("Circle")) {
			return SYMBOL_CIRCLE;
		} else if (textCode.equalsIgnoreCase("Diamond")) {
			return SYMBOL_DIAMOND;
		} else
			return 0;
	}

	/**
	 * 
	 * @return the text code for this symbol
	 */
	public char getTextCode() {
		return getTextCode(this.symbol);
	}
	/**
	 * Get a text code for a symbol
	 * @param symbol symbol type
	 * @return text code
	 */
	static public char getTextCode(int symbol) {
		switch(symbol) {
		case SYMBOL_CROSS:
			return '+';
		case SYMBOL_CROSS2:
			return 'x';
		case SYMBOL_SQUARE:
			return 's';
		case SYMBOL_TRIANGLEU:
			return '^';
		case SYMBOL_CIRCLE:
			return 'o';
		case SYMBOL_DIAMOND:
			return 'd';
		case SYMBOL_POINT:
			return '.';
		case SYMBOL_STAR:
			return '*';
		case SYMBOL_TRIANGLED:
			return 'v';
		case SYMBOL_TRIANGLEL:
			return '<';
		case SYMBOL_TRIANGLER:
			return '>';
		case SYMBOL_PENTAGRAM:
			return 'p';
		case SYMBOL_HEXAGRAM:
			return 'h';
		}
		return 0;
	}
	/*
	 * drawing dimensions for the various shapes.
	 */
	//	static private final double[] sqx = { -.5, .5, .5, -.5 };
	//
	//	static private final double[] sqy = { -.5, -.5, .5, .5 };

	static private final double[] sqx = { -1, 1, 1, -1 };

	static private final double[] sqy = { -1, -1, 1, 1 };

	//	static private final double[] diax = { 0, 0.5, 0, -0.5 };
	//
	//	static private final double[] diay = { -.5, 0, .5, 0 };

	static private final double[] diax = { 0, 1, 0, -1 };

	static private final double[] diay = { -1, 0, 1, 0 };

	//	static private final double[] trux = { 0, 0.866, -0.866 };
	//
	//	static private final double[] truy = { -0.866, 0.866, 0.866 };
	static private final double[] trux = { 0, 1, -1 };

	static private final double[] truy = { -0.5, 0.5, 0.5 };

	//	static private final double[] trdx = { 0, 0.866, -0.866 };
	//
	//	static private final double[] trdy = { 0.866, -0.866, -0.866 };
	static private final double[] trdx = { 0, 1, -1 };

	static private final double[] trdy = { 0.5, -0.5, -0.5 };

	//	static private final double[] trrx = { 1, -0.5, -0.5 };
	//
	//	static private final double[] trry = { 0, 0.866, -0.866 };
	static private final double[] trrx = { 0.5, -0.5, -0.5 };

	static private final double[] trry = { 0, 1, -1 };

	//	static private final double[] trlx = { -1, 0.5, 0.5 };
	//
	//	static private final double[] trly = { 0, 0.866, -0.866 };

	static private final double[] trlx = { -.5, .5, .5 };

	static private final double[] trly = { 0, 1, -1 };

	static private final double[] pentx = { 0, 0.951, 0.588, -0.588, -0.951 };

	static private final double[] penty = { -1, -0.309, 0.809, 0.809, -0.309 };

	static private final double[] hexx = { 1, .5, -.5, -1, -.5, .5 };

	static private final double[] hexy = { 0, 0.866, 0.866, 0, -0.866, -0.866 };

	// static private boolean first = true;

	/*
	 * Parameters used to describe a shape - all other are statics.
	 */
	private int symbol = SYMBOL_CIRCLE;

	private int width = 5;

	private int height = 5;

	private float lineThickness = 2;

	private boolean fill = true;

	private Color fillColor = Color.BLACK;

	private Color lineColor = Color.BLACK;

	/**
	 * Simplest constructor creates a PamSymbol with 
	 * default attributes. You will probably only use
	 * this constructor if you plan to subsequently
	 * modify it with PamSymbolDialog
	 * @see PamSymbolDialog
	 *
	 */
	public PamSymbol() {
		super();
	}

	/**
	 * Creates a PamSymbol with a given shape, size, colour, etc.
	 * @param symbol Symbol type
	 * @param width  Width of symbol in pixels
	 * @param height  Height of symbol in pixels
	 * @param fill  true if the symbol is to be filled, false if the shape should be hollow
	 * @param fillColor fill colour (required fill to be true)
	 * @param lineColor line colour
	 */
	public PamSymbol(int symbol, int width, int height, boolean fill,
			Color fillColor, Color lineColor) {
		super();
		// TODO Auto-generated constructor stub
		this.symbol = symbol;
		this.width = width;
		this.height = height;
		this.fill = fill;
		this.fillColor = fillColor;
		this.lineColor = lineColor;
	}

	/**
	 * Draw the symbbol at a given point using it's preset size.
	 * @param g graphics component to draw on
	 * @param pt x,y coordinate to draw centre of symbol at
	 */
	public Rectangle draw(Graphics g, Point pt) {
		return draw(g, pt, width, height, fill, lineThickness, fillColor,
				lineColor);
	}

	/**
	 * Draw the symbol at a given point using a new width and height.
	 * @param g graphics component to draw on
	 * @param pt x,y coordinate to draw centre of symbol at
	 * @param width width for drawing symbol (overrides preset width)
	 * @param height height for drawing symbol (overrides prest height)
	 */
	public void draw(Graphics g, Point pt, int width, int height) {
		draw(g, pt, width, height, fill, lineThickness, fillColor,
				lineColor);
	}


	/**
	 * 
	 * Draw the symbol using a complete new set of parameters.
	 * @param g graphics component to draw on
	 * @param pt x,y coordinate to draw centre of symbol at
	 * @param w width for drawing symbol (overrides preset width)
	 * @param h height for drawing symbol (overrides prest height)
	 * @param fill true if the symbol is to be filled, false for hollow
	 * @param lineThickness outer line thickness
	 * @param fillColor fill colour
	 * @param lineColor line colour
	 * @return a rectangle giving an outer boud of the shape (can be used to invaldiate a 
	 * graphic for redrawing).
	 */
	public Rectangle draw(Graphics g, Point pt, double w,
			double h, boolean fill, float lineThickness, Color fillColor,
			Color lineColor) {

		Graphics2D g2d = (Graphics2D) g;

		// g2d.setPaint(lineColor);
		// g2d.setColor(lineColor);
		Stroke oldStroke = g2d.getStroke();
		g2d.setStroke(new BasicStroke(lineThickness, BasicStroke.CAP_ROUND,
				BasicStroke.JOIN_MITER));

		int i;
		double halfWidth = Math.max(1., w / 2.);
		double halfHeight = Math.max(1., h / 2.);
		// if (!Fill) DC.RestoreBrush();
		// LOGPEN lp;
		if (symbol == SYMBOL_POINT) {
			// HPEN CurrPen = (HPEN) DC.GetCurrentObject(OBJ_PEN);
			// OWL::TPen tp(CurrPen);
			// tp.GetObject(lp);
			// g2d.fil
			//			draw(g, pt, SYMBOL_CIRCLE, 2, 2, true, 1, lineColor, lineColor);
		}
		switch (symbol) {
		case SYMBOL_POINT:
			halfWidth = 1;
			halfHeight = 1;
			w = h = 2;
			fill = true;
			fillColor = lineColor;
			// DC.SetPixel(Pt, NS_CLASSLIB::TColor(lp.lopnColor));
			Ellipse2D j = new Ellipse2D.Double(pt.x - halfWidth, pt.y
					- halfHeight, w, h);
			if (fill) {
				g2d.setPaint(fillColor);
				g2d.fill(j);
			}
			// else {
			g2d.setPaint(lineColor);
			g2d.draw(j);
			setSquareDrawnPolygon(pt.x, pt.y, pt.x, pt.y);
			break;
		case SYMBOL_CROSS:
			g2d.setPaint(lineColor);
			g.drawLine((int) (pt.x - halfWidth), pt.y,
					(int) (pt.x + halfWidth), pt.y);
			g.drawLine(pt.x, (int) (pt.y - halfHeight), pt.x,
					(int) (pt.y + halfHeight));
			setSquareDrawnPolygon(pt.x, (int) (pt.y - halfHeight), pt.x,
					(int) (pt.y + halfHeight));
			break;
		case SYMBOL_CROSS2:
			g2d.setPaint(lineColor);
			g.drawLine((int) (pt.x - halfWidth), (int) (pt.y - halfHeight),
					(int) (pt.x + halfWidth), (int) (pt.y + halfHeight));
			g.drawLine((int) (pt.x + halfWidth), (int) (pt.y - halfHeight),
					(int) (pt.x - halfWidth), (int) (pt.y + halfHeight));
			setSquareDrawnPolygon(pt.x, (int) (pt.y - halfHeight), pt.x,
					(int) (pt.y + halfHeight));
			break;
		case SYMBOL_SQUARE:
			drawScaledPolygon(g2d, pt, 3, halfWidth, halfHeight, sqx, sqy,
					fill, fillColor, lineColor);
			break;
		case SYMBOL_TRIANGLEU:
			drawScaledPolygon(g2d, pt, 3, halfWidth, halfHeight, trux, truy,
					fill, fillColor, lineColor);
			break;
		case SYMBOL_TRIANGLED:
			drawScaledPolygon(g2d, pt, 3, halfWidth, halfHeight, trdx, trdy,
					fill, fillColor, lineColor);
			break;
		case SYMBOL_TRIANGLER:
			drawScaledPolygon(g2d, pt, 3, halfWidth, halfHeight, trrx, trry,
					fill, fillColor, lineColor);
			break;
		case SYMBOL_TRIANGLEL:
			drawScaledPolygon(g2d, pt, 3, halfWidth, halfHeight, trlx, trly,
					fill, fillColor, lineColor);
			break;
		case SYMBOL_DOUBLETRIANGLER:
			pt.x -= (halfWidth/2+1);
			drawScaledPolygon(g2d, pt, 3, halfWidth, halfHeight, trrx, trry,
					fill, fillColor, lineColor);
			pt.x += halfWidth+2;
			drawScaledPolygon(g2d, pt, 3, halfWidth, halfHeight, trrx, trry,
					fill, fillColor, lineColor);
			break;
		case SYMBOL_DOUBLETRIANGLEL:
			pt.x -= (halfWidth/2+1);
			drawScaledPolygon(g2d, pt, 3, halfWidth, halfHeight, trlx, trly,
					fill, fillColor, lineColor);
			pt.x += halfWidth+2;
			drawScaledPolygon(g2d, pt, 3, halfWidth, halfHeight, trlx, trly,
					fill, fillColor, lineColor);
			break;
		case SYMBOL_PENTAGRAM:
			drawScaledPolygon(g2d, pt, 5, halfWidth, halfHeight, pentx, penty,
					fill, fillColor, lineColor);
			break;
		case SYMBOL_HEXAGRAM:
			drawScaledPolygon(g2d, pt, 6, halfWidth, halfHeight, hexx, hexy,
					fill, fillColor, lineColor);
			break;
		case SYMBOL_DIAMOND:
			drawScaledPolygon(g2d, pt, 6, halfWidth, halfHeight, diax, diay,
					fill, fillColor, lineColor);
			break;
		case SYMBOL_CIRCLE:
			Ellipse2D o = new Ellipse2D.Double(pt.x - halfWidth+1, pt.y
					- halfHeight+1, w-2, h-2);
			if (fill) {
				g2d.setPaint(fillColor);
				g2d.fill(o);
			}
			// else {
			g2d.setPaint(lineColor);
			g2d.draw(o);
			setSquareDrawnPolygon(pt.x, (int) (pt.y - halfHeight), pt.x,
					(int) (pt.y + halfHeight));
			// }
			break;
		case SYMBOL_STAR:
			for (i = 0; i < 6; i++) {
				g2d.setPaint(lineColor);
				g2d.drawLine(pt.x, pt.y, (int) (pt.x + Math.ceil(hexx[i]
				                                                      * halfWidth)), (int) (pt.y - Math.ceil(hexy[i]
				                                                                                                  * halfHeight)));
				// DC.MoveTo(pt.x + ceil(hexx[i] * HalfSize), pt.y -
				// ceil(hexy[i]*HalfSize));
				// DC.LineTo(pt.x + ceil(hexx[i+3] * HalfSize), pt.y -
				// ceil(hexy[i+3]*HalfSize));
			}
			setSquareDrawnPolygon(pt.x, (int) (pt.y - halfHeight), pt.x,
					(int) (pt.y + halfHeight));
			break;
		case SYMBOL_CUSTOMPOLYGON:
			drawScaledPolygon(g2d, pt, 6, halfWidth, halfHeight, getXPoints(), getYPoints(),
					fill, fillColor, lineColor);
			break;
		default:
			g2d.setPaint(lineColor);
		g2d.fillOval((int) (pt.x - halfWidth), (int) (pt.y - halfHeight),
				(int) (pt.x + halfWidth), (int) (pt.y + halfHeight));
		setSquareDrawnPolygon(pt.x, (int) (pt.y - halfHeight), pt.x,
				(int) (pt.y + halfHeight));
		break;
		}
		g2d.setStroke(oldStroke);
		return new Rectangle((int) Math.floor(pt.x - halfWidth), (int) Math.floor(pt.y - halfHeight),
				(int) w+1, (int) h+1);
	}

	/**
	 * Called to set a square drawn polygon when 
	 * drawing shapes which didn't actually use the polygon draw function. 
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 */
	private void setSquareDrawnPolygon(int x1, int y1, int x2, int y2) {
		int[] xp = {x1, x1, x2, x2};
		int[] yp = {y1, y2, y2, y1};
		drawnPolygon = new Polygon(xp, yp, 4);
	}

	/**
	 * The last polygon used for drawing. 
	 */
	private transient Polygon drawnPolygon;

	public static final int ICON_HORIZONTAL_LEFT = 0;
	public static final int ICON_HORIZONTAL_CENTRE = 1;
	public static final int ICON_HORIZONTAL_RIGHT = 2;
	public static final int ICON_HORIZONTAL_FILL = 3;
	private int iconHorizontalAlignment = ICON_HORIZONTAL_LEFT;
	public static final int ICON_VERTICAL_TOP = 0;
	public static final int ICON_VERTICAL_MIDDLE = 1;
	public static final int ICON_VERTICAL_BOTTOM = 2;
	public static final int ICON_VERTICAL_FILL = 3;
	private int iconVerticalAlignment = ICON_VERTICAL_MIDDLE;

	/**
	 * 
	 * @return the last drawn polygon
	 */
	public Polygon getDrawnPolygon() {
		return drawnPolygon;
	}

	/**
	 * Does the actual drawing work.
	 */
	private void drawScaledPolygon(Graphics2D g2d, Point pt, int np,
			double halfWidth, double halfHeight, double[] px, double[] py,
			boolean fill, Color fillColor, Color lineColor) {

		if (px == null || py == null) return;

		int[] xpoints = new int[px.length + 1];
		int[] ypoints = new int[py.length + 1];
		for (int i = 0; i < px.length; i++) {
			xpoints[i] = (int) (pt.x + px[i] * halfWidth);
			ypoints[i] = (int) (pt.y + py[i] * halfHeight);
		}
		xpoints[px.length] = xpoints[0];
		ypoints[px.length] = ypoints[0];

		drawnPolygon = new Polygon(xpoints, ypoints, xpoints.length);
		if (fill) {
			g2d.setPaint(fillColor);
			g2d.fill(drawnPolygon);
		}
		g2d.setPaint(lineColor);
		g2d.draw(drawnPolygon);

	}


	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		//		g.drawLine(0, 0, 10, 10);
		Graphics2D g2d = (Graphics2D) g;
		if ((iconStyle & ICON_STYLE_SYMBOL) != 0) {
			//			draw(g, new Point(c.getWidth() + x - getIconHeight() / 2, y + getIconHeight() / 2),
			//					getIconHeight()-2, getIconHeight()-2, fill, lineThickness,
			//					fillColor, lineColor);
			if (c == null) {
				draw(g, new Point(x + getIconWidth() / 2, getIconHeight() / 2),
						getIconWidth(), getIconHeight(), fill, lineThickness,
						fillColor, lineColor);
			}
			else {
				int w = getWidth();
				int h = getHeight();
				if (w == 0) {
					w = getIconWidth();
				}
				if (h == 0) {
					h = getIconHeight();
				}
				//				x = c.getWidth()/2;
				switch (iconHorizontalAlignment) {
				case ICON_HORIZONTAL_LEFT:
					x = getIconWidth()/2+1;
					break;
				case ICON_HORIZONTAL_RIGHT:
					x = c.getWidth()-getIconWidth()/2-1;
					break;
				case ICON_HORIZONTAL_CENTRE:
					x = c.getWidth()/2;
					break;
				case ICON_HORIZONTAL_FILL:
					x = c.getWidth()/2;
					w = c.getWidth()-2;
					break;
				default:
					x = Math.max(x, getWidth() / 2);						
				}
				switch (iconVerticalAlignment) {
				case ICON_VERTICAL_TOP:
					y = h/2+1;
					break;
				case ICON_VERTICAL_BOTTOM:
					y = c.getHeight() - h/2 - 1;
					break;
				case ICON_VERTICAL_MIDDLE:
					y = c.getHeight()/2;
					break;
				case ICON_VERTICAL_FILL:
					y = c.getHeight()/2;
					h = c.getHeight()-2;
				default:
					y = c.getHeight() / 2;						
				}
				draw(g, new Point(x, y),
						w, h, fill, lineThickness,
						fillColor, lineColor);
			}
		}
		if ((iconStyle & ICON_STYLE_LINE) != 0) {
			g.setColor(this.lineColor);
			g2d.setStroke(new BasicStroke(lineThickness, BasicStroke.CAP_ROUND,
					BasicStroke.JOIN_MITER));
			g.drawLine(x, getIconHeight() / 2, c.getWidth() - getIconHeight() / 2, getIconHeight() / 2);
		}
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub

		switch (symbol) {
		case SYMBOL_NONE:
		case SYMBOL_REGIONSTART:
		case SYMBOL_LINESTART:
		case SYMBOL_LINESEGMENT:
			break;
		case SYMBOL_CROSS:
			return "+ Cross";
		case SYMBOL_CROSS2:
			return "X Cross";
		case SYMBOL_SQUARE:
			return "Square";
		case SYMBOL_TRIANGLEU:
			return "Up Traiangle";
		case SYMBOL_CIRCLE:
			return "Circle";
		case SYMBOL_DIAMOND:
			return "Diamond";
		case SYMBOL_POINT:
			return "Point";
		case SYMBOL_STAR:
			return "Star";
		case SYMBOL_TRIANGLED:
			return "Down Triangle";
		case SYMBOL_TRIANGLEL:
			return "Left Triangle";
		case SYMBOL_TRIANGLER:
			return "Right Triangle";
		case SYMBOL_PENTAGRAM:
			return "Pentagram";
		case SYMBOL_HEXAGRAM:
			return "Hexagram";
		}

		return super.toString();
	}

	/**
	 * @return true if the symbol is a solid shape - e.g. is true
	 * for a circle, but false for a cross.
	 */
	public boolean isSolidShape() {
		// returns true for shapes than can have fill
		switch (symbol) {
		case SYMBOL_SQUARE:
		case SYMBOL_TRIANGLEU:
		case SYMBOL_CIRCLE:
		case SYMBOL_DIAMOND:
		case SYMBOL_POINT:
		case SYMBOL_TRIANGLED:
		case SYMBOL_TRIANGLEL:
		case SYMBOL_TRIANGLER:
		case SYMBOL_PENTAGRAM:
		case SYMBOL_HEXAGRAM:
			return true;
		}

		return false;
	}
	/**
	 * Returns the icon's width.
	 * 
	 * @return an int specifying the fixed width of the icon.
	 */
	public int getIconWidth() {
		int iconWidth = 0;
		if ((iconStyle & ICON_STYLE_SYMBOL) != 0) {
			iconWidth += 16;
		}
		if ((iconStyle & ICON_STYLE_LINE) != 0) {
			iconWidth += 16;
		}
		return Math.max(iconWidth, 16);
		//		return 16;
	}

	/**
	 * Returns the icon's height.
	 * 
	 * @return an int specifying the fixed height of the icon.
	 */
	public int getIconHeight() {
		if ((iconStyle & ICON_STYLE_SYMBOL) != 0) {
			return 16;
		}
		else {
			return 3;
		}
		//		return 16;
	}

	public boolean isFill() {
		return fill;
	}

	public void setFill(boolean fill) {
		this.fill = fill;
	}

	public Color getFillColor() {
		return fillColor;
	}

	public void setFillColor(Color fillColor) {
		this.fillColor = fillColor;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public Color getLineColor() {
		return lineColor;
	}

	public void setLineColor(Color lineColor) {
		this.lineColor = lineColor;
	}

	public int getSymbol() {
		return symbol;
	}

	public void setSymbol(int symbol) {
		this.symbol = symbol;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public float getLineThickness() {
		return lineThickness;
	}

	public void setLineThickness(float lineThickness) {
		this.lineThickness = lineThickness;
	}

	/**
	 * Create a small JPanel to incorporate into 
	 * a key. The component will contain a small panel
	 * on the left with a symbol drawn in it and a panel
	 * on the right with the text as a JLabel.
	 * @param text
	 * @return Java component to include in a key
	 */
	public PamKeyItem makeKeyItem(String text) {
		return new SymbolKeyItem(this, text);
	}

	private class KeyPanel extends JPanel implements ColorManaged {
		static private final int size = 16;
		KeyPanel() {
			setPreferredSize(new Dimension(size,size));
			//			PamColors.getInstance().registerComponent(this, PamColor.PlOTWINDOW);
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			draw(g, new Point(size/2, size/2), size-4, size-4);
		}

		private PamColor defaultColor = PamColor.PlOTWINDOW;

		public PamColor getDefaultColor() {
			return defaultColor;
		}

		public void setDefaultColor(PamColor defaultColor) {
			this.defaultColor = defaultColor;
		}

		@Override
		public PamColor getColorId() {
			return defaultColor;
		}
	}


	@Override
	public PamSymbol clone() {
		// TODO Auto-generated method stub
		try {
			return (PamSymbol) super.clone();
		} catch (CloneNotSupportedException Ex) {
			Ex.printStackTrace();
		}
		return null;
	}

	public static Rectangle drawArrow(Graphics g, int x1, int y1, int x2, int y2, int headSize) {
		return drawArrow(g, x1, y1, x2, y2, headSize, 45, false);
	}

	public static Rectangle drawArrow(Graphics g, double x1, double y1, double x2, double y2, double headSize, double headAngle, boolean doubleEnded) {
		Rectangle r = new Rectangle((int)Math.min(x1, x2), (int)Math.min(y1, y2), (int)Math.abs(x1-x2),(int) Math.abs(y2-y1));
		g.drawLine((int) x1, (int)y1, (int)x2, (int)y2);

		double arrowDir = Math.atan2(y2-y1, x2-x1);
		double x3, y3;
		double newDir;
		newDir = arrowDir + (180-headAngle) * Math.PI/180;
		x3 = x2 +  (headSize * Math.cos(newDir));
		y3 = y2 +  (headSize * Math.sin(newDir));
		g.drawLine((int) x2, (int) y2, (int) x3, (int) y3);
		newDir = arrowDir - (180-headAngle) * Math.PI/180;
		x3 = x2 + (headSize * Math.cos(newDir));
		y3 = y2 + (headSize * Math.sin(newDir));
		g.drawLine((int) x2, (int) y2, (int) x3, (int) y3);
		if (doubleEnded) {
			arrowDir = arrowDir + Math.PI;
			newDir = arrowDir + (180-headAngle) * Math.PI/180;
			x3 = x1 + (int) (headSize * Math.cos(newDir));
			y3 = y1 + (int) (headSize * Math.sin(newDir));
			g.drawLine((int) x1, (int) y1, (int) x3, (int) y3);
			newDir = arrowDir - (180-headAngle) * Math.PI/180;
			x3 = x1 + (int) (headSize * Math.cos(newDir));
			y3 = y1 + (int) (headSize * Math.sin(newDir));
			g.drawLine((int) x1, (int) y1, (int) x3, (int) y3);
		}

		return r;
	}

	public double[] getXPoints() {
		return null;
	}

	public double[] getYPoints() {
		return null;
	}

	/**
	 * @return the iconStyle
	 */
	public int getIconStyle() {
		return iconStyle;
	}

	/**
	 * @param iconStyle the iconStyle to set
	 */
	public void setIconStyle(int iconStyle) {
		this.iconStyle = iconStyle;
	}

	/**
	 * 
	 * @param hAlignment the icon horizontal alignment
	 */
	public void setIconHorizontalAlignment(int hAlignment) {
		this.iconHorizontalAlignment = hAlignment;
	}

	/**
	 * @return the iconVerticalAlignment
	 */
	public int getIconVerticalAlignment() {
		return iconVerticalAlignment;
	}

	/**
	 * @param iconVerticalAlignment the iconVerticalAlignment to set
	 */
	public void setIconVerticalAlignment(int iconVerticalAlignment) {
		this.iconVerticalAlignment = iconVerticalAlignment;
	}

	/**
	 * @return the iconHorizontalAlignment
	 */
	public int getIconHorizontalAlignment() {
		return iconHorizontalAlignment;
	}
}
