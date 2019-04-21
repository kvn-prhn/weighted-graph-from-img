
package polyfromimagecombinedspring;

/**
 * A simple point container. 
 * @author Kevin
 */
public class SimpleNode {

    public double x, y;
    public int id;
    private static int prev_id = 0;
    
    // cost for pathfinding
    public double cost;
    // the parent simple node for pathfinding
    public SimpleNode pathParent;
    
    public SimpleNode(double x, double y) {
        this.x = x;
        this.y = y;
        this.id = prev_id;
        prev_id++;
        //this.id = -1; // unassigned
        this.cost = Double.MAX_VALUE / 2;
        this.pathParent = null;
    }

    @Override
    public String toString() {
        return "[SimplePoint:x=" + x + ",y=" + y + "]";
    }
/*
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof SimpleNode)) 
            return false;
        return ((SimpleNode)obj).x == this.x && ((SimpleNode)obj).y == this.y;
    }
  */  
    
    /**
     * Find the distance from this node to another node (Euclidean)
     * @param other 
     * @return 
     */
    public double distanceTo(SimpleNode other) {
        double diffX = other.x - this.x;
        double diffY = other.y - this.y;
        return Math.sqrt((diffX * diffX) + (diffY * diffY));
    }
    
}
