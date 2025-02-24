package com.jpprade.jcgmtosvg.extension;

import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.Stroke;

import org.apache.batik.svggen.DOMGroupManager;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.Element;

public class SVGGraphics2DHS extends SVGGraphics2D {

	private static final String HOTSPOT_COLOR = "transparent";
	private static final String SANITIZER_REGEX = "[^a-zA-Z0-9-_.:,/() ]";
	
	public SVGGraphics2DHS(SVGGeneratorContext generatorCtx,
	                       boolean textAsShapes) {
		super(generatorCtx, textAsShapes);
	}

	public void drawHotSpot(Shape s, String id, String apsName) {
		drawHotSpot(s, id, id, apsName, null, null);
	}
	
	public void drawHotSpot(Shape s, String id, String apsId, String apsName, String link, String color) {
		// Only BasicStroke can be converted to an SVG attribute equivalent.
		// If the GraphicContext's Stroke is not an instance of BasicStroke,
		// then the stroked outline is filled.
		Stroke stroke = this.getGraphicContext().getStroke();
		if (stroke instanceof BasicStroke) {
			Element svgShape = this.shapeConverter.toSVG(s);
			if (svgShape != null) {
				enrichHS(svgShape, id, apsId, apsName, link, color);
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
		Stroke stroke = this.getGraphicContext().getStroke();
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
	
	private void enrichHS(Element svgShape, String id, String apsId, String apsName, String link, String color) {

		id = hotSpotAttributeSanitizer(id);
		apsName = hotSpotAttributeSanitizer(apsName);
		apsId = hotSpotAttributeSanitizer(apsId);
		link = hotSpotAttributeSanitizer(link);

		String hotSpotRectangle = svgShape.getAttribute("x")+","+svgShape.getAttribute("y")+","+svgShape.getAttribute("width")+","+svgShape.getAttribute("height");
		String hotSpotLink = (link != null) ? "window.location.href='"+link+"?id="+apsId+"&name="+apsName+"&rect=["+hotSpotRectangle+"]'" : "";
		String hotSpotColor = (color != null) ? color : HOTSPOT_COLOR;
		
		hotSpotRectangle = hotSpotAttributeSanitizer(hotSpotRectangle);
		hotSpotColor = hotSpotAttributeSanitizer(hotSpotColor);		

		svgShape.setAttributeNS(null, "id", id);
		svgShape.setAttributeNS(null, "apsname", apsName);
		svgShape.setAttributeNS(null, "apsid", apsId);
		svgShape.setAttributeNS(null, "fill-rule", "evenodd");
		svgShape.setAttributeNS(null, "fill", hotSpotColor);
		svgShape.setAttributeNS(null, "class", "hotspot");
		svgShape.setAttributeNS(null, "onclick", "clickHS('" + id + "');"+hotSpotLink);
		svgShape.setAttributeNS(null, "stroke", "none");
	}
	
	private void enrichTDET(Element svgShape, String apsId, String apsName) {

		apsName = hotSpotAttributeSanitizer(apsName);
		apsId = hotSpotAttributeSanitizer(apsId);

		svgShape.setAttributeNS(null, "apsname", apsName);
		svgShape.setAttributeNS(null, "apsid", apsId);
		svgShape.setAttributeNS(null, "class", "tdet");
	}

	private String hotSpotAttributeSanitizer(String hotSpotAttribute) {
		if(hotSpotAttribute == null) {
			return "";
		}
		return hotSpotAttribute.replaceAll(SANITIZER_REGEX, "");
		
	}
	
}
