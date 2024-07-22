package com.jpprade.jcgmtosvg.commands;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.jpprade.jcgmtosvg.extension.SVGGraphics2DHS;

import net.sf.jcgm.core.HatchIndex.HatchType;

public class ExtendedCommand {
	
	private static final double STEP_X = 1.41;
	private static final double STEP_Y = 1.41;
	
	public void drawHatch(Shape s, SVGGraphics2DHS g2d, Color fillColor, HatchType hatchType) {
		// remember the clip and the stroke since we're overwriting them here
		Shape previousClippingArea = g2d.getClip();
		Stroke previousStroke = g2d.getStroke();
		
		Rectangle2D bounds = s.getBounds2D();
		g2d.setClip(s);
		
		g2d.setStroke(new BasicStroke(0.2f));
		
		g2d.setColor(fillColor);
		
		final double slopeStep = STEP_X * 1.41;
		
		if (HatchType.HORIZONTAL_LINES.equals(hatchType)) {
			drawHorizontalLines(bounds, STEP_Y, g2d);
		} else if (HatchType.VERTICAL_LINES.equals(hatchType)) {
			drawVerticalLines(bounds, STEP_X, g2d);
		} else if (HatchType.POSITIVE_SLOPE_LINES.equals(hatchType)) {
			drawPositiveSlopeLines(bounds, slopeStep, g2d);
		} else if (HatchType.NEGATIVE_SLOPE_LINES.equals(hatchType)) {
			drawNegativeSlopeLines(bounds, slopeStep, g2d);
		} else if (HatchType.HORIZONTAL_VERTICAL_CROSSHATCH.equals(hatchType)) {
			drawHorizontalLines(bounds, STEP_Y, g2d);
			drawVerticalLines(bounds, STEP_X, g2d);
		} else if (HatchType.POSITIVE_NEGATIVE_CROSSHATCH.equals(hatchType)) {
			drawPositiveSlopeLines(bounds, slopeStep, g2d);
			drawNegativeSlopeLines(bounds, slopeStep, g2d);
		}
		
		// restore the previous clipping area and stroke
		g2d.setClip(previousClippingArea);
		g2d.setStroke(previousStroke);
	}
	
	
	private void drawVerticalLines(Rectangle2D bounds, final double stepX, SVGGraphics2DHS g2d) {
		for (double x = bounds.getX(); x < bounds.getX() + bounds.getWidth(); x += stepX) {
			g2d.draw(new Line2D.Double(x, bounds.getY(), x, bounds.getY() + bounds.getHeight()));
		}
	}
	
	private void drawHorizontalLines(Rectangle2D bounds, final double stepY, SVGGraphics2DHS g2d) {
		for (double y = bounds.getY(); y < bounds.getY() + bounds.getHeight(); y += stepY) {
			g2d.draw(new Line2D.Double(bounds.getX(), y, bounds.getX() + bounds.getWidth(), y));
		}
	}
	
	private void drawPositiveSlopeLines(Rectangle2D bounds, final double slopeStep, SVGGraphics2DHS g2d) {
		Point2D.Double currentBegin = new Point2D.Double(bounds.getX(), bounds.getY() + bounds.getHeight());
		Point2D.Double currentEnd = currentBegin;
		
		boolean done = false;
		while (!done) {
			// move begin
			if (currentBegin.y > bounds.getY()) {
				// move the begin down the Y axis
				currentBegin = new Point2D.Double(currentBegin.x, currentBegin.y - slopeStep);
			} else {
				// move the begin right the X axis
				currentBegin = new Point2D.Double(currentBegin.x + slopeStep, currentBegin.y);
			}
			
			// move end
			if (currentEnd.x < bounds.getX() + bounds.getWidth()) {
				// move end right the X axis
				currentEnd = new Point2D.Double(currentEnd.x + slopeStep, currentEnd.y);
			} else {
				// move end down the Y axis
				currentEnd = new Point2D.Double(currentEnd.x, currentEnd.y - slopeStep);
			}
			
			g2d.draw(new Line2D.Double(currentBegin.x, currentBegin.y, currentEnd.x, currentEnd.y));
			
			if (currentBegin.x > bounds.getX() + bounds.getWidth() || currentEnd.getY() < bounds.getY()) {
				done = true;
			}
		}
	}
	
	private void drawNegativeSlopeLines(Rectangle2D bounds, final double slopeStep, SVGGraphics2DHS g2d) {
		Point2D.Double currentBegin = new Point2D.Double(bounds.getX(), bounds.getY());
		Point2D.Double currentEnd = currentBegin;
		
		boolean done = false;
		while (!done) {
			// move begin
			if (currentBegin.y < bounds.getY() + bounds.getHeight()) {
				// move the begin up the Y axis
				currentBegin = new Point2D.Double(currentBegin.x, currentBegin.y + slopeStep);
			} else {
				// move the begin right the X axis
				currentBegin = new Point2D.Double(currentBegin.x + slopeStep, currentBegin.y);
			}
			
			// move end
			if (currentEnd.x < bounds.getX() + bounds.getWidth()) {
				// move end right the X axis
				currentEnd = new Point2D.Double(currentEnd.x + slopeStep, currentEnd.y);
			} else {
				// move end up the Y axis
				currentEnd = new Point2D.Double(currentEnd.x, currentEnd.y + slopeStep);
			}
			
			g2d.draw(new Line2D.Double(currentBegin.x, currentBegin.y, currentEnd.x, currentEnd.y));
			
			if (currentBegin.x > bounds.getX() + bounds.getWidth() || currentEnd.getY() < bounds.getY()) {
				done = true;
			}
		}
	}
	
}
