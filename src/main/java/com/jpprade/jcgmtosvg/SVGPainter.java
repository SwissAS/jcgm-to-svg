package com.jpprade.jcgmtosvg;

import com.jpprade.jcgmtosvg.extension.SVGGraphics2DHS;
import net.sf.jcgm.core.ApplicationStructureAttribute;
import net.sf.jcgm.core.CGMDisplay;
import net.sf.jcgm.core.Member;
import net.sf.jcgm.core.StructuredDataRecord;
import net.sf.jcgm.core.workaround.Graphics2DDecorator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

public class SVGPainter {
	
	private static final Logger logger = LoggerFactory.getLogger(SVGPainter.class);
	
	public void paint(ApplicationStructureAttribute aps, CGMDisplay d, PaintHolder ph, Dimension dimension) {
		String attributeType = aps.getApplicationStructureAttributeType();
		StructuredDataRecord structuredDataRecord = aps.getSdr();
		
		if ("region".equals(attributeType)) {
			List<Member> members = structuredDataRecord.getMembers();
			
			if (members != null && members.size() == 2 && members.get(0).getCount() > 0) {
				if (members.get(0).getData().getFirst().toString().equals("4")) {//polybezier
					List<Double> objects = (List<Double>) (Object) members.get(1).getData();
					
					int n = (objects.size() - 2) / 6;
					
					Point2D.Double[] p1;
					Point2D.Double[] p2;
					Point2D.Double[] p3;
					Point2D.Double[] p4;
					
					p1 = new Point2D.Double[n];
					p2 = new Point2D.Double[n];
					p3 = new Point2D.Double[n];
					p4 = new Point2D.Double[n];
					
					int point = 0;
					int pos = 0;
					while (point < n) {
						if (point == 0) {
							p1[point] = new Point2D.Double(objects.get(pos), objects.get(pos + 1));
							pos += 2;
						} else {
							p1[point] = p4[point - 1];
						}
						p2[point] = new Point2D.Double(objects.get(pos), objects.get(pos + 1));
						pos += 2;
						p3[point] = new Point2D.Double(objects.get(pos), objects.get(pos + 1));
						pos += 2;
						p4[point] = new Point2D.Double(objects.get(pos), objects.get(pos + 1));
						pos += 2;
						point++;
					}

					SVGGraphics2DHS g2d;
					if (d.getGraphics2D() instanceof Graphics2DDecorator decorator) {
						g2d = (SVGGraphics2DHS) decorator.getDelegate();
					} else {
						g2d = (SVGGraphics2DHS) d.getGraphics2D();
					}
					g2d.setStroke(d.getLineStroke());
					g2d.setColor(d.getLineColor());
					
					
					//svgGenerator.getTopLevelGroup().setAttribute("id", "whatever")
					
					GeneralPath gp = new GeneralPath();
					
					for (int i = 0; i < p1.length; i++) {
						if (i == 0) {
							gp.moveTo(p1[i].x, p1[i].y);
						}
						
						gp.curveTo(p2[i].x, p2[i].y, p3[i].x, p3[i].y, p4[i].x, p4[i].y);
						
						if (i == p1.length - 1) {
							gp.closePath();
						}
					}
					
					g2d.drawHotSpot(gp, ph.getApsid(), ph.getName());
					
					
					//Element e = g2d.
				} else if (members.get(0).getData().getFirst().toString().equals("1")) {//rectangle
					List<Double> objects = (List<Double>) (Object) members.get(1).getData();
					if (objects.size() != 4) {
						return;
					}
					
					double x1 = objects.get(0);
					double y1 = objects.get(1);
					double x2 = objects.get(2);
					double y2 = objects.get(3);
					
					if (x1 > x2) {
						double temp = x1;
						x1 = x2;
						x2 = temp;
					}
					
					if (y1 > y2) {
						double temp = y1;
						y1 = y2;
						y2 = temp;
					}
					
					double w = x2 - x1;
					double h = y2 - y1;
					
					Rectangle2D.Double shape = new Rectangle2D.Double(x1, y1, w, h);

					SVGGraphics2DHS g2d;
					if (d.getGraphics2D() instanceof Graphics2DDecorator decorator) {
						g2d = (SVGGraphics2DHS) decorator.getDelegate();
					} else {
						g2d = (SVGGraphics2DHS) d.getGraphics2D();
					}
					
					g2d.setColor(d.getEdgeColor());
					g2d.setStroke(d.getEdgeStroke());
					g2d.drawHotSpot(shape, ph.getApsid(), ph.getName());
				} else if (members.get(0).getData().getFirst().toString().equals("3")) {//polygon
					List<Double> objects = (List<Double>) (Object) members.get(1).getData();
					Path2D.Double polygon = new Path2D.Double(Path2D.WIND_EVEN_ODD);
					SVGGraphics2DHS g2d;
					if (d.getGraphics2D() instanceof Graphics2DDecorator decorator) {
						g2d = (SVGGraphics2DHS) decorator.getDelegate();
					} else {
						g2d = (SVGGraphics2DHS) d.getGraphics2D();
					}
					
					for (int i = 0; i < objects.size(); i = i + 2) {
						if (i == 0) {
							polygon.moveTo(objects.get(i), objects.get(i + 1));
						} else {
							polygon.lineTo(objects.get(i), objects.get(i + 1));
						}
					}
					polygon.closePath();
					
					g2d.setColor(d.getEdgeColor());
					g2d.setStroke(d.getEdgeStroke());
					g2d.drawHotSpot(polygon, ph.getApsid(), ph.getName());
					
				} else {
					logger.warn("unhandled HS type: {}", members.getFirst().getData().getFirst());
				}
				
			}
		} else if ("name".equals(attributeType)) {
			/*
			List<Member> members = structuredDataRecord.getMembers();
			if(members!=null && members.size() ==1) {
				if(members.get(0).getCount() > 0) {
					if(members.get(0).getData() != null 
							&& members.get(0).getData().size() > 0
							&& members.get(0).getData().get(0) instanceof String) {
						this.currentApsName = (String) members.get(0).getData().get(0);
					}
				}
			}*/
			
		} else if ("viewcontext".equals(attributeType)) {
			List<Member> members = structuredDataRecord.getMembers();
			
			if (members != null && members.size() == 1) {
				Member first = members.getFirst();
				if (first.getCount() == 4) {
					List<Double> objects = (List<Double>) (Object) first.getData();
					
					double x1 = objects.get(0);
					double y1 = objects.get(1);
					double x2 = objects.get(2);
					double y2 = objects.get(3);
					
					if (x1 > x2) {
						double temp = x1;
						x1 = x2;
						x2 = temp;
					}
					
					if (y1 > y2) {
						double temp = y1;
						y1 = y2;
						y2 = temp;
					}
					
					double w = x2 - x1;
					double h = y2 - y1;
					
					if (x1 == Double.NEGATIVE_INFINITY) {
						x1 = 0;
					}
					if (y1 == Double.NEGATIVE_INFINITY) {
						y1 = 0;
					}
					if (w == Double.POSITIVE_INFINITY) {
						w = dimension.getWidth();
					}
					if (h == Double.POSITIVE_INFINITY) {
						h = dimension.getHeight();
					}
					
					Rectangle2D.Double shape = new Rectangle2D.Double(x1, y1, w, h);

					SVGGraphics2DHS g2d;
					if (d.getGraphics2D() instanceof Graphics2DDecorator decorator) {
						g2d = (SVGGraphics2DHS) decorator.getDelegate();
					} else {
						g2d = (SVGGraphics2DHS) d.getGraphics2D();
					}
					
					Color trans = new Color(255, 255, 255, 255);
					
					g2d.setColor(trans);
					g2d.setStroke(d.getEdgeStroke());
					g2d.drawTDET(shape, ph.getApsid(), ph.getName());
					
				}
			}
		}
	}
}
