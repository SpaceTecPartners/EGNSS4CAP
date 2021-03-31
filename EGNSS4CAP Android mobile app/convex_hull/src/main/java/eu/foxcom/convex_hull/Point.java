package eu.foxcom.convex_hull;

public class Point
{
    double x, y;

    Point()
    {}

    public Point(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    public Point(double x, double y)
    {
        this.x = x;
        this.y = y;
    }

    // region get, set

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }


    // endregion
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
