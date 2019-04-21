
package polyfromimagecombinedspring;

/**
 * 
 * @author Kevin
 */
public class SimpleEdge {

    public final SimpleNode p0, p1;
    private final double weight;
    
    public SimpleEdge(SimpleNode p0, SimpleNode p1, double weight) {
        this.p0 = p0;
        this.p1 = p1;
        this.weight = weight;
    }
    
    public boolean hasPoint(SimpleNode point) {
        if ((int)(point.x) == (int)(p0.x) && (int)(point.y) == (int)(p0.y)) {
            return true;
        } else if ((int)(point.x) == (int)(p1.x) && (int)(point.y) == (int)(p1.y)) {
            return true;
        }
        return false;
    }

    public double getWeight() {
        return weight;
    }
    
    
}
