
package polyfromimagecombinedspring;

import java.awt.Color;
import java.util.Set;

/**
 *
 * @author Kevin
 */
public class ColorCluster {
    public ColorCluster(Color ic, int icluster) {
        c = ic;
        cluster = icluster;
    }
    public ColorCluster(Set<ColorCluster> cs) {
        if (cs.size() > 0) {
            int totalR = 0;
            int totalG = 0;
            int totalB = 0;
            for (ColorCluster ic : cs) {
                totalR += ic.c.getRed();
                totalG += ic.c.getGreen();
                totalB += ic.c.getBlue();
            }
            c = new Color(totalR / cs.size(), totalG / cs.size(), totalB / cs.size());
            cluster = cs.iterator().next().cluster;
        }
    }
    Color c;
    int cluster;
    public double similarity(ColorCluster other) {
        int dr = other.c.getRed() - c.getRed();
        int dg = other.c.getGreen() - c.getGreen();
        int db = other.c.getBlue() - c.getBlue();
        return Math.sqrt((dr * dr) + (dg * dg) + (db * db));
    }
}
