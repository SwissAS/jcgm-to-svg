package com.jpprade.jcgmtosvg.extension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.awt.BasicStroke;
import java.awt.GraphicsConfiguration;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.Field;

import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.ext.awt.g2d.GraphicContext;
import org.apache.batik.svggen.DOMGroupManager;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.svggen.SVGShape;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

class SVGGraphics2DHSTest {

    private SVGGraphics2DHS svgGraphics;
    private SVGGeneratorContext mockContext;
    private DOMGroupManager mockGroupManager;
    private SVGShape mockShapeConverter;
    private Shape mockShape;
    private Shape defaultRectangle;
    private Element mockElement;
    private Stroke mockStroke;
    private GraphicContext mockGraphicContext;

    @BeforeEach
    void setUp() throws Exception {
        // Get a DOMImplementation.
		DOMImplementation domImpl = SVGDOMImplementation.getDOMImplementation();
		
		// Create an instance of org.w3c.dom.Document.
		Document document = domImpl.createDocument(SVGDOMImplementation.SVG_NAMESPACE_URI, "svg", null);
        mockContext = SVGGeneratorContext.createDefault(document);
        
        mockGroupManager = mock(DOMGroupManager.class);
        mockShapeConverter = mock(SVGShape.class);
        mockShape = mock(Shape.class);
        mockElement = mock(Element.class);
        mockStroke = mock(Stroke.class);
        mockGraphicContext = mock(GraphicContext.class); 

        defaultRectangle = new Rectangle(10, 10, 50, 50);

        svgGraphics = spy(new SVGGraphics2DHS(mockContext, true));
        setPrivateField(svgGraphics, "domGroupManager", mockGroupManager);
        setPrivateField(svgGraphics, "shapeConverter", mockShapeConverter);
    }

    private void setPrivateField(Object object, String fieldName, Object value) throws Exception {
        Field field = object.getClass().getSuperclass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(object, value);
    }


    @Test
    void testDrawHotSpot_WithBasicStroke() {
        doReturn(new BasicStroke()).when(svgGraphics).getStroke();
        when(mockShapeConverter.toSVG(any(Shape.class))).thenReturn(mockElement);

        svgGraphics.drawHotSpot(defaultRectangle, "hotspot1", "aps1", "Test Hotspot", "http://example.com", "red");

        verify(mockElement).setAttributeNS(null, "id", "hotspot1");
        verify(mockGroupManager).addElement(mockElement, DOMGroupManager.DRAW);
    }

    @Test
    void testDrawHotSpot_WithNonBasicStroke() {
        when((svgGraphics).getGraphicContext()).thenReturn(mockGraphicContext);
        when((svgGraphics).getGraphicContext().getStroke()).thenReturn(mockStroke);
        when(mockStroke.createStrokedShape(any(Shape.class))).thenReturn(mockShape);

        svgGraphics.drawHotSpot(defaultRectangle, "hotspot2", "aps2", "Another Hotspot", "http://example.com", "blue");

        verify(svgGraphics).fill(mockShape);
    }

    @Test
    void testDrawTDET_WithBasicStroke() {
        doReturn(new BasicStroke()).when(svgGraphics).getStroke();
        when(mockShapeConverter.toSVG(any(Shape.class))).thenReturn(mockElement);

        svgGraphics.drawTDET(defaultRectangle, "aps3", "Test TDET");

        verify(mockElement).setAttributeNS(null, "apsname", "Test TDET");
        verify(mockGroupManager).addElement(mockElement, DOMGroupManager.DRAW);
    }

    @Test
    void testDrawTDET_WithNonBasicStroke() {
        when((svgGraphics).getGraphicContext()).thenReturn(mockGraphicContext);
        when((svgGraphics).getGraphicContext().getStroke()).thenReturn(mockStroke);
        when(mockStroke.createStrokedShape(any(Shape.class))).thenReturn(mockShape);

        svgGraphics.drawTDET(defaultRectangle, "aps4", "Another TDET");

        verify(svgGraphics).fill(mockShape);
    }
}
