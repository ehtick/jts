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

package org.locationtech.jts.operation.overlay.snap;

import org.locationtech.jts.algorithm.Distance;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateList;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;

/**
 * Snaps the vertices and segments of a {@link LineString} 
 * to a set of target snap vertices.
 * A snap distance tolerance is used to control where snapping is performed.
 * <p>
 * The implementation handles empty geometry and empty snap vertex sets.
 *
 * @author Martin Davis
 * @version 1.7
 */
public class LineStringSnapper
{
  private double snapTolerance = 0.0;
  private double snapToleranceSq = 0.0;

  private Coordinate[] srcPts;
  private boolean allowSnappingToSourceVertices = false;
  private boolean isClosed = false;

  /**
   * Creates a new snapper using the points in the given {@link LineString}
   * as source snap points.
   * 
   * @param srcLine a LineString to snap (may be empty)
   * @param snapTolerance the snap tolerance to use
   */
  public LineStringSnapper(LineString srcLine, double snapTolerance)
  {
    this(srcLine.getCoordinates(), snapTolerance);
  }

  /**
   * Creates a new snapper using the given points
   * as source points to be snapped.
   * 
   * @param srcPts the points to snap 
   * @param snapTolerance the snap tolerance to use
   */
  public LineStringSnapper(Coordinate[] srcPts, double snapTolerance)
  {
    this.srcPts = srcPts;
    isClosed = isClosed(srcPts);
    this.snapTolerance = snapTolerance;
    this.snapToleranceSq = snapTolerance*snapTolerance;
  }

  public void setAllowSnappingToSourceVertices(boolean allowSnappingToSourceVertices)
  {
    this.allowSnappingToSourceVertices = allowSnappingToSourceVertices;
  }
  private static boolean isClosed(Coordinate[] pts)
  {
    if (pts.length <= 1) return false;
    return pts[0].equals2D(pts[pts.length - 1]);
  }
  /**
   * Snaps the vertices and segments of the source LineString 
   * to the given set of snap vertices.
   * 
   * @param snapPts the vertices to snap to
   * @return a list of the snapped points
   */
  public Coordinate[] snapTo(Coordinate[] snapPts)
  {
    CoordinateList coordList = new CoordinateList(srcPts);

    snapVertices(coordList, snapPts);
    snapSegments(coordList, snapPts);

    Coordinate[] newPts = coordList.toCoordinateArray();
    return newPts;
  }

  /**
   * Snap source vertices to vertices in the target.
   * 
   * @param srcCoords the points to snap
   * @param snapPts the points to snap to
   */
  private void snapVertices(CoordinateList srcCoords, Coordinate[] snapPts)
  {
    // try snapping vertices
    // if src is a ring then don't snap final vertex
    int end = isClosed ? srcCoords.size() - 1 : srcCoords.size();
    for (int i = 0; i < end; i++) {
      Coordinate srcPt = (Coordinate) srcCoords.get(i);
      Coordinate snapVert = findSnapForVertex(srcPt, snapPts);
      if (snapVert != null) {
        // update src with snap pt
        srcCoords.set(i, new Coordinate(snapVert));
        // keep final closing point in synch (rings only)
        if (i == 0 && isClosed)
          srcCoords.set(srcCoords.size() - 1, new Coordinate(snapVert));
      }
    }
  }

  private Coordinate findSnapForVertex(Coordinate pt, Coordinate[] snapPts)
  {
    for (int i = 0; i < snapPts.length; i++) {
      // if point is already equal to a src pt, don't snap
      if (pt.equals2D(snapPts[i]))
        return null;
      if (pt.distance(snapPts[i]) < snapTolerance)
        return snapPts[i];
    }
    return null;
  }

  /**
   * Snap segments of the source to nearby snap vertices.
   * Source segments are "cracked" at a snap vertex.
   * A single input segment may be snapped several times 
   * to different snap vertices.
   * <p>
   * For each distinct snap vertex, at most one source segment
   * is snapped to.  This prevents "cracking" multiple segments 
   * at the same point, which would likely cause 
   * topology collapse when being used on polygonal linework.
   * 
   * @param srcCoords the coordinates of the source linestring to be snapped
   * @param snapPts the target snap vertices
   */
  private void snapSegments(CoordinateList srcCoords, Coordinate[] snapPts)
  {
    // guard against empty input
    if (snapPts.length == 0) return;
    
    int distinctPtCount = snapPts.length;

    // check for duplicate snap pts when they are sourced from a linear ring.  
    // TODO: Need to do this better - need to check *all* snap points for dups (using a Set?)
    if (snapPts[0].equals2D(snapPts[snapPts.length - 1]))
        distinctPtCount = snapPts.length - 1;

    for (int i = 0; i < distinctPtCount; i++) {
      Coordinate snapPt = snapPts[i];
      int index = findSegmentIndexToSnap(snapPt, srcCoords);
      /**
       * If a segment to snap to was found, "crack" it at the snap pt.
       * The new pt is inserted immediately into the src segment list,
       * so that subsequent snapping will take place on the modified segments.
       * Duplicate points are not added.
       */
      if (index >= 0) {
        srcCoords.add(index + 1, new Coordinate(snapPt), false);
      }
    }
  }


  /**
   * Finds a src segment which snaps to (is close to) the given snap point.
   * <p>
   * Only a single segment is selected for snapping.
   * This prevents multiple segments snapping to the same snap vertex,
   * which would almost certainly cause invalid geometry
   * to be created.
   * (The heuristic approach to snapping used here
   * is really only appropriate when
   * snap pts snap to a unique spot on the src geometry.)
   * <p>
   * Also, if the snap vertex occurs as a vertex in the src coordinate list,
   * no snapping is performed.
   * 
   * @param snapPt the point to snap to
   * @param srcCoords the source segment coordinates
   * @return the index of the snapped segment
   * or -1 if no segment snaps to the snap point
   */
  private int findSegmentIndexToSnap(Coordinate snapPt, CoordinateList srcCoords)
  {
    double minDist = Double.MAX_VALUE;
    int snapIndex = -1;
    for (int i = 0; i < srcCoords.size() - 1; i++) {
      final Coordinate p0 = srcCoords.get(i);
      final Coordinate p1 = srcCoords.get(i+1);

      /**
       * Check if the snap pt is equal to one of the segment endpoints.
       * 
       * If the snap pt is already in the src list, don't snap at all.
       */
      if (p0.equals2D(snapPt) || p1.equals2D(snapPt)) {
        if (allowSnappingToSourceVertices)
          continue;
        else
          return -1;
      }
      
      double dist = Distance.pointToSegmentSq(snapPt, p0, p1);
      if (dist < snapToleranceSq && dist < minDist) {
        minDist = dist;
        snapIndex = i;
      }
    }
    return snapIndex;
  }

}
