/*
 * Copyright (c) 2016 Vivid Solutions.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.locationtech.jtstest.function;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.util.AffineTransformation;
import org.locationtech.jts.operation.polygonize.Polygonizer;
import org.locationtech.jts.precision.GeometryPrecisionReducer;
import org.locationtech.jts.shape.random.RandomPointsBuilder;
import org.locationtech.jts.shape.random.RandomPointsInGridBuilder;
import org.locationtech.jtstest.geomfunction.Metadata;


public class CreateRandomShapeFunctions {

  public static Geometry randomPointsInGrid(Geometry g, int nPts) {
  	RandomPointsInGridBuilder shapeBuilder = new RandomPointsInGridBuilder(FunctionsUtil.getFactoryOrDefault(g));
  	shapeBuilder.setExtent(FunctionsUtil.getEnvelopeOrDefault(g));
  	shapeBuilder.setNumPoints(nPts);
    return shapeBuilder.getGeometry();
  }

  public static Geometry randomPointsInGridCircles(Geometry g, int nPts) {
  	RandomPointsInGridBuilder shapeBuilder = new RandomPointsInGridBuilder(FunctionsUtil.getFactoryOrDefault(g));
  	shapeBuilder.setExtent(FunctionsUtil.getEnvelopeOrDefault(g));
  	shapeBuilder.setNumPoints(nPts);
  	shapeBuilder.setConstrainedToCircle(true);
    return shapeBuilder.getGeometry();
  }

  public static Geometry randomPointsInGridWithGutter(Geometry g, int nPts,
      @Metadata(title="Gutter fraction")
      double gutterFraction) {
  	RandomPointsInGridBuilder shapeBuilder = new RandomPointsInGridBuilder(FunctionsUtil.getFactoryOrDefault(g));
  	shapeBuilder.setExtent(FunctionsUtil.getEnvelopeOrDefault(g));
  	shapeBuilder.setNumPoints(nPts);
  	shapeBuilder.setGutterFraction(gutterFraction);
    return shapeBuilder.getGeometry();
  }

  public static Geometry randomPoints(Geometry g, int nPts) {
  	RandomPointsBuilder shapeBuilder = new RandomPointsBuilder(FunctionsUtil.getFactoryOrDefault(g));
  	shapeBuilder.setExtent(FunctionsUtil.getEnvelopeOrDefault(g));
  	shapeBuilder.setNumPoints(nPts);
    return shapeBuilder.getGeometry();
  }

  public static Geometry randomPointsInPolygon(Geometry g, int nPts) {
  	RandomPointsBuilder shapeBuilder = new RandomPointsBuilder(FunctionsUtil.getFactoryOrDefault(g));
  	shapeBuilder.setExtent(g);
  	shapeBuilder.setNumPoints(nPts);
    return shapeBuilder.getGeometry();
  }

  public static Geometry randomPointsInTriangle(Geometry g, int nPts) {
    GeometryFactory geomFact = FunctionsUtil.getFactoryOrDefault(g);
    Coordinate[] gpts = g.getCoordinates();
    Coordinate tri0 = gpts[0];
    Coordinate tri1 = gpts[1];
    Coordinate tri2 = gpts[2];
    
    List pts = new ArrayList();

    for (int i = 0; i < nPts; i++) {
      pts.add(geomFact.createPoint(randomPointInTriangle(tri0, tri1, tri2)));
    }
    return geomFact.buildGeometry(pts);
  }

  private static Coordinate randomPointInTriangle(Coordinate p0, Coordinate p1, Coordinate p2)
  {
    double s = Math.random();
    double t = Math.random();
    if (s + t > 1) {
      s = 1.0 - s;
      t = 1.0 - t;
    }
    double a = 1 - (s + t);
    double b = s;
    double c = t;
    
    double rpx = a * p0.x + b * p1.x + c * p2.x; 
    double rpy = a * p0.y + b * p1.y + c * p2.y; 
    
    return new Coordinate(rpx, rpy);
  }

  public static Geometry randomRadialPoints(Geometry g, int nPts) {
    Envelope env = FunctionsUtil.getEnvelopeOrDefault(g);
    GeometryFactory geomFact = FunctionsUtil.getFactoryOrDefault(g);
    double xLen = env.getWidth();
    double yLen = env.getHeight();
    double rMax = Math.min(xLen, yLen) / 2.0;
    
    double centreX = env.getMinX() + xLen/2;
    double centreY = env.getMinY() + yLen/2;
    
    List pts = new ArrayList();

    for (int i = 0; i < nPts; i++) {
      double rand = Math.random();
      // use rand^2 to accentuate radial distribution
      double r = rMax * rand * rand;
      // produces even distribution
      //double r = rMax * Math.sqrt(rand);
      double ang = 2 * Math.PI * Math.random();
      double x = centreX + r * Math.cos(ang);
      double y = centreY + r * Math.sin(ang);
      pts.add(geomFact.createPoint(new Coordinate(x, y)));
    }
    return geomFact.buildGeometry(pts);
  }
  
  @Metadata(description="Create Halton points using bases 2 and 3")
  public static Geometry haltonPoints(Geometry g, int nPts)
  {
    return haltonPointsWithBases(g, nPts, 2, 3);
  }
  
  @Metadata(description="Create Halton points using bases 5 and 7")
  public static Geometry haltonPoints57(Geometry g, int nPts)
  {
    return haltonPointsWithBases(g, nPts, 5, 7);
  }
  
  @Metadata(description="Create Halton points using provided bases")
  public static Geometry haltonPointsWithBases(Geometry g, int nPts, 
      @Metadata(title="Base 1")
      int basei, 
      @Metadata(title="Base 2")
      int basej)
  {
    Envelope env = FunctionsUtil.getEnvelopeOrDefault(g);
    Coordinate[] pts = new Coordinate[nPts];
    double baseX = env.getMinX();
    double baseY = env.getMinY();
    
    int i = 0;
    while (i < nPts) {
      double x = baseX + env.getWidth() * haltonOrdinate(i + 1, basei);
      double y = baseY + env.getHeight() * haltonOrdinate(i + 1, basej);
      Coordinate p = new Coordinate(x, y);
      if (! env.contains(p))
        continue;
      pts[i++] = p;
    }
    return FunctionsUtil.getFactoryOrDefault(g).createMultiPoint(pts);
  }
  
  private static double haltonOrdinate(int index, int base)
  {
    double result = 0;
    double f = 1.0 / base;
    int i = index;
    while (i > 0) {
        result = result + f * (i % base);
        i = (int) Math.floor(i / (double) base);
        f = f / base;
    }
    return result;
  }
  
  static final double PHI2 = 1.32471795724474602596;
  
  /**
   * Creates a set of quasi-random 2D points using the Roberts recurrences.
   * <a href='http://extremelearning.com.au/unreasonable-effectiveness-of-quasirandom-sequences'>Roberts recurrences</a> 
   * are based on the generalized Golden Ratio (for the 2D case, Phi2).
   * They have excellent low-discrepancy characteristics.
   * This mean they are non-periodic and have less clustering
   * than random points or Halton points.
   * 
   * @param geom
   * @param nPts
   * @return
   */
  @Metadata(description="Create Roberts quasi-random points")
  public static Geometry robertsPoints(Geometry geom, int nPts)
  {
    Envelope env = FunctionsUtil.getEnvelopeOrDefault(geom);
    Coordinate[] pts = new Coordinate[nPts];
    double baseX = env.getMinX();
    double baseY = env.getMinY();
    
    final double A1 = 1.0 / PHI2;
    final double A2 = 1.0/(PHI2 * PHI2);
    double r1 = 0.5;
    double r2 = 0.5;
    int i = 0;
    while (i < nPts) {
      r1 = quasirandom(r1, A1);
      r2 = quasirandom(r2, A2);
      double x = baseX + env.getWidth() * r1;
      double y = baseY + env.getHeight() * r2;
      Coordinate p = new Coordinate(x, y);
      if (! env.contains(p))
        continue;
      pts[i++] = p;
    }
    return FunctionsUtil.getFactoryOrDefault(geom).createMultiPoint(pts);
  }

  private static double quasirandom(double curr, double alpha) {
    double next = curr + alpha;
    if (next < 1) return next;
    return next - Math.floor(next);
  }
  
  public static Geometry randomSegments(Geometry g, int nPts) {
    Envelope env = FunctionsUtil.getEnvelopeOrDefault(g);
    GeometryFactory geomFact = FunctionsUtil.getFactoryOrDefault(g);
    double xLen = env.getWidth();
    double yLen = env.getHeight();

    List lines = new ArrayList();

    for (int i = 0; i < nPts; i++) {
      double x0 = env.getMinX() + xLen * Math.random();
      double y0 = env.getMinY() + yLen * Math.random();
      double x1 = env.getMinX() + xLen * Math.random();
      double y1 = env.getMinY() + yLen * Math.random();
      lines.add(geomFact.createLineString(new Coordinate[] {
          new Coordinate(x0, y0), new Coordinate(x1, y1) }));
    }
    return geomFact.buildGeometry(lines);
  }

  public static Geometry randomSegmentsInGrid(Geometry g, int nPts) {
    Envelope env = FunctionsUtil.getEnvelopeOrDefault(g);
    GeometryFactory geomFact = FunctionsUtil.getFactoryOrDefault(g);

    int nCell = (int) Math.sqrt(nPts) + 1;

    double xLen = env.getWidth() / nCell;
    double yLen = env.getHeight() / nCell;

    List lines = new ArrayList();

    for (int i = 0; i < nCell; i++) {
      for (int j = 0; j < nCell; j++) {
        double x0 = env.getMinX() + i * xLen + xLen * Math.random();
        double y0 = env.getMinY() + j * yLen + yLen * Math.random();
        double x1 = env.getMinX() + i * xLen + xLen * Math.random();
        double y1 = env.getMinY() + j * yLen + yLen * Math.random();
        lines.add(geomFact.createLineString(new Coordinate[] {
            new Coordinate(x0, y0), new Coordinate(x1, y1) }));
      }
    }
    return geomFact.buildGeometry(lines);
  }

  public static Geometry randomSegmentsRectilinear(Geometry g, int nPts) {
    Envelope env = FunctionsUtil.getEnvelopeOrDefault(g);
    GeometryFactory geomFact = FunctionsUtil.getFactoryOrDefault(g);
    double xLen = env.getWidth();
    double yLen = env.getHeight();

    List lines = new ArrayList();

    for (int i = 0; i < nPts; i++) {
      double x0 = env.getMinX() + xLen * Math.random();
      double x1 = env.getMinY() + yLen * Math.random();
      double v = env.getMinX() + xLen * Math.random();
      double y0 = v;
      double y1 = v;
      boolean isXFixed = Math.random() < 0.5;
      if (isXFixed) {
        y0 = x0;
        y1 = x1;
        x0 = v;
        x1 = v;
      }
      lines.add(geomFact.createLineString(new Coordinate[] {
          new Coordinate(x0, y0), new Coordinate(x1, y1) }));
    }
    return geomFact.buildGeometry(lines);
  }

  public static Geometry randomLineString(Geometry g, int nPts) {
    Envelope env = FunctionsUtil.getEnvelopeOrDefault(g);
    GeometryFactory geomFact = FunctionsUtil.getFactoryOrDefault(g);
    double width = env.getWidth();
    double hgt = env.getHeight();

    Coordinate[] pts = new Coordinate[nPts];

    for (int i = 0; i < nPts; i++) {
      double xLen = width * Math.random();
      double yLen = hgt * Math.random();
      pts[i] = randomPtInRectangleAround(env.centre(), xLen, yLen);
    }
    return geomFact.createLineString(pts);
  }

  public static Geometry randomRectilinearWalk(Geometry g, int nPts) {
    Envelope env = FunctionsUtil.getEnvelopeOrDefault(g);
    GeometryFactory geomFact = FunctionsUtil.getFactoryOrDefault(g);
    double xLen = env.getWidth();
    double yLen = env.getHeight();

    Coordinate[] pts = new Coordinate[nPts];

    boolean xory = true;
    for (int i = 0; i < nPts; i++) {
      Coordinate pt = null;
      if (i == 0) {
       pt = randomPtInRectangleAround(env.centre(), xLen, yLen);
      }
      else {
        double dist = xLen * (Math.random() - 0.5);
        double x = pts[i-1].x;
        double y = pts[i-1].y;
        if (xory) {
          x += dist;
        }
        else {
          y += dist;
        }
        // switch orientation
        xory = ! xory;
        pt = new Coordinate(x, y);
      }
      pts[i] = pt;
    }
    return geomFact.createLineString(pts);
  }

  private static int randomQuadrant(int exclude)
  {
    while (true) { 
      int quad = (int) (Math.random() * 4);
      if (quad > 3) quad = 3;
      if (quad != exclude) return quad;
    }
  }
  
  private static Coordinate randomPtInRectangleAround(Coordinate centre, double width, double height)
  {
    double x0 = centre.x + width * (Math.random() - 0.5);
    double y0 = centre.y + height * (Math.random() - 0.5);
    return new Coordinate(x0, y0);    
  }

  /**
   * Truchet tiling created from a base (lower-left) tile defined by from set of lines.
   * The tiles are copied to a square grid of size nSide,
   * and rotated by a random multiple of 90 degrees.
   * The tile line endpoints are snapped to a precision grid to make them align.
   * If tiling is to be polygonized lines should be noded and snapped to edge points.
   * 
   * @param tileLines set of lines defining base tile
   * @param nSide number of tiles per side of tiling
   * @return lines for tiling.
   */
  @Metadata(description="Create Truchet tiling from lines defining lower left tile")
  public static Geometry truchetTiling(Geometry tileLines, 
      @Metadata(title="Grid side cell #")
      int nSide,
      @Metadata(title="Randomness")
      double randomness) {
    PrecisionModel pmSnap = new PrecisionModel(10000.0);
    //Geometry tileSnap = snapEndpoints(tileLines.copy(), pmSnap);
    Geometry tileSnap = GeometryPrecisionReducer.reduce(tileLines, pmSnap);
    Envelope env = tileSnap.getEnvelopeInternal();
    int side = (int) Math.max(env.getHeight(), env.getWidth());
    Coordinate centre = env.centre();
    
    List<Geometry> tiles = new ArrayList<Geometry>();
    for (int i = 0; i < nSide; i++) {
      for (int j = 0; j < nSide; j++) {
        
        int nPi2 = (i + j) % 4;
        if (Math.random() < randomness) {
        //-- random rotation by PI/2, translate to grid cell
          nPi2 = (int) (4 * Math.random());
        }
        
        AffineTransformation trans = AffineTransformation.rotationInstance(nPi2 * Math.PI / 2.0, 
            centre.getX(), centre.getY());
        trans.translate(i * side, j * side);
        
        Geometry tileTrans = tileSnap.copy();
        //-- don't transform base tile
        if (i > 0 || j > 0) {
          tileTrans.apply(trans);
        }
        //Geometry tileTransSnap = snapEndpoints(tileTrans, pmSnap);
        Geometry tileTransSnap = GeometryPrecisionReducer.reduce(tileTrans, pmSnap);
        tiles.add(tileTransSnap);
      }
    }
    //-- close tiling and polygonize
    double baseX = env.getMinX();
    double baseY = env.getMinY();
    Envelope tilingEnv = new Envelope(baseX, baseX + nSide * side, baseY, baseY + nSide * side); 
    Geometry tilingBdy = tileLines.getFactory().toGeometry(tilingEnv).getBoundary();
    tiles.add(tilingBdy);
    Geometry tileLinesGeom = tileLines.getFactory().buildGeometry(tiles);
    Geometry allLines = tileLinesGeom.union();
    //System.out.println(allLines);
    Polygonizer polygonizer = new Polygonizer();
    polygonizer.add(allLines);
    return polygonizer.getGeometry();
  }

}
