package eu.foxcom.convex_hull;

import java.util.ArrayList;
import java.util.List;

public class Cluster {
    List<Point> points = new ArrayList<>();
    QuickHull quickHull = new QuickHull();

    public void reset() {
        points = new ArrayList<>();
    }

    public void addPoint(Point point) {
        points.add(point);
    }

    public void removePoint(Point point) {
        points.remove(point);
    }

    public Point computeCentroid(List<Point> lastPerimeter) {
        if (points.size() == 0) {
            return null;
        }
        Point centroid = new Point();
        List<Point> hull = quickHull.quickHull(points);
        List<Point> lastHull = null;
        List<Point> remainPoints = new ArrayList<>(points);
        while (hull.size() > 0) {
            lastHull = new ArrayList<>(hull);
            for (Point hullPoint : hull) {
                remainPoints.remove(hullPoint);
            }
            hull = quickHull.quickHull(remainPoints);
        }

        double centroidX = 0;
        double centroidY = 0;
        for (Point hullPoint : lastHull) {
            centroidX += hullPoint.getX();
            centroidY += hullPoint.getY();
        }
        centroidX = centroidX / lastHull.size();
        centroidY = centroidY / lastHull.size();
        centroid.setX(centroidX);
        centroid.setY(centroidY);
        if (lastPerimeter != null) {
            lastPerimeter.clear();
            lastPerimeter.addAll(lastHull);
        }
        return centroid;
    }

    public int getSize() {
        return points.size();
    }
}
