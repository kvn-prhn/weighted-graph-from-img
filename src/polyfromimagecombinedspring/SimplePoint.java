
package polyfromimagecombinedspring;

/**
 * A simple point container. 
 * @author Kevin
 */
public class SimplePoint {

    public double x, y;
    public int id;
    
    public SimplePoint(double x, double y) {
        this.x = x;
        this.y = y;
        this.id = -1; // unassigned
    }

    @Override
    public String toString() {
        return "[SimplePoint:x=" + x + ",y=" + y + "]";
    }

    @Override
    public boolean equals(Object obj) {
        return ((SimplePoint)obj).x == this.x && ((SimplePoint)obj).y == this.y;
    }
    
    public double distanceTo(SimplePoint other) {
        double diffX = other.x - this.x;
        double diffY = other.y - this.y;
        return Math.sqrt((diffX * diffX) + (diffY * diffY));
    }
    
}
