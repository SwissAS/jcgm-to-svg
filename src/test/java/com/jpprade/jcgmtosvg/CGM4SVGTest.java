package com.jpprade.jcgmtosvg;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.jpprade.jcgmtosvg.extension.SVGGraphics2DHS;

import net.sf.jcgm.core.CGMDisplay;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

class CGM4SVGTest {
    private CGM4SVG cgm4svg;
    private SVGPainter mockPainter;
    private Map<String, Object> options;

    @BeforeEach
    void setUp() throws IOException {
        mockPainter = Mockito.mock(SVGPainter.class);
        options = new HashMap<>();
        byte[] emptyCgmData = new byte[0];
        ByteArrayInputStream inputStream = new ByteArrayInputStream(emptyCgmData);
        cgm4svg = new CGM4SVG(inputStream, mockPainter, options);
    }

    @Test
    void testConstructorWithFile() throws IOException {
        File tempFile = File.createTempFile("test", ".cgm");
        CGM4SVG cgmFromFile = new CGM4SVG(tempFile, mockPainter);
        assertNotNull(cgmFromFile);
    }

    @Test
    void testConstructorWithInputStream() {
        assertNotNull(cgm4svg);
    }

    @Test
    void testOptionsAreSetCorrectly() {
        options.put("hotSpotEnabled", true);
        CGM4SVG newCgm4svg;
        try {
            newCgm4svg = new CGM4SVG(new ByteArrayInputStream(new byte[0]), mockPainter, options);
            assertTrue((boolean) newCgm4svg.options.get("hotSpotEnabled"));
        } catch (IOException e) {
            e.printStackTrace();
        }
   }

   @Test
    void testAddOffset() throws Exception {
        CGMDisplay mockDisplay = Mockito.mock(CGMDisplay.class);
        Rectangle2D.Double shape = new Rectangle2D.Double(10, 20, 30, 40);
        Point2D offset = new Point2D.Double(5, 10);

        // Accéder à la méthode privée via la réflexion
        Method method = CGM4SVG.class.getDeclaredMethod("addOffset", CGMDisplay.class, Rectangle2D.Double.class, Point2D.class);
        method.setAccessible(true);
        
        Rectangle2D.Double result = (Rectangle2D.Double) method.invoke(cgm4svg, mockDisplay, shape, offset);

        assertEquals(15, result.x);
        assertEquals(10, result.y);
        assertEquals(30, result.width);
        assertEquals(40, result.height);
    }

    @Test
    void testAddPadding() throws Exception {
        CGMDisplay mockDisplay = Mockito.mock(CGMDisplay.class);
        SVGGraphics2DHS mockGraphics = Mockito.mock(SVGGraphics2DHS.class);
        AffineTransform mockTransform = new AffineTransform();
        mockTransform.scale(2, 2);

        Mockito.when(mockDisplay.getGraphics2D()).thenReturn(mockGraphics);
        Mockito.when(mockGraphics.getTransform()).thenReturn(mockTransform);

        options.put("hotSpotPadding", 10.0);
        Rectangle2D.Double shape = new Rectangle2D.Double(10, 20, 30, 40);

        Method method = CGM4SVG.class.getDeclaredMethod("addPadding", CGMDisplay.class, Rectangle2D.Double.class);
        method.setAccessible(true);
        
        Rectangle2D.Double result = (Rectangle2D.Double) method.invoke(cgm4svg, mockDisplay, shape);

        assertEquals(5, result.x);
        assertEquals(15, result.y);
        assertEquals(40, result.width);
        assertEquals(50, result.height);
    }

    @Test
    void testAddNoPadding() throws Exception {
        CGMDisplay mockDisplay = Mockito.mock(CGMDisplay.class);
        SVGGraphics2DHS mockGraphics = Mockito.mock(SVGGraphics2DHS.class);
        AffineTransform mockTransform = new AffineTransform();
        mockTransform.scale(2, 2);

        Mockito.when(mockDisplay.getGraphics2D()).thenReturn(mockGraphics);
        Mockito.when(mockGraphics.getTransform()).thenReturn(mockTransform);

        Rectangle2D.Double shape = new Rectangle2D.Double(10, 20, 30, 40);

        Method method = CGM4SVG.class.getDeclaredMethod("addPadding", CGMDisplay.class, Rectangle2D.Double.class);
        method.setAccessible(true);
        
        Rectangle2D.Double result = (Rectangle2D.Double) method.invoke(cgm4svg, mockDisplay, shape);

        assertEquals(10, result.x);
        assertEquals(20, result.y);
        assertEquals(30, result.width);
        assertEquals(40, result.height);
    }
}