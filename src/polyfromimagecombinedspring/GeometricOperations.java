
package polyfromimagecombinedspring;

import java.awt.Polygon;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Stack;

/**
 * This is where I am stuffing all of the geometric operations like 
 * finding the convex hull or the point grouping stage.
 * @author Kevin
 */
public class GeometricOperations {

    public static ArrayList<ArrayList<SimplePoint>> groupPoints(ArrayList<SimplePoint> points, 
            double min_dist_to_group, double max_dist_of_groups, double cutoff_group_size, double min_variance) {

        ArrayList<Integer> x_points = new ArrayList<>();
        ArrayList<Integer> y_points = new ArrayList<>();
        for (SimplePoint sp : points) {
            x_points.add((int)sp.x);
            y_points.add((int)sp.y);
        }
        
        // the g_of_groups represents the indices of the points
        // that are found in the x_points and y_points arrays.
        ArrayList< ArrayList<Integer> > g_of_groups = new ArrayList<>();
        for (int i = 0; i < x_points.size(); i++) {
            ArrayList<Integer> init_group = new ArrayList<>();  
            init_group.add(i);
            g_of_groups.add(init_group);
        }
        
	boolean combined = true;
	while (combined) {
            combined = false;
            for (int i = 0; i < g_of_groups.size(); i++) {
                // look at each other group- if at least
                // one element in the two groups are within a minimum
                // distance, then you can combine the groups
                for (int j = i + 1; j < g_of_groups.size(); j++) {

                    double min_dist = min_dist_to_group + 1;
                    double max_dist = max_dist_of_groups - 1;    // also track the max_dist
                    for (int me = 0; me < g_of_groups.get(i).size(); me++) {
                        for (int other = 0; other < g_of_groups.get(j).size(); other++) {
                            double diffX = x_points.get(g_of_groups.get(i).get(me)) - 
                                    x_points.get(g_of_groups.get(j).get(other));
                            double diffY = y_points.get(g_of_groups.get(i).get(me)) - 
                                    y_points.get(g_of_groups.get(j).get(other));
                            double dist = Math.sqrt((diffX * diffX) + (diffY * diffY));
                            if (dist < min_dist) {
                                min_dist = dist;
                            }
                            if (dist > max_dist) {
                                max_dist = dist;
                            }
                        }
                    }
                    // combine groups if they are close enough andn ot too far away
                    if (min_dist < min_dist_to_group && max_dist < max_dist_of_groups) {
                        // combine 
                        for (int w = 0; w < g_of_groups.get(j).size(); w++) {
                            g_of_groups.get(i).add(g_of_groups.get(j).get(w));
                        }
                        ArrayList<Integer> removed = g_of_groups.remove(j);
                        combined = true;
                    }

                } 
            }
	}        
        
        System.err.println("BEFORE CUTTING SMALL GROUPS: " + g_of_groups.size());
	// trim negligable groups.
	
	for (int i = 0; i < g_of_groups.size(); i++) {
            if (g_of_groups.get(i).size() < cutoff_group_size) {
                g_of_groups.remove(i);
                i--;
            }
        }
         System.err.println("BEFORE CUTTING NON-VARIANT GROUPS: " + g_of_groups.size());
        // trim all groups that have a low variance of points in x or y dimension
        
        for (int i = 0; i < g_of_groups.size(); i++) {
            int n = g_of_groups.get(i).size();
            if (n > 1) {
                // standard deviation of the x coordinate
                int mean_x = 0;
                int mean_y = 0;
                for (Integer spIndex : g_of_groups.get(i) ) {
                    mean_x += x_points.get(spIndex);
                    mean_y += y_points.get(spIndex);
                }
                mean_x /= g_of_groups.get(i).size();
                mean_y /= g_of_groups.get(i).size();

                double group_std_x = 0;
                double group_std_y = 0;
                for (Integer spIndex : g_of_groups.get(i)) {
                    int minus_mean_x = (x_points.get(spIndex) - mean_x);
                    group_std_x += minus_mean_x * minus_mean_x;

                    int minus_mean_y = (y_points.get(spIndex) - mean_y);
                    group_std_y += minus_mean_y * minus_mean_y;
                }
                group_std_x = Math.sqrt(group_std_x / (n - 1));
                group_std_y = Math.sqrt(group_std_y / (n - 1));

                // if the variance is too small, remove it
                if (group_std_x < min_variance || group_std_y < min_variance) {
                    g_of_groups.remove(i);
                    i--;
                } 
            } else {
                g_of_groups.remove(i); // group too small (still?);
                i--;
            }
        }
        
        ArrayList<ArrayList<SimplePoint>> groups = new ArrayList<>();
        for (int i = 0; i < g_of_groups.size(); i++) {
            ArrayList<SimplePoint> group = new ArrayList<>(); // start of a new group
            for (int k = 0; k < g_of_groups.get(i).size(); k++) {
                group.add(new SimplePoint(x_points.get(g_of_groups.get(i).get(k)), y_points.get(g_of_groups.get(i).get(k))));
            }
            groups.add(group);
        }
        
        return groups;
    }
    
    
    
    
    public static ArrayList<ArrayList<SimplePoint>> convexHullOnPointGroups(ArrayList<ArrayList<SimplePoint>> g_of_groups, 
            double max_distance_between_points) {      
        try {
            ArrayList<ArrayList<SimplePoint>> groupHulls = new ArrayList<>();
            
            // all points are loaded into the program at this point.
            g_of_groups.forEach((group) -> {
                
                // before making the hull, sort the group
                // since the algorithm expects it to be sorted.
                group.sort((SimplePoint o1, SimplePoint o2) -> {
                        if (o1.x != o2.x)
                            return (int)(o1.x - o2.x);
                        return (int)(o1.y - o2.y);
                    });
                
                Stack<SimplePoint> lowerHull = new Stack<>();
                Stack<SimplePoint> upperHull = new Stack<>();
                // assumed to be the left-most point goes first.                
                for (int i = 0; i < group.size(); i++) {
                    while (lowerHull.size() >= 2 && 
                            cross(lowerHull.get(lowerHull.size()-2), 
                            lowerHull.get(lowerHull.size()-1), group.get(i)) >= 0) {
                        lowerHull.pop();
                    }
                    lowerHull.push(group.get(i));
                }
                for (int i = group.size() - 1; i >= 0 ; i--) {
                    while (upperHull.size() >= 2 && 
                            cross(upperHull.get(upperHull.size()-2), 
                            upperHull.get(upperHull.size()-1), group.get(i)) >= 0) {
                        upperHull.pop();
                    }
                    upperHull.push(group.get(i));
                }
                for (int i = 0; i < upperHull.size(); i++) {
                    if (!lowerHull.contains(upperHull.get(i))) 
                        lowerHull.add(upperHull.get(i));
                }
                groupHulls.add(new ArrayList<>(lowerHull));
            });
                        
            // convert all of the group hulls into polygons for additiona testing.
            ArrayList<Polygon> groupHullsPolys = new ArrayList<>();
            groupHulls.forEach((hull) -> {
                int on = 0;
                int[] xPoints = new int[hull.size()];
                int[] yPoints = new int[hull.size()];
                for (SimplePoint point : hull) {
                   xPoints[on] = (int)point.x; 
                   yPoints[on] = (int)point.y;
                   on++;
                }
               groupHullsPolys.add(new Polygon(xPoints, yPoints, hull.size())); 
            });
            
            System.err.println("Before removing unneeded: " + groupHullsPolys.size() + " polygons");
            // remove all polygons that are totally contained inside of another polygon.
            for (int i = 0; i < groupHullsPolys.size(); i++) {
                for (int j = i+1; j < groupHullsPolys.size(); j++) {
                    // if every point in j is inside of i, remove j
                    boolean pointNotIn = false;
                    for (int k = 0; k < groupHullsPolys.get(j).npoints; k++) {
                        int  k_px =  groupHullsPolys.get(j).xpoints[k];
                        int  k_py =  groupHullsPolys.get(j).ypoints[k];
                        if (!groupHullsPolys.get(i).contains(k_px, k_py)) {
                            pointNotIn = true;
                            break;
                        }
                    }
                    if (!pointNotIn) {
                        groupHullsPolys.remove(j);
                        j--;
                    }
                }
            }
            
            System.err.println("AFTER removing unneeded: " + groupHullsPolys.size() + " polygons");
            
            // reduce the number of points in the polygon...
            // for each point in the polygon, if it is within some threshold            
            for (Polygon cPoly : groupHullsPolys) {
                
                // populate array lists with points so we can remove points easily
                ArrayList<Integer> x_points = new ArrayList<>();
                for (int k = 0; k < cPoly.npoints; k++)
                    x_points.add(cPoly.xpoints[k]);
                ArrayList<Integer> y_points = new ArrayList<>();
                for (int k = 0; k < cPoly.npoints; k++)
                    y_points.add(cPoly.ypoints[k]);
                
                for (int i = 0; i < x_points.size(); i++) {
                    // get the indices of each point to compare, wrapping around if needed
                    int prevIndex = i-1;
                    if (prevIndex < 0)
                        prevIndex = x_points.size() - 1;
                    int nextIndex = i+1;
                    if (nextIndex >= x_points.size())
                        nextIndex = 0;
                    
                    // check the distance between this point and prev/next points
                    double diffXprev = x_points.get(prevIndex) - x_points.get(i);
                    double diffYprev = y_points.get(prevIndex) - y_points.get(i);
                    double diffXnext = x_points.get(nextIndex) - x_points.get(i);
                    double diffYnext = y_points.get(nextIndex) - y_points.get(i);
                    
                    double distancePrev = Math.sqrt((diffXprev * diffXprev) + (diffYprev * diffYprev));
                    double distanceNext = Math.sqrt((diffXnext * diffXnext) + (diffYnext * diffYnext));
                    
                    
                    double currVX = x_points.get(i) - x_points.get(prevIndex);
                    double currVY = y_points.get(i) - y_points.get(prevIndex);
                    double potentialVX = x_points.get(nextIndex) - x_points.get(prevIndex);
                    double potentialVY = y_points.get(nextIndex) - y_points.get(prevIndex);
                    
                    double dotProd = (currVX * potentialVX) + (currVY * potentialVY);
                    double currMagn = Math.sqrt((currVX * currVX) + (currVY * currVY));
                    double potentialMagn = Math.sqrt((potentialVX * potentialVX) + (potentialVY * potentialVY));
                    double diffAngle = Math.acos(dotProd / (currMagn * potentialMagn));
                    
                    // has to be within a certain angle and a certain distance
                    if (diffAngle < Math.PI / 5.0 && distancePrev < max_distance_between_points && distanceNext < max_distance_between_points) {  //  arbitray
                        // remove index i 
                        Integer removedX = x_points.remove(i);  // putting in variable so it knows which "remove" to use.
                        Integer removedY = y_points.remove(i);
                        i--;                                    // account the future points' indices calling back
                    }
                }
                
                // rebuild the polygon with our reduced points.
                cPoly.reset();
                for (int i = 0; i < x_points.size(); i++) {
                    cPoly.addPoint(x_points.get(i), y_points.get(i));
                }
            }
             
            // add all of the newly transformed polygons into our array of polygons as points. 
            ArrayList<ArrayList<SimplePoint>> polyPoints = new ArrayList<>();
            for (Polygon cPoly : groupHullsPolys) {
                ArrayList<SimplePoint> tpoly = new ArrayList<>();
                for (int i = 0; i < cPoly.npoints; i++) {
                    tpoly.add(new SimplePoint(cPoly.xpoints[i], cPoly.ypoints[i]));
                }
                polyPoints.add(tpoly);
            }

            return polyPoints;
            /* */
        } catch(Exception e) {
            System.err.println("Error: " + e);
            e.printStackTrace();
        }
        return new ArrayList<>(); // unsuccessful
    }
    
    
     public static SimpleGraph polygonsToGraph(ArrayList<ArrayList<SimplePoint>> inputPolys, double polygon_padding_scale, int boundary_top, int boundary_bottom, int boundary_left, int boundary_right) {
        // each array is going to be a polygon, where the points are 
        // all stored as point objects.
        // points and edges are the output arrays
        ArrayList<SimplePoint> points = new ArrayList<>();
        SimpleGraph simpleGraph = new SimpleGraph();
       // System.err.println("Number of polygons: " + inputPolys.size());
        
        int id_p = 0;
        for (int i = 0; i < inputPolys.size(); i++) {
                ArrayList<SimplePoint> this_points = new ArrayList<>();
                for (SimplePoint p : inputPolys.get(i)) {
                    SimplePoint thePoint = new SimplePoint(p.x, p.y);
                    thePoint.id = id_p++;
                    this_points.add(thePoint);
                }
                
                int this_centerX = 0;
                int this_centerY = 0;
                for (SimplePoint p : this_points) {
                    this_centerX += p.x;
                    this_centerY += p.y;
                }
                this_centerX /= this_points.size();   // find the average x point
                this_centerY /= this_points.size();   // find the average y point.
                
                // transform the POINTS themselves to be a bit larger than our own polygons
                // first, tranform it to local space
                for (SimplePoint p : this_points) {
                    p.x = p.x - this_centerX;
                    p.y = p.y - this_centerY;
                }
                // now, scale all point
                for (SimplePoint p : this_points) {
                    p.x = p.x * polygon_padding_scale;
                    p.y = p.y * polygon_padding_scale;
                }
                // finally transform everything back.
                for (SimplePoint p : this_points) {
                    p.x = p.x + this_centerX;
                    p.y = p.y + this_centerY;
                }                
                
                // add the Transformed points to the points array
                for (int k = 0; k < this_points.size(); k++) {
                    points.add(this_points.get(k));
                }
            }
            
        // add the four corners of the map
        simpleGraph.nodes.add(new SimpleNode(boundary_left, boundary_top));
        simpleGraph.nodes.add(new SimpleNode(boundary_left, boundary_bottom));
        simpleGraph.nodes.add(new SimpleNode(boundary_right, boundary_top));
        simpleGraph.nodes.add(new SimpleNode(boundary_right, boundary_bottom));
        // give each point an ID number representing its index.
        // this will not change again for the entire program
        for (int i = 0; i < points.size(); i++) {
            simpleGraph.nodes.add(new SimpleNode(points.get(i).x, points.get(i).y)); //points.get(i).id = i;
        }
        
        // Attempt #3:
        //System.err.println("number of polygons: " + inputPolys.size());        
        System.err.println("number of nodes in the graph: " + simpleGraph.nodes.size());        
        for (int i = 0; i < simpleGraph.nodes.size(); i++) {
            for (int j = i+1; j < simpleGraph.nodes.size(); j++) {
                SimpleNode p1 = simpleGraph.nodes.get(i);
                SimpleNode p2 = simpleGraph.nodes.get(j);
                SimpleEdge l = new SimpleEdge(p1, p2, p1.distanceTo(p2));      // candidate line
                if (!lineCollidesPolys(l, inputPolys)) {
                    simpleGraph.edges.add(l);
                }
            }
        }
        System.err.println("Number of edges " + simpleGraph.edges.size());
        
        return simpleGraph;
     }
    
    
    /**
     * Return true if the given line collides with any lines in
     * any of the given polygons.
     * @param line The line that is being tested
     * @param polys The polygons to test again
     * @return True if the line collides with a polygon, false otherwise. 
     */
    public static boolean lineCollidesPolys(SimpleEdge line, ArrayList<ArrayList<SimplePoint>> polys) {
        for (ArrayList<SimplePoint> poly : polys) {
            for (int i = 0; i < poly.size(); i++) {
                int prevIndex = i-1;
                if (prevIndex < 0)
                    prevIndex = poly.size() - 1;  // wrap around
                
               // if (linesIntersect(line.p0.x, line.p0.y, 
                if (Line2D.linesIntersect(line.p0.x, line.p0.y, 
                        line.p1.x, line.p1.y,
                        poly.get(prevIndex).x, poly.get(prevIndex).y, 
                        poly.get(i).x, poly.get(i).y)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    
    
    
    private static double cross(SimplePoint p0, SimplePoint p1, SimplePoint p2) {
        return (p0.x - p1.x) * (p2.y - p1.y) - (p0.y - p1.y) * (p2.x - p1.x);
    }
}
