package org.openimaj.vis.ternary;

import java.text.AttributedCharacterIterator.Attribute;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.DoubleFVComparison;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.ColourMap;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.typography.FontStyle;
import org.openimaj.image.typography.FontStyle.VerticalAlignment;
import org.openimaj.math.geometry.line.Line2d;
import org.openimaj.math.geometry.point.Point2d;
import org.openimaj.math.geometry.point.Point2dImpl;
import org.openimaj.math.geometry.shape.Rectangle;
import org.openimaj.math.geometry.shape.Triangle;
import org.openimaj.math.geometry.transforms.TransformUtilities;
import org.openimaj.math.geometry.triangulation.DelaunayTriangulator;
import org.openimaj.math.util.Interpolation;
import org.openimaj.util.pair.IndependentPair;

import scala.actors.threadpool.Arrays;

/**
 * 
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 */
public class TernaryPlot {
	private static final float ONE_OVER_ROOT3 = (float) (1f / Math.sqrt(3));

	public static class TernaryData extends DoubleFV {
		public TernaryData(float a, float b, float c, float value) {
			this.values = new double[] { a, b, c };
			this.value = value;

		}

		/**
		 * @return
		 */
		public Point2d asPoint() {
			final double a = this.values[0];
			final double b = this.values[1];
			final double c = this.values[2];
			final double x = 0.5 * (2 * b + c) / (a + b + c);
			final double y = (Math.sqrt(3) / 2) * (c) / (a + b + c);

			return new Point2dImpl((float) x, (float) y);
		}

		public float value;

		@Override
		public int hashCode() {
			return Arrays.hashCode(values);
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof TernaryData && this.hashCode() == obj.hashCode()
					&& this.value == ((TernaryData) obj).value;
		}
	}

	public static class TrenaryDataTriangles {
		private HashMap<Triangle, List<TernaryData>> triToData;
		private HashMap<Point2d, TernaryData> pointToTre;

		public TrenaryDataTriangles(List<TernaryData> data) {
			this.pointToTre = new HashMap<Point2d, TernaryData>();
			for (final TernaryData trenaryData : data) {
				pointToTre.put(trenaryData.asPoint(), trenaryData);
			}
			this.triToData = new HashMap<Triangle, List<TernaryData>>();
			final List<Triangle> triangles = DelaunayTriangulator
					.triangulate(new ArrayList<Point2d>(pointToTre.keySet()));
			for (final Triangle triangle : triangles) {
				final List<TernaryData> triangleData = new ArrayList<TernaryData>();
				triangleData.add(pointToTre.get(triangle.vertices[0]));
				triangleData.add(pointToTre.get(triangle.vertices[1]));
				triangleData.add(pointToTre.get(triangle.vertices[2]));

				triToData.put(triangle, triangleData);
			}
		}

		public Triangle getHoldingTriangle(Point2d point) {
			for (final Triangle t : this.triToData.keySet()) {
				if (t.isInsideOnLine(point)) {
					return t;
				}
			}
			return null;
		}

		public TernaryData getPointData(Point2d point) {
			return pointToTre.get(point);
		}
	}

	private Triangle tri;
	private float height;
	private float width;
	private List<TernaryData> data;
	private Point2dImpl pointA;
	private Point2dImpl pointB;
	private Point2dImpl pointC;
	private TrenaryDataTriangles dataTriangles;

	/**
	 * @param width
	 * @param height
	 * @param data
	 */
	public TernaryPlot(float width, List<TernaryData> data) {
		this.width = width;
		this.height = (float) Math.sqrt((width * width) - ((width * width) / 4));
		pointA = new Point2dImpl(0, height);
		pointB = new Point2dImpl(width, height);
		pointC = new Point2dImpl(width / 2, 0);

		this.tri = new Triangle(new Point2d[] {
				pointA,
				pointB,
				pointC,
		});

		this.data = data;
		if (data.size() > 2) {
			this.dataTriangles = new TrenaryDataTriangles(data);
		}
	}

	/**
	 * @return {@link #draw(TernaryParams)} with the defaults of
	 *         {@link TernaryParams}
	 */
	public MBFImage draw() {
		return draw(new TernaryParams());

	}

	/**
	 * @param params
	 * @return draw the plot
	 */
	public MBFImage draw(TernaryParams params) {

		final int padding = params.getTyped(TernaryParams.PADDING);
		final Float[] bgColour = params.getTyped(TernaryParams.BG_COLOUR);

		final MBFImage ret = new MBFImage((int) width + padding * 2, (int) height + padding * 2, ColourSpace.RGB);
		ret.fill(bgColour);
		drawTernaryPlot(ret, params);
		drawLabels(ret, params);
		drawTriangle(ret, params);
		drawBorder(ret, params);
		drawScale(ret, params);

		return ret;
	}

	private void drawScale(MBFImage ret, TernaryParams params) {
		final boolean drawScale = params.getTyped(TernaryParams.DRAW_SCALE);
		if (!drawScale)
			return;

		final Map<? extends Attribute, Object> typed = params.getTyped(TernaryParams.SCALE_FONT);
		@SuppressWarnings("unchecked")
		final FontStyle<?, Float[]> fs = FontStyle.parseAttributes(typed, ret.createRenderer());

		final int padding = params.getTyped(TernaryParams.PADDING);
		final ColourMap cm = params.getTyped(TernaryParams.COLOUR_MAP);
		final Rectangle r = ret.getBounds();
		r.width = r.width / 2.f;
		r.height = r.height * 2.f;
		r.scale(0.15f);
		r.x = width * TernaryParams.TOP_RIGHT_X;
		r.y = height * TernaryParams.TOP_RIGHT_Y;
		r.translate(padding, padding);
		ret.drawShape(r, 2, RGBColour.BLACK);
		for (float i = r.y; i < r.y + r.height; i++) {
			final Float[] col = cm.apply(((i - r.y) / r.height));
			ret.drawLine((int) r.x, (int) i, (int) (r.x + r.width), (int) i, col);
		}
		fs.setVerticalAlignment(VerticalAlignment.VERTICAL_BOTTOM);
		final String minText = params.getTyped(TernaryParams.SCALE_MIN);
		ret.drawText(minText, (int) r.x - 3, (int) (r.y + r.height), fs);
		fs.setVerticalAlignment(VerticalAlignment.VERTICAL_TOP);
		final String maxText = params.getTyped(TernaryParams.SCALE_MAX);
		ret.drawText(maxText, (int) r.x - 3, (int) r.y, fs);
	}

	private void drawBorder(MBFImage ret, TernaryParams params) {
		final int padding = params.getTyped(TernaryParams.PADDING);
		final boolean drawTicks = params.getTyped(TernaryParams.TRIANGLE_BORDER_TICKS);
		if (drawTicks) {
			final Triangle drawTri = tri.transform(TransformUtilities.translateMatrix(padding, padding));

			for (int i = 0; i < 3; i++) {
				final Point2d start = drawTri.vertices[i];
				final Point2d end = drawTri.vertices[(i + 1) % 3];
				final int nTicks = 10;
				for (int j = 0; j < nTicks + 1; j++) {
					Line2d tickLine = new Line2d(start, end);
					final double length = tickLine.calculateLength();
					// bring its end to the correct position
					double desired = length - j * (length / nTicks);
					if (desired == 0)
						desired = 0.001;
					double scale = desired / length;
					tickLine = tickLine.transform(TransformUtilities.scaleMatrixAboutPoint(scale, scale, start));
					// make it 10 pixels long
					scale = 5f / tickLine.calculateLength();
					tickLine = tickLine.transform(TransformUtilities.scaleMatrixAboutPoint(scale, scale, tickLine.end));
					// Now rotate it by 90 degrees
					tickLine = tickLine.transform(TransformUtilities.rotationMatrixAboutPoint(-Math.PI / 2,
							tickLine.end.getX(), tickLine.end.getY()));
					final int thickness = params.getTyped(TernaryParams.TRIANGLE_BORDER_TICK_THICKNESS);
					final Float[] col = params.getTyped(TernaryParams.TRIANGLE_BORDER_COLOUR);
					ret.drawLine(tickLine, thickness, col);
				}

			}
		}
	}

	private void drawTriangle(MBFImage ret, TernaryParams params) {
		final int padding = params.getTyped(TernaryParams.PADDING);
		final boolean drawTriangle = params.getTyped(TernaryParams.TRIANGLE_BORDER);
		if (drawTriangle) {
			final int thickness = params.getTyped(TernaryParams.TRIANGLE_BORDER_THICKNESS);
			final Float[] col = params.getTyped(TernaryParams.TRIANGLE_BORDER_COLOUR);
			ret.drawShape(this.tri.transform(TransformUtilities.translateMatrix(padding, padding)), thickness, col);
		}
	}

	private void drawLabels(MBFImage ret, TernaryParams params) {
		final int padding = params.getTyped(TernaryParams.PADDING);
		final List<IndependentPair<TernaryData, String>> labels = params.getTyped(TernaryParams.LABELS);
		final Map<? extends Attribute, Object> typed = params.getTyped(TernaryParams.LABEL_FONT);
		@SuppressWarnings("unchecked")
		final FontStyle<?, Float[]> fs = FontStyle.parseAttributes(typed, ret.createRenderer());
		if (labels != null) {
			for (final IndependentPair<TernaryData, String> labelPoint : labels) {
				final Point2d point = labelPoint.firstObject().asPoint();
				point.setX(point.getX() * width + padding);
				point.setY(height - (point.getY() * width) + padding);
				if (point.getY() < height / 2) {
					point.setY(point.getY() - 10);
				}
				else {
					point.setY(point.getY() + 35);
				}
				ret.drawText(labelPoint.getSecondObject(), point, fs);
			}
		}
	}

	private void drawTernaryPlot(MBFImage ret, TernaryParams params) {
		final ColourMap cm = params.getTyped(TernaryParams.COLOUR_MAP);
		final int padding = params.getTyped(TernaryParams.PADDING);
		final Float[] bgColour = params.getTyped(TernaryParams.BG_COLOUR);
		for (int y = 0; y < height + padding; y++) {
			for (int x = 0; x < width + padding; x++) {
				final int xp = x - padding;
				final int yp = y - padding;
				final Point2dImpl point = new Point2dImpl(xp, yp);
				if (this.tri.isInside(point)) {
					final TernaryData closest = weightThreeClosest(point);
					Float[] apply = null;
					if (cm != null)
						apply = cm.apply(1 - closest.value);
					else {
						apply = new Float[] { closest.value, closest.value, closest.value };
					}

					ret.setPixel(x, y, apply);
				}
				else {
					ret.setPixel(x, y, bgColour);
				}
			}
		}
	}

	/**
	 * @return draw the triangles generated from the data
	 */
	public MBFImage drawTriangles() {
		final MBFImage img = new MBFImage((int) width, (int) height, ColourSpace.RGB);
		for (final Triangle tri : this.dataTriangles.triToData.keySet()) {
			img.drawShape(tri.transform(TransformUtilities.scaleMatrix(width, height)), RGBColour.RED);
		}
		return img;
	}

	class DistanceToPointComparator implements Comparator<TernaryData> {

		private TernaryData terneryPoint;

		public DistanceToPointComparator(TernaryData point) {

			this.terneryPoint = point;
		}

		@Override
		public int compare(TernaryData o1, TernaryData o2) {
			final double o1d = DoubleFVComparison.EUCLIDEAN.compare(o1, this.terneryPoint);
			final double o2d = DoubleFVComparison.EUCLIDEAN.compare(o2, this.terneryPoint);
			return Double.compare(o1d, o2d);
		}

	}

	private float calcBfromXY(float xn, float yn) {
		return xn - ONE_OVER_ROOT3 * yn;
	}

	private float calcCfromXY(float xn, float yn) {
		return 2 * ONE_OVER_ROOT3 * yn;
	}

	private float calcAfromXY(float xn, float yn) {
		return 1f - xn - ONE_OVER_ROOT3 * yn;
	}

	private TernaryData weightThreeClosest(Point2dImpl point) {
		final float xn = (point.x - pointA.x) / width;
		final float yn = (pointA.y - point.y) / width;

		final float a = calcAfromXY(xn, yn);
		final float b = calcBfromXY(xn, yn);
		final float c = calcCfromXY(xn, yn);
		final TernaryData trenData = new TernaryData(a, b, c, 0f);
		if (data.size() == 1) {
			return data.get(0);
		} else if (data.size() == 2) {
			final TernaryData tpa = data.get(0);
			final TernaryData tpb = data.get(1);
			final double da = DoubleFVComparison.EUCLIDEAN.compare(tpa, trenData);
			final double db = DoubleFVComparison.EUCLIDEAN.compare(tpb, trenData);
			final double sumd = da + db;
			trenData.value = (float) ((1 - (da / sumd)) * tpa.value + (1 - (db / sumd)) * tpb.value);
		}
		else {
			final Triangle t = dataTriangles.getHoldingTriangle(new Point2dImpl(xn, yn));
			if (t == null) {
				return new TernaryData(a, b, c, 0f);
			}
			final Map<Line2d, Point2d> points = t.intersectionSides(
					new Line2d(
							new Point2dImpl(0, yn),
							new Point2dImpl(1, yn)
					)
					);

			if (points.size() == 2) {
				final Iterator<Line2d> liter = points.keySet().iterator();
				final Line2d l1 = liter.next();
				final Line2d l2 = liter.next();
				final Point2d p1 = points.get(l1);
				final Point2d p2 = points.get(l2);

				final double p1Value = linePointInterp(l1, p1);
				final double p2Value = linePointInterp(l2, p2);

				final double pointValue = linePointInterp(new Line2d(p1, p2), new Point2dImpl(xn, yn), p1Value, p2Value);

				if ((l1.begin.getX() == l1.end.getX() || l2.begin.getX() == l2.end.getX()) && pointValue < 0.5) {
					System.out.println("A vertical line created a 0 value");
				}

				trenData.value = (float) pointValue;

			}
			else { // 0, 1 or more than 2
				System.out.println("Found 3 or 0 lines: " + points.size());
				return new TernaryData(a, b, c, 0f);
			}
		}
		return trenData;
	}

	private double linePointInterp(Line2d line, Point2d point) {
		final TernaryData l1p1data = dataTriangles.getPointData(line.begin);
		final TernaryData l1p2data = dataTriangles.getPointData(line.end);
		final float l1p1datav = l1p1data.value;
		final float l1p2datav = l1p2data.value;

		return linePointInterp(line, point, l1p1datav, l1p2datav);
	}

	private double linePointInterp(Line2d line, Point2d point, double lineBeginValue, double lineEndValue) {
		final double l1Len = line.calculateLength();
		final double l1Prop = Line2d.distance(line.begin, point);
		final double p1Value = Interpolation.lerp(l1Prop, 0, lineBeginValue, l1Len, lineEndValue);
		return p1Value;
	}

	public static void main(String[] args) {
		final List<TernaryData> data = new ArrayList<TernaryData>();
		data.add(new TernaryData(1 / 3f + 0.1f, 1 / 3f - 0.1f, 1 / 3f, 0.8f));
		data.add(new TernaryData(1 / 3f - 0.1f, 1 / 3f + 0.1f, 1 / 3f, 0.2f));
		data.add(new TernaryData(1f, 0, 0, 0));
		data.add(new TernaryData(0, 1f, 0, 0));
		data.add(new TernaryData(0, 0, 1f, 0));
		final TernaryPlot plot = new TernaryPlot(500, data);
		final TernaryParams params = new TernaryParams();
		DisplayUtilities.display(plot.draw());
		DisplayUtilities.display(plot.drawTriangles());
	}

}
