package com.jpprade.jcgmtosvg.extension;

import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.Stroke;

import org.apache.batik.svggen.DOMGroupManager;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.Element;

public class SVGGraphics2DHS extends SVGGraphics2D {
	
	public SVGGraphics2DHS(SVGGeneratorContext generatorCtx,
	                       boolean textAsShapes) {
		super(generatorCtx, textAsShapes);
	}

	public void drawHotSpot(Shape s, String apsId, String apsName) {
		drawHotSpot(s, apsId, apsName, null);
	}
	
	public void drawHotSpot(Shape s, String apsId, String apsName, String link) {
		// Only BasicStroke can be converted to an SVG attribute equivalent.
		// If the GraphicContext's Stroke is not an instance of BasicStroke,
		// then the stroked outline is filled.
		Stroke stroke = this.gc.getStroke();
		if (stroke instanceof BasicStroke) {
			Element svgShape = this.shapeConverter.toSVG(s);
			if (svgShape != null) {
				enrichHS(svgShape, apsId, apsName, link);
				this.domGroupManager.addElement(svgShape, DOMGroupManager.DRAW);
			}
		} else {
			Shape strokedShape = stroke.createStrokedShape(s);
			fill(strokedShape);
		}
	}
	
	public void drawTDET(Shape s, String apsId, String apsName) {
		// Only BasicStroke can be converted to an SVG attribute equivalent.
		// If the GraphicContext's Stroke is not an instance of BasicStroke,
		// then the stroked outline is filled.
		Stroke stroke = this.gc.getStroke();
		if (stroke instanceof BasicStroke) {
			Element svgShape = this.shapeConverter.toSVG(s);
			if (svgShape != null) {
				enrichTDET(svgShape, apsId, apsName);
				this.domGroupManager.addElement(svgShape, DOMGroupManager.DRAW);
			}
		} else {
			Shape strokedShape = stroke.createStrokedShape(s);
			fill(strokedShape);
		}
	}
	
	private void enrichHS(Element svgShape, String apsId, String apsName, String link) {

		String hotSpotLink = "";

		if(link != null){
			hotSpotLink ="window.location.href='"+link+"?id="+apsId+"&name="+apsName+"'";
		}

		svgShape.setAttributeNS(null, "id", apsId);
		svgShape.setAttributeNS(null, "apsname", apsName);
		svgShape.setAttributeNS(null, "apsid", apsId);
		svgShape.setAttributeNS(null, "fill-rule", "evenodd");
		svgShape.setAttributeNS(null, "fill", "rgba(29, 99, 165, 0.5)");
		svgShape.setAttributeNS(null, "class", "hotspot");
		svgShape.setAttributeNS(null, "onclick", "clickHS('" + apsId + "');"+hotSpotLink);
		svgShape.setAttributeNS(null, "stroke-width", "0");
	}
	
	private void enrichTDET(Element svgShape, String apsId, String apsName) {
		svgShape.setAttributeNS(null, "apsname", apsName);
		svgShape.setAttributeNS(null, "apsid", apsId);
		svgShape.setAttributeNS(null, "class", "tdet");
	}
	
}
