
package polyfromimagecombinedspring;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File; 
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;

/**
 *
 * @author Kevin
 */
public class PolyFromImageCombinedSpring {

    private final static Color[] distinctColors = {
            Color.RED, Color.BLUE, Color.CYAN, Color.ORANGE, Color.GREEN,
            Color.GRAY, Color.MAGENTA, Color.PINK, Color.DARK_GRAY,
            Color.RED.brighter(), Color.BLUE.brighter(), Color.CYAN.brighter(), 
            Color.ORANGE.darker(), Color.GREEN.darker()
        };
    
    /**
     * @param args the command line arguments
     * The first argument is the file name of the initial image file
     * and the second name is he name of the output image. file.
     */
    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Three arguments needed: initial image, version, progress images=1|0");
            return;
        } 
        BufferedImage imageRaw = null; // The initial input image.
        try {
            imageRaw = (BufferedImage) ImageIO.read(new File(args[0]));
        } catch(Exception e) {
            System.err.println(e);
        }
        if (imageRaw != null) {
            System.out.println("Loaded image successfully");
            boolean showProgressImages = Integer.parseInt(args[2]) == 1;
            int version = Integer.parseInt(args[1]); // 0 is with textures, 1 is with colors.
            int numClusters = 5;
            int textureWindow = 13;
            String clusteringMethod = "texture";
            if (version == 1)
                clusteringMethod = "color";
            else if (version == 2)
                clusteringMethod = "groundtruth";
            String outputStrPrefix = outputImgStr(args[0]) + "-v" + clusteringMethod;
            // get that output name by adding _out to the end of the input image name
            File outputImageClusters = new File(outputStrPrefix + "_clusters" + ".png");
            File outputImageSaliency = new File(outputStrPrefix + "_saliency" + ".png");
            File outputImageThreshold = new File(outputStrPrefix + "_threshold" + ".png");
            File outputImageGroups = new File(outputStrPrefix + "_groups" + ".png");
            File outputImagePolygons = new File(outputStrPrefix + "_polygons" + ".png");
            
            File outputImageOverlay = new File(outputStrPrefix + "_overlay" + ".png");
            File outputImageGraph = new File(outputStrPrefix + "_graph" + ".png");
            File outputJsonGraph = new File(outputStrPrefix + "_graph.json");
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
            File outputCsvRuntime = new File(outputStrPrefix + "_runtime_" + timeStamp + ".csv");
            long timeTextures = -1, timeStage1cluster = -1, timeStage1saliency = -1, timeStage2, timeStage3, timeStage4;
            long beforeTime = System.currentTimeMillis(); 
            
            int[][] clusterMembership = null;
            int[] clusterSaliencyOrdering = null;
            TextureDescription[] textureDescription = null;
            ColorCluster[] colorsArray = null;
                    
            if (version == 0) { // texture clustering
                System.out.print("Finding the texture descriptions...");
                textureDescription = TextureClusterer.findTextureDescription(imageRaw, textureWindow); 
                System.out.println("Finished finding texture descriptions.");
                timeTextures = System.currentTimeMillis() - beforeTime;
                
                beforeTime = System.currentTimeMillis();
                System.out.print("Calculating clusters... ");
                clusterMembership = TextureClusterer.textureClusteringIndices(imageRaw, textureDescription, numClusters);
                System.out.println("Finished finding clusters.");
                timeStage1cluster = System.currentTimeMillis() - beforeTime;
            
                beforeTime = System.currentTimeMillis();
                System.out.print("Finding most salient clusters... ");
                clusterSaliencyOrdering = TextureClusterer.findSalientImageClusters(imageRaw, textureDescription, clusterMembership, numClusters);
                System.out.println("Finished finding salient clusters.");
                timeStage1saliency = System.currentTimeMillis() - beforeTime;
            } else if (version == 1) { // color clustering
                beforeTime = System.currentTimeMillis();
                System.out.println("Biulding colors array...");
                colorsArray = new ColorCluster[ imageRaw.getWidth() * imageRaw.getHeight() ];
                for (int i = 0; i < imageRaw.getWidth(); i++) {
                    for (int j = 0; j < imageRaw.getHeight(); j++) {
                        Color pc = new Color(imageRaw.getRGB(i, j));
                        colorsArray[ (i * imageRaw.getHeight()) + j ] = new ColorCluster(pc, 0);
                    }
                }
                
                System.out.print("Calculating clusters... ");
                clusterMembership = TextureClusterer.colorClusteringIndices(imageRaw, colorsArray, numClusters);
                System.out.println("Finished finding clusters.");
                timeStage1cluster = System.currentTimeMillis() - beforeTime;
                
                beforeTime = System.currentTimeMillis();
                System.out.print("Finding most salient clusters... ");
                clusterSaliencyOrdering = TextureClusterer.findSalientImageClustersColor(imageRaw, colorsArray, clusterMembership, numClusters);
                System.out.println("Finished finding salient clusters.");
                timeStage1saliency = System.currentTimeMillis() - beforeTime;
            } else if (version == 2) { // ground truth image.
                clusterMembership = new int[ imageRaw.getWidth() ][ imageRaw.getHeight() ];
                clusterSaliencyOrdering = new int[ numClusters ]; // since this initialized to all zeros, cluster 0 will be the desired salient region
                for (int i = 0; i < imageRaw.getWidth(); i++) {
                    for (int j = 0; j < imageRaw.getHeight(); j++) {
                        Color pc = new Color(imageRaw.getRGB(i, j));
                        clusterMembership[i][j] = pc.getRed() < 32 ? 0 : 1; // if it is black, cluster 0. else, cluster 1
                    }
                }
            }
            
            if (version < 0 || version > 2) {
                System.err.println("Invalid version number");
                return;
            }
            
            // create a threshold image. true = black/obstacle. false = white.
            boolean[][] thresholdImage = new boolean[ imageRaw.getWidth() ][ imageRaw.getHeight() ];
            boolean[][] thresholdNoEdgeImage = new boolean[ imageRaw.getWidth() ][ imageRaw.getHeight() ];
            for (int i = 0; i < clusterMembership.length; i++) {
                for (int j = 0; j < clusterMembership[i].length; j++) {
                    thresholdImage[i][j] = ((clusterMembership[i][j] == clusterSaliencyOrdering[0]) || // two most salient clusters
                            (clusterMembership[i][j] == clusterSaliencyOrdering[1]));
                }
            }
            // edge removal on the threshold image
            for (int i = 1; i < thresholdImage.length - 1; i++) {
                for (int j = 1; j < thresholdImage[i].length - 1; j++) {
                    int k = 0; // see if this is an edge or not.
                    k += (thresholdImage[i + 1][j] ? 1 : 0);
                    k += (thresholdImage[i - 1][j] ? 1 : 0);
                    k += (thresholdImage[i][j + 1] ? 1 : 0);
                    k += (thresholdImage[i][j - 1] ? 1 : 0);
                    k += (thresholdImage[i + 1][j - 1] ? 1 : 0);
                    k += (thresholdImage[i + 1][j + 1] ? 1 : 0);
                    k += (thresholdImage[i - 1][j - 1] ? 1 : 0);
                    k += (thresholdImage[i - 1][j + 1] ? 1 : 0); 
                    if (k == 8)
                        thresholdNoEdgeImage[i][j] = false; // only have edge pixels
                    else
                        thresholdNoEdgeImage[i][j] = thresholdImage[i][j];
                }
            }
            
            ArrayList<SimplePoint> points = new ArrayList<>(); 
            for (int i = 1; i < thresholdNoEdgeImage.length; i++) {
                for (int j = 1; j < thresholdNoEdgeImage[i].length; j++) {
                    if (thresholdNoEdgeImage[i][j]) { 
                        points.add(new SimplePoint((int)i, (int)j));
                    }
                }
            }
            
            beforeTime = System.currentTimeMillis();
            // group points...
            System.out.print("Grouping " + (points.size()) +" points... ");
            ArrayList< ArrayList<SimplePoint> > groups = GeometricOperations.groupPoints(points, 2, 150, 10, 2);
            System.out.println("Finished grouping the points"); 
            timeStage2 = System.currentTimeMillis() - beforeTime;
            int stage2NumGroups = groups.size();
            
            beforeTime = System.currentTimeMillis();
            // convex hull...
            System.out.print("Constructing convex hulls of point groups...");
            ArrayList< ArrayList<SimplePoint> > polygons = GeometricOperations.convexHullOnPointGroups(groups, 3);
            System.out.println("Finished constructing convex hulls.");
            timeStage3 = System.currentTimeMillis() - beforeTime;
            int stage3NumPolygons = polygons.size();
            
            beforeTime = System.currentTimeMillis();
            // create the visbilitly graph
            System.out.print("Constructing visibility graph...");
            SimpleGraph graph = GeometricOperations.polygonsToGraph(polygons, 1.2, 0, imageRaw.getHeight(), 0, imageRaw.getWidth());
            System.out.println("Finished constructing convex hulls.");
            timeStage4 = System.currentTimeMillis() - beforeTime;
            
            System.out.print("Outputting images....");
            BufferedImage imageOutClusters = null;
            BufferedImage imageOutSaliency = null;
            BufferedImage imageOutGroups = null;
            BufferedImage imageOutThreshold = null;
            BufferedImage imageOutPolygons = null;
            if (showProgressImages) {
                imageOutClusters = new BufferedImage(imageRaw.getWidth(), imageRaw.getHeight(), BufferedImage.TYPE_INT_RGB);
                imageOutSaliency = new BufferedImage(imageRaw.getWidth(), imageRaw.getHeight(), BufferedImage.TYPE_INT_RGB);
                imageOutGroups = new BufferedImage(imageRaw.getWidth(), imageRaw.getHeight(), BufferedImage.TYPE_INT_RGB);
                imageOutThreshold = new BufferedImage(imageRaw.getWidth(), imageRaw.getHeight(), BufferedImage.TYPE_INT_RGB);
                imageOutPolygons = new BufferedImage(imageRaw.getWidth(), imageRaw.getHeight(), BufferedImage.TYPE_INT_RGB); 
            }            
            BufferedImage imageOutGraph = new BufferedImage(imageRaw.getWidth(), imageRaw.getHeight(), BufferedImage.TYPE_INT_RGB); 
            BufferedImage overlayImage = new BufferedImage(imageRaw.getWidth(), imageRaw.getHeight(), BufferedImage.TYPE_INT_RGB); 
            for (int i = 0; i < imageRaw.getWidth(); i++) {
                for (int j = 0; j < imageRaw.getHeight(); j++) {  
                    if (showProgressImages) {
                        imageOutClusters.setRGB(i, j, distinctColors[ clusterMembership[i][j] % distinctColors.length ].getRGB());

                        int srank = 0;
                        for (int s = 0; s < clusterSaliencyOrdering.length; s++)
                            if (clusterMembership[i][j] == clusterSaliencyOrdering[s])
                                srank = s;
                        int saliencyValue = (int)(255.0 * ((numClusters - srank) / (double)numClusters));
                        int sliencyNotRed = (srank == 0) ? 0 : saliencyValue;
                        sliencyNotRed = (srank == 1) ? 128 : sliencyNotRed;
                        imageOutSaliency.setRGB(i, j, (new Color(saliencyValue, sliencyNotRed, sliencyNotRed)).getRGB());

                        imageOutThreshold.setRGB(i, j, (thresholdNoEdgeImage[i][j] ? Color.BLACK : Color.WHITE).getRGB()); 
                        imageOutGroups.setRGB(i, j, Color.WHITE.getRGB()); // clear the groups image right now.
                    }
                    overlayImage.setRGB(i, j, imageRaw.getRGB(i, j));  // copy the raw image for the overlay image.
                    imageOutGraph.setRGB(i, j, imageRaw.getRGB(i, j)); 
                }
            } 
            
            if (showProgressImages) {
                // output the groups image and polgons.
                int groupIndex = 0;
                for (ArrayList<SimplePoint> group : groups) { 
                    for (SimplePoint p : group) {
                        imageOutGroups.setRGB((int)p.x, (int)p.y, distinctColors[ groupIndex % distinctColors.length ].getRGB());
                    }
                    groupIndex++; 
                }  
            }
            // output the polygons image at the same time.
            Graphics2D gOverlay = (Graphics2D)overlayImage.getGraphics();
            Graphics2D g = null;
            if (showProgressImages) {
                g = (Graphics2D)imageOutPolygons.getGraphics();
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, imageOutPolygons.getWidth(), imageOutPolygons.getHeight());
                g.setStroke(new BasicStroke(2));
            }
            gOverlay.setStroke(new BasicStroke(2));
            int polyIndex = 0;
            for (ArrayList<SimplePoint> polygon : polygons) { 
                // output this group as a polygon.
                int[] poly_x_points = new int[ polygon.size() ];
                int[] poly_y_points = new int[ polygon.size() ];
                int pIndex = 0;
                for (SimplePoint p : polygon) {
                    poly_x_points[pIndex] = (int)p.x;
                    poly_y_points[pIndex] = (int)p.y;
                    pIndex++;
                }
                polyIndex++;
                if (showProgressImages)
                    g.setColor(distinctColors[ polyIndex % distinctColors.length ]);
                gOverlay.setColor(distinctColors[ polyIndex % distinctColors.length ]);
                for (int i = 1; i < poly_x_points.length; i++) { 
                    if (showProgressImages)
                        g.drawLine(poly_x_points[i - 1], poly_y_points[i - 1], poly_x_points[i], poly_y_points[i]); 
                    gOverlay.drawLine(poly_x_points[i - 1], poly_y_points[i - 1], poly_x_points[i], poly_y_points[i]); 
                }
                if (showProgressImages)
                    g.drawLine(poly_x_points[0], poly_y_points[0], poly_x_points[poly_x_points.length - 1], poly_y_points[poly_x_points.length - 1]); 
                gOverlay.drawLine(poly_x_points[0], poly_y_points[0], poly_x_points[poly_x_points.length - 1], poly_y_points[poly_x_points.length - 1]); 
            }  
            
            // output the graph
            Graphics2D gGraph = (Graphics2D)imageOutGraph.getGraphics();
            gGraph.setColor(Color.GREEN);
            graph.edges.forEach((edge) -> {
                gGraph.drawLine((int)edge.p0.x, (int)edge.p0.y, (int)edge.p1.x, (int)edge.p1.y);
            });
            
            try {
                PrintWriter out = new PrintWriter(outputJsonGraph);
                out.println("{\"graph\":{");
                out.println("\"directed\": false,");
                out.println("\"nodes\": [");
                for (int i = 0; i < graph.nodes.size(); i++) { 
                    out.print("{\"id\": \"" + (graph.nodes.get(i).id) + "\"}"); 
                    if (i != graph.nodes.size() - 1)
                        out.println(",");
                }
                out.println("],");
                
                for (int i = 0; i < graph.edges.size(); i++) { 
                    out.print("{\"source\": \"" + (graph.edges.get(i).p0.id) + "\","); 
                    out.print("\"target\": \"" + (graph.edges.get(i).p1.id) + "\","); 
                    out.print("\"metadata\": {\"weight\": " + (graph.edges.get(i).getWeight()) + "}}"); 
                    if (i != graph.nodes.size() - 1)
                        out.println(",");
                }
                out.println("],"); 
                out.println("}}");
                out.close();
                
                // output the csv runtime data
                PrintWriter outCsv = new PrintWriter(outputCsvRuntime);
                outCsv.println("Runtime Name," + outputStrPrefix);
                outCsv.println("Input Image Width," + (imageRaw.getWidth()));
                outCsv.println("Input Image Height," + (imageRaw.getHeight()));
                outCsv.println("Number Groups Stage 2," + stage2NumGroups);
                outCsv.println("Number Polygons Stage 3," + stage3NumPolygons);
                outCsv.println("Number Nodes in Output Graph," + graph.nodes.size());
                outCsv.println("Number Edges in Output Graph," + graph.edges.size());
                outCsv.println("Texture Calculation Time," + timeTextures);
                outCsv.println("Stage 1 Cluster/Segmentation Time," + timeStage1cluster);
                outCsv.println("Stage 1 Saliency Region Time," + timeStage1saliency);
                outCsv.println("Stage 2 Grouping Time," + timeStage2);
                outCsv.println("Stage 3 Polygon Construction Time," + timeStage3);
                outCsv.println("Stage 4 Graph Construction Time," + timeStage4);
                outCsv.close();
                
                if (showProgressImages) {
                    ImageIO.write(imageOutClusters, "png", outputImageClusters); 
                    ImageIO.write(imageOutSaliency, "png", outputImageSaliency);
                    ImageIO.write(imageOutGroups, "png", outputImageGroups);
                    ImageIO.write(imageOutThreshold, "png", outputImageThreshold);
                    ImageIO.write(imageOutPolygons, "png", outputImagePolygons);
                }
                ImageIO.write(overlayImage, "png", outputImageOverlay);
                ImageIO.write(imageOutGraph, "png", outputImageGraph);
            } catch(Exception e) {
                System.err.println(e);
            } 
            System.out.println("Done.");
            
        }
    } 
    
    /**
     * Given a file input name, transform it to an output name.
     * @param inputImgStr The input file name
     * @return The output file name with no file extension.
     */
    public static String outputImgStr(String inputImgStr) {
        String[] seperatedInput = inputImgStr.split("/");
        String lastPart = seperatedInput[ seperatedInput.length - 1 ];
        String imageNameWithoutURL = lastPart.split("\\.")[0];
        return imageNameWithoutURL + "_out";
    }
    
}
