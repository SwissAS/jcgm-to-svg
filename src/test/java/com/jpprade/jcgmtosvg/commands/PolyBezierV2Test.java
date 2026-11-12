package com.jpprade.jcgmtosvg.commands;

import net.sf.jcgm.core.CGM;
import net.sf.jcgm.core.CGMDisplay;
import net.sf.jcgm.core.PolyBezier;
import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.junit.jupiter.api.Test;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import com.jpprade.jcgmtosvg.extension.SVGGraphics2DHS;
import net.sf.jcgm.core.BeginFigure;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PolyBezierV2Test {

	@Test
	void mergeShapeConcatenatesAllEndpointArrays() {
		PolyBezierV2 first = polyBezier(
				points(0, 0), points(1, 1), points(2, 1), points(3, 0));
		PolyBezierV2 second = polyBezier(
				points(4, 0), points(5, 1), points(6, 1), points(7, 0));

		first.mergeShape(second);

		assertEquals(2, first.getP1().length);
		assertEquals(2, first.getP2().length);
		assertEquals(2, first.getP3().length);
		assertEquals(2, first.getP4().length);
		assertEquals(3, first.getP4()[0].x);
		assertEquals(7, first.getP4()[1].x);
	}

	@Test
	void paintFlattensCurvesWhenCgmRequestsFlattening() {
		PolyBezierV2 polyBezier = polyBezier(
				points(0, 0), points(0, 10), points(10, 10), points(10, 0));
		CGMDisplay display = displayWithFlatteningEnabled();

		SVGGraphics2DHS graphics = spy(createGraphics());
		when(display.getGraphics2D()).thenReturn(graphics);
		when(display.getLineStroke()).thenReturn(new BasicStroke());
		when(display.getLineColor()).thenReturn(Color.BLACK);

		polyBezier.paint(display, null);

		ArgumentCaptor<Shape> shapeCaptor = ArgumentCaptor.forClass(Shape.class);
		verify(graphics).draw(shapeCaptor.capture());
		List<Integer> segmentTypes = pathSegmentTypes(shapeCaptor.getValue());
		assertFalse(segmentTypes.contains(PathIterator.SEG_CUBICTO));
		assertTrue(segmentTypes.contains(PathIterator.SEG_LINETO));
	}

	@Test
	void paintFigureFlattensCurvesWhenCgmRequestsFlattening() {
		PolyBezierV2 polyBezier = polyBezier(
				points(0, 0), points(0, 10), points(10, 10), points(10, 0));
		CGMDisplay display = displayWithFlatteningEnabled();

		SVGGraphics2DHS graphics = spy(createGraphics());
		when(display.getGraphics2D()).thenReturn(graphics);
		when(display.getLineStroke()).thenReturn(new BasicStroke());
		when(display.getLineColor()).thenReturn(Color.BLACK);

		polyBezier.paint(display, mock(BeginFigure.class));

		ArgumentCaptor<Shape> shapeCaptor = ArgumentCaptor.forClass(Shape.class);
		verify(graphics).draw(shapeCaptor.capture());
		List<Integer> segmentTypes = pathSegmentTypes(shapeCaptor.getValue());
		assertFalse(segmentTypes.contains(PathIterator.SEG_CUBICTO));
		assertTrue(segmentTypes.contains(PathIterator.SEG_LINETO));
	}

	private static CGMDisplay displayWithFlatteningEnabled() {
		CGM cgm = mock(CGM.class);
		when(cgm.shouldFlattenCurve()).thenReturn(true);
		CGMDisplay display = mock(CGMDisplay.class);
		when(display.getCGM()).thenReturn(cgm);
		return display;
	}

	private static PolyBezierV2 polyBezier(Point2D.Double p1, Point2D.Double p2,
	                                       Point2D.Double p3, Point2D.Double p4) {
		PolyBezier source = mock(PolyBezier.class);
		when(source.getP1()).thenReturn(new Point2D.Double[]{p1});
		when(source.getP2()).thenReturn(new Point2D.Double[]{p2});
		when(source.getP3()).thenReturn(new Point2D.Double[]{p3});
		when(source.getP4()).thenReturn(new Point2D.Double[]{p4});
		return new PolyBezierV2(source);
	}

	private static Point2D.Double points(double x, double y) {
		return new Point2D.Double(x, y);
	}

	private static SVGGraphics2DHS createGraphics() {
		var domImpl = SVGDOMImplementation.getDOMImplementation();
		var document = domImpl.createDocument(SVGDOMImplementation.SVG_NAMESPACE_URI, "svg", null);
		return new SVGGraphics2DHS(SVGGeneratorContext.createDefault(document), false);
	}

	private static List<Integer> pathSegmentTypes(Shape shape) {
		List<Integer> segmentTypes = new ArrayList<>();
		PathIterator iterator = shape.getPathIterator(null);
		double[] coordinates = new double[6];
		while (!iterator.isDone()) {
			segmentTypes.add(iterator.currentSegment(coordinates));
			iterator.next();
		}
		return segmentTypes;
	}
}
