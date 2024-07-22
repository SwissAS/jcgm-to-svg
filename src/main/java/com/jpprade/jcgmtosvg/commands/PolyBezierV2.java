package com.jpprade.jcgmtosvg.commands;

import com.jpprade.jcgmtosvg.extension.SVGGraphics2DHS;
import net.sf.jcgm.core.BeginFigure;
import net.sf.jcgm.core.CGMDisplay;
import net.sf.jcgm.core.EdgeColour;
import net.sf.jcgm.core.EdgeWidth;
import net.sf.jcgm.core.FillColour;
import net.sf.jcgm.core.InteriorStyle;
import net.sf.jcgm.core.InteriorStyle.Style;
import net.sf.jcgm.core.PolyBezier;

import java.awt.*;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;

public class PolyBezierV2 extends ExtendedCommand {
	
	/**
	 * The scales curves to draw
	 */
	private CubicCurve2D.Double[] curves;
	
	private Point2D.Double[] p1;
	private Point2D.Double[] p2;
	private Point2D.Double[] p3;
	private Point2D.Double[] p4;
	
	public PolyBezierV2(PolyBezier polyBezier) {
		this.p1 = polyBezier.getP1();
		this.p2 = polyBezier.getP2();
		this.p3 = polyBezier.getP3();
		this.p4 = polyBezier.getP4();
	}
	
	public void initCurves() {
		this.curves = new CubicCurve2D.Double[this.p1.length];
		
		for (int i = 0; i < this.p1.length; i++) {
			CubicCurve2D.Double c = new CubicCurve2D.Double();
			c.setCurve(this.p1[i].x, this.p1[i].y,
					this.p2[i].x, this.p2[i].y, this.p3[i].x, this.p3[i].y, this.p4[i].x, this.p4[i].y);
			this.curves[i] = c;
		}
	}
	
	public void paint(CGMDisplay d, BeginFigure figure) {
		this.paint(d, figure, null, null, null);
	}
	
	public void paint(CGMDisplay d, BeginFigure figure, FillColour currentFC, EdgeColour currentEC, EdgeWidth currentEW) {
		int mode = figure == null ? 0 : 1;
		if (mode == 0) {
			if (this.curves == null) {
				initCurves();
			}
			
			SVGGraphics2DHS g2d = (SVGGraphics2DHS) d.getGraphics2D();
			g2d.setStroke(d.getLineStroke());
			g2d.setColor(d.getLineColor());
			
			GeneralPath gp = new GeneralPath();
			for (Shape curve : this.curves) {
				gp.append(curve, true);
			}
			
			drawCustom(g2d, gp);
		} else {
			SVGGraphics2DHS g2d = (SVGGraphics2DHS) d.getGraphics2D();
			if (currentEC != null || currentEW != null) {
				g2d.setStroke(d.getEdgeStroke());
				g2d.setColor(d.getEdgeColor());
			} else {
				g2d.setStroke(d.getLineStroke());
				g2d.setColor(d.getLineColor());
			}
			
			GeneralPath gp = new GeneralPath();
			
			for (int i = 0; i < this.p1.length; i++) {
				if (i == 0) {
					gp.moveTo(this.p1[i].x, this.p1[i].y);
				}
				
				gp.curveTo(this.p2[i].x, this.p2[i].y, this.p3[i].x, this.p3[i].y, this.p4[i].x, this.p4[i].y);
				
				if (i == this.p1.length - 1) {
					gp.closePath();
				}
			}
			
			Style s = d.getInteriorStyle();
			if (currentFC != null) {
				if (InteriorStyle.Style.HATCH.equals(s)) {
					drawHatch(gp, g2d, d.getFillColor(), d.getHatchType());
					drawCustom(g2d, gp);
				} else if (InteriorStyle.Style.EMPTY.equals(s)) {
					drawCustom(g2d, gp);
				} else {
					Color fColor = d.getFillColor();
					g2d.setPaint(fColor);
					g2d.fill(gp);
					if (d.drawEdge()) {
						g2d.setColor(d.getEdgeColor());
						g2d.setStroke(d.getEdgeStroke());
						drawCustom(g2d, gp);
					}
				}
				
			} else {
				if (InteriorStyle.Style.SOLID.equals(s)) {
					Color fColor = d.getFillColor();
					g2d.setPaint(fColor);
					g2d.fill(gp);
					g2d.setStroke(d.getLineStroke());
					g2d.setColor(d.getLineColor());
					if (d.drawEdge()) {
						g2d.setColor(d.getEdgeColor());
						g2d.setStroke(d.getEdgeStroke());
						drawCustom(g2d, gp);
					}
				} else {
					drawCustom(g2d, gp);
				}
			}
		}
	}
	
	private void drawCustom(SVGGraphics2DHS g2d, Shape gp) {
		g2d.draw(gp);
	}
	
	public Point2D.Double[] getP1() {
		return this.p1.clone();
	}
	
	public Point2D.Double[] getP2() {
		return this.p2.clone();
	}
	
	public Point2D.Double[] getP3() {
		return this.p3.clone();
	}
	
	public Point2D.Double[] getP4() {
		return this.p4.clone();
	}
	
	private Point2D.Double[] concatArray(Point2D.Double[] a1, Point2D.Double[] a2) {
		Point2D.Double[] ret = new Point2D.Double[a1.length + a2.length];
		int index = 0;
		for (Double aDouble : a1) {
			ret[index] = aDouble;
			index++;
		}
		for (Double aDouble : a2) {
			ret[index] = aDouble;
			index++;
		}
		return ret;
	}
	
	public void mergeShape(PolyBezierV2 polyBezierV2) {
		this.p1 = concatArray(this.p1, polyBezierV2.getP1());
		this.p2 = concatArray(this.p2, polyBezierV2.getP2());
		this.p3 = concatArray(this.p3, polyBezierV2.getP3());
		this.p4 = concatArray(this.p3, polyBezierV2.getP4());
	}
	
}
