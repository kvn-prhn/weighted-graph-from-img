
package polyfromimagecombinedspring;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set; 

/**
 * This class will take in an input image and separate
 * it by clusters based on textures using k-means clustering.
 * @author Kevin
 */
public class TextureClusterer {

    /**
     * Find the texture feature vector for the given image using the given sample size.
     * @param imageRaw The input image
     * @param textureSampleSize The sample size, also is the window size around each pixel
     * @return An array of texture description objects for the input image.
     */
    public static TextureDescription[] findTextureDescription(BufferedImage imageRaw, int textureSampleSize) {
        TextureDescription[] textureDescriptions = new TextureDescription[imageRaw.getWidth() * imageRaw.getHeight()];
        int textSampleHalf = (textureSampleSize + 1) / 2;
        // the texture of a pixel is described by the texture in a window around it.
        for (int x = 0; x < imageRaw.getWidth(); x++) {
            for (int y = 0; y < imageRaw.getHeight(); y++) {
                Color[] windowColor = new Color[textureSampleSize * textureSampleSize];
                // populate the window size with colors.
                int localWX = 0;
                for (int wx = x - textSampleHalf; wx < x + textSampleHalf; wx++) {
                    int localWY = 0; // the window location
                    if (localWX < textureSampleSize && localWY < textureSampleSize) {
                        for (int wy = y - textSampleHalf; wy < y + textSampleHalf; wy++) {
                            int realWX = wx; //Math.max(0, Math.min(imageRaw.getWidth() - 1, wx));
                            int realWY = wy; //Math.max(0, Math.min(imageRaw.getHeight() - 1, wy));
                            // System.out.print(realWX + ", " + realWY + " => ");
                            if (realWX < 0) {
                                realWX = Math.abs(realWX);
                            } else if (realWX >= imageRaw.getWidth() - 1) {
                                int diff = realWX - (imageRaw.getWidth() - 1);
                                realWX = imageRaw.getWidth() - 1 - diff;
                            }
                            if (realWY < 0) {
                                realWY = Math.abs(realWY);
                            } else if (realWY >= imageRaw.getHeight() - 1) {
                                int diff = realWY - (imageRaw.getHeight() - 1);
                                realWY = imageRaw.getHeight() - 1 - diff;
                            } 
                            int wcIndex = Math.min((localWX * textureSampleSize) + localWY, windowColor.length - 1);
                            windowColor[wcIndex] = new Color(imageRaw.getRGB(realWX, realWY));
                            localWY++;
                        }
                    }
                    localWX++;
                } 
                textureDescriptions[ (x * imageRaw.getHeight()) + y ] = new TextureDescription(windowColor);
            }
        }
        return textureDescriptions;
    }
    
    
    /**
     * Perform k means clustering for the given image with
     * the given texture description.
     * @param imageRaw The input image
     * @param textureDescriptions The texture descriptions for the input image
     * @param numClusters The number of clusters
     * @return A 2d array that is the same size as the input image
     *  where each entry is the index of the cluster that pixel is a member of.
     */
    public static int[][] textureClusteringIndices(BufferedImage imageRaw, 
            TextureDescription[] textureDescriptions, int numClusters) {
        // an array of indices referencing texture descriptions in the textureDescriptions array. 
        TextureDescription[] centroids = new TextureDescription[ numClusters ];

        // randomize the centroids.
        for (int i = 0; i < numClusters; i++) { 
            int x = (i * 197) % imageRaw.getWidth();
            int y = (i * 137) % imageRaw.getHeight(); 
            centroids[i] = textureDescriptions[ (x * imageRaw.getHeight()) + y ];
        }

        int numComputations = 0;
        double averageDelta;
        while (numComputations < 1000) {
            // find the cluster membership of each point.
            Arrays.stream(textureDescriptions).forEach((desc) -> {
                int closest = 0;
                double closeSim = centroids[0].similarity(desc);
                for (int i = 0; i < centroids.length; i++) { 
                    double s = centroids[i].similarity(desc);
                    if (s < closeSim) {
                        closeSim = s;
                        closest = i;
                    }
                }
                desc.cluster = closest; 
            });

            // recompute the centroids.
            Map<Integer, Set<TextureDescription>> clusterMembership = new HashMap<>();
            for (int i = 0; i < numClusters; i++) {
                clusterMembership.put(i, new HashSet<>());
            }
            Arrays.stream(textureDescriptions).forEach((desc) -> {
                clusterMembership.get(desc.cluster).add(desc);
            });
            // see how far the clusters moved to see if we keep iterating.
            averageDelta = 0;
            for (int i = 0; i < numClusters; i++) {
                TextureDescription newCenter = new TextureDescription(clusterMembership.get(i));
                double diff = centroids[i].similarity(newCenter);
                averageDelta += diff;
                centroids[i] = newCenter;
            }
            averageDelta /= numClusters;
            if (averageDelta < 0.01) {  // check if the delta is small enough to be done.
                break;
            }
            numComputations++;
        }
        
        int[][] pixelClusterMembership = new int[ imageRaw.getWidth() ][ imageRaw.getHeight() ];
        for (int i = 0; i < imageRaw.getWidth(); i++) {
            for (int j = 0; j < imageRaw.getHeight(); j++) { 
                pixelClusterMembership[i][j] = textureDescriptions[ (i * imageRaw.getHeight()) + j ].cluster;
            }
        } 
        return pixelClusterMembership;
    }
    
    /**
     * Perform k means clustering for the given image with
     * the given texture description.
     * @param imageRaw The input image
     * @param numClusters The number of clusters
     * @return A 2d array that is the same size as the input image
     *  where each entry is the index of the cluster that pixel is a member of.
     */
    public static int[][] colorClusteringIndices(BufferedImage imageRaw, 
            ColorCluster[] colorsArray, int numClusters) {
        // an array of indices referencing color values in the colorsArray array. 
        ColorCluster[] centroids = new ColorCluster[ numClusters ];
        
        // randomize the centroids.
        for (int i = 0; i < numClusters; i++) { 
            int x = (i * 197) % imageRaw.getWidth();
            int y = (i * 137) % imageRaw.getHeight(); 
            centroids[i] = colorsArray[ (x * imageRaw.getHeight()) + y ];
        }

        int numComputations = 0;
        double averageDelta;
        while (numComputations < 1000) {
            // find the cluster membership of each point.
            Arrays.stream(colorsArray).forEach((c) -> {
                int closest = 0;
                double closeSim = c.similarity(centroids[0]); // init val
                for (int i = 0; i < centroids.length; i++) { 
                    double s = c.similarity(centroids[i]);
                    if (s < closeSim) {
                        closeSim = s;
                        closest = i;
                    }
                }
                c.cluster = closest; 
            });

            // recompute the centroids.
            Map<Integer, Set<ColorCluster>> clusterMembership = new HashMap<>();
            for (int i = 0; i < numClusters; i++) {
                clusterMembership.put(i, new HashSet<>());
            }
            Arrays.stream(colorsArray).forEach((desc) -> {
                clusterMembership.get(desc.cluster).add(desc);
            });
            // see how far the clusters moved to see if we keep iterating.
            averageDelta = 0;
            for (int i = 0; i < numClusters; i++) {
                ColorCluster newCenter = new ColorCluster(clusterMembership.get(i));
                double diff = centroids[i].similarity(newCenter);
                averageDelta += diff;
                centroids[i] = newCenter;
            }
            averageDelta /= numClusters;
            if (averageDelta < 0.01) {  // check if the delta is small enough to be done.
                break;
            }
            numComputations++;
        }
        
        int[][] pixelClusterMembership = new int[ imageRaw.getWidth() ][ imageRaw.getHeight() ];
        for (int i = 0; i < imageRaw.getWidth(); i++) {
            for (int j = 0; j < imageRaw.getHeight(); j++) { 
                pixelClusterMembership[i][j] = colorsArray[ (i * imageRaw.getHeight()) + j ].cluster;
            }
        } 
        return pixelClusterMembership;
    }
    
    
    /**
     * Given a 2d array where indices represent the region membership, 
     * convert it to a transformed 2d array where no regions connect.
     * @param regionMembership The initial region membership array
     * @param numClusters The number of clusters initially.
     * @return 
     */
    public static int[][] breakUpNonConnectedRegions(int[][] regionMembership, int numClusters) {
        int[][] t = new int[ regionMembership.length ][ regionMembership[0].length ]; // the transformed array

        // map the cluster index to a list of points which have that cluster index.
        Map<Integer, ArrayList< Integer > > clusterPointsX = new HashMap<>();
        Map<Integer, ArrayList< Integer > > clusterPointsY = new HashMap<>();
        for (int i = 1; i < regionMembership.length - 1; i++) {
            for (int j = 1; j < regionMembership[i].length - 1; j++) {
                int k = 0; // track how many same-cluster pixels are around it.
                k += (regionMembership[i][j] == regionMembership[i + 1][j] ? 1 : 0);
                k += (regionMembership[i][j] == regionMembership[i - 1][j] ? 1 : 0);
                k += (regionMembership[i][j] == regionMembership[i][j + 1] ? 1 : 0);
                k += (regionMembership[i][j] == regionMembership[i][j - 1] ? 1 : 0);
                if (k < 4) {
                    if (!clusterPointsX.containsKey(regionMembership[i][j])) {
                        clusterPointsX.put(regionMembership[i][j], new ArrayList<>());
                        clusterPointsY.put(regionMembership[i][j], new ArrayList<>());
                    }
                    clusterPointsX.get(regionMembership[i][j]).add(i);
                    clusterPointsY.get(regionMembership[i][j]).add(j);
                }
            }
        }
        // now analyze each index seperately.
        
        // break up all of the points into individual groups and combine
        // until they can't be combined any more. 
        int newRegionNumbering = 0;
        for (int rIndex = 0; rIndex < numClusters; rIndex++) {
            System.out.println("\nWorking on index " + rIndex);
            // the number of initial groups is equal to the number of points for that index
            ArrayList< ArrayList< Integer > > sepGroupsX = new ArrayList<>( clusterPointsX.get(rIndex).size() );
            ArrayList< ArrayList< Integer > > sepGroupsY = new ArrayList<>( clusterPointsY.get(rIndex).size() );
            // setup the initial groups. Each group is initiallze size 1
            for (int k = 0; k < clusterPointsX.get(rIndex).size(); k++) {
                ArrayList< Integer > groupX = new ArrayList<>();
                ArrayList< Integer > groupY = new ArrayList<>();
                // only add it if it is an edge point... 
                groupX.add(clusterPointsX.get(rIndex).get(k));
                groupY.add(clusterPointsY.get(rIndex).get(k));
                // find a good position to go so the groups are initially sorted the x axis
                int indexToAdd = 0;
               /* for (int f = 1; f < sepGroupsX.size(); f++) {
                    if (sepGroupsX.get(f - 1).get(0) <= clusterPointsX.get(rIndex).get(k) && 
                            sepGroupsX.get(f).get(0) > clusterPointsX.get(rIndex).get(k)) {
                        indexToAdd = f;
                    }
                } */
                // sepGroupsX.add(indexToAdd, groupX);
                // sepGroupsY.add(indexToAdd, groupY);
                sepGroupsX.add(groupX);
                sepGroupsY.add(groupY);
            }
            // now we hae to keep combining groups until we can't combine any more. 
            System.out.println("Now combining...");
            double min_dist_to_group = 2;
            boolean combined = true;
            int iterations = 0;
            while (combined) {
                System.out.println("Iterations=" + iterations);
                iterations++;
                combined = false;
                for (int i = 0; i < sepGroupsX.size(); i++) {
                    // look at each other group- if at least
                    // one element in the two groups are within a minimum
                    // distance, then you can combine the groups
                    // always check for the ith index as well because the size of groups will change.
                    for (int j = i + 1; i < sepGroupsX.size() && j < sepGroupsX.size(); j++) {

                        double min_dist = min_dist_to_group + 1;    // also track the max_dist
                        for (int me = 0; me < sepGroupsX.get(i).size(); me++) {
                            for (int other = 0; other < sepGroupsX.get(j).size(); other++) {
                             /* System.out.println("sepGroupsX.size()=" + sepGroupsX.size());
                                System.out.println("sepGroupsX.get(i).size()=" + sepGroupsX.get(i).size());
                                System.out.println("sepGroupsY.size()=" + sepGroupsY.size());
                                System.out.println("sepGroupsY.get(i).size()=" + sepGroupsY.get(i).size()); */
                                double diffX = sepGroupsX.get(i).get(me) - sepGroupsX.get(j).get(other); 
                                double diffY = sepGroupsY.get(i).get(me) - sepGroupsY.get(j).get(other); 
                                double dist = Math.sqrt((diffX * diffX) + (diffY * diffY));
                                if (dist < min_dist) {
                                    min_dist = dist;
                                } 
                            }
                        }
                        // combine groups if they are close enough andn ot too far away
                        if (min_dist < min_dist_to_group) {
                            // combine 
                            for (int w = 0; w < sepGroupsX.get(j).size(); w++) {
                                sepGroupsX.get(i).add(sepGroupsX.get(j).get(w));
                                sepGroupsY.get(i).add(sepGroupsY.get(j).get(w));
                            }
                            ArrayList<Integer> removedX = sepGroupsX.remove(j);
                            ArrayList<Integer> removedY = sepGroupsY.remove(j);
                            combined = true;
                        }

                    } 
                }
            } 
            // number each new region into t
            for (int i = 0; i < sepGroupsX.size(); i++) {
                for (int g = 0; g < sepGroupsX.get(i).size(); g++) { 
                    t[ sepGroupsX.get(i).get(g) ][ sepGroupsY.get(i).get(g) ] = newRegionNumbering;
                }
                newRegionNumbering++; 
            }
        }
        return t;
    }
    
    /**
     * Given information about an image, the textures, and 
     * the cluster membership, return an ordered array where 
     * each element is a cluster index, and they are ordered
     * according to saliency. 
     * @param imageRaw
     * @param textureDescription
     * @param clusterMembership
     * @param numClusters
     * @return A ranked ordering of cluster indices based on saliency. 
     */
    public static int[] findSalientImageClusters(BufferedImage imageRaw, 
        TextureDescription[] textureDescription, 
        int[][] clusterMembership, int numClusters) { 
        Map<Integer, Set<TextureDescription>> descByCluster = new HashMap<>();
        for (int i = 0; i < textureDescription.length; i++) {
            int xPos = i % imageRaw.getWidth();
            int yPos = i / imageRaw.getWidth();
            if (!descByCluster.containsKey(clusterMembership[xPos][yPos])) {
                descByCluster.put(clusterMembership[xPos][yPos], new HashSet<>());
            }
            descByCluster.get(clusterMembership[xPos][yPos]).add(textureDescription[i]);
        }
        // find the average values for different clusters
        TextureDescription[] clusterAverages = new TextureDescription[ numClusters ];
        TextureDescription imageAverage;
        for (int i = 0; i < clusterAverages.length; i++) {
            clusterAverages[i] = new TextureDescription(descByCluster.get(i));
        }
        int[] clusterSaliencyOrdering = new int[ numClusters ]; // order the clusters by saliency
        double[] clusterSaliencyValues = new double[ numClusters ];
        for (int i = 0; i < numClusters; i++) {
            clusterSaliencyOrdering[i] = i; // initial setup. 
            Set<TextureDescription> others = new HashSet<>(); // calculate saliency of each region
            for (int k = 0; k < numClusters; k++) {
                if (k != i) {
                    others.addAll(descByCluster.get(k));
                }
            }
            TextureDescription averageOthers = new TextureDescription(others);
            clusterSaliencyValues[i] = (clusterAverages[i].similarity(averageOthers));
        }
        // sort the saliency result list.
        for (int i = 0; i < numClusters - 1; i++) {
            int min = i+1;
            for (int j = i + 1; j < numClusters; j++) {
                if (clusterSaliencyValues[ clusterSaliencyOrdering[j] ] < clusterSaliencyValues[ clusterSaliencyOrdering[min] ])
                    min = j; 
            }
            int temp = clusterSaliencyOrdering[min];
            clusterSaliencyOrdering[min] = clusterSaliencyOrdering[i];
            clusterSaliencyOrdering[i] = temp;
        }
        // System.out.println("The saliency values: ");
        // for (int i =0; i  < numClusters; i++)
        //     System.out.println(clusterSaliencyOrdering[i] + " => " + clusterSaliencyValues[ clusterSaliencyOrdering[i] ]);

        return clusterSaliencyOrdering;
    }
    
    
    /**
     * Given information about an image, the colors, and 
     * the cluster membership, return an ordered array where 
     * each element is a cluster index, and they are ordered
     * according to saliency. 
     * @param imageRaw
     * @param colorsArray
     * @param clusterMembership
     * @param numClusters
     * @return A ranked ordering of cluster indices based on saliency. 
     */
    public static int[] findSalientImageClustersColor(BufferedImage imageRaw, 
        ColorCluster[] colorsArray, 
        int[][] clusterMembership, int numClusters) { 
        Map<Integer, Set<ColorCluster>> colorByCluster = new HashMap<>();
        for (int i = 0; i < colorsArray.length; i++) {
            int xPos = i % imageRaw.getWidth();
            int yPos = i / imageRaw.getWidth();
            if (!colorByCluster.containsKey(clusterMembership[xPos][yPos])) {
                colorByCluster.put(clusterMembership[xPos][yPos], new HashSet<>());
            }
            colorByCluster.get(clusterMembership[xPos][yPos]).add(colorsArray[i]);
        }
        // find the average values for different clusters
        ColorCluster[] clusterAverages = new ColorCluster[ numClusters ];
        ColorCluster imageAverage;
        for (int i = 0; i < clusterAverages.length; i++) {
            clusterAverages[i] = new ColorCluster(colorByCluster.get(i));
        }
        int[] clusterSaliencyOrdering = new int[ numClusters ]; // order the clusters by saliency
        double[] clusterSaliencyValues = new double[ numClusters ];
        for (int i = 0; i < numClusters; i++) {
            clusterSaliencyOrdering[i] = i; // initial setup. 
            Set<ColorCluster> others = new HashSet<>(); // calculate saliency of each region
            for (int k = 0; k < numClusters; k++) {
                if (k != i) {
                    others.addAll(colorByCluster.get(k));
                }
            }
            ColorCluster averageOthers = new ColorCluster(others);
            clusterSaliencyValues[i] = (clusterAverages[i].similarity(averageOthers));
        }
        // sort the saliency result list.
        for (int i = 0; i < numClusters - 1; i++) {
            int min = i+1;
            for (int j = i + 1; j < numClusters; j++) {
                if (clusterSaliencyValues[ clusterSaliencyOrdering[j] ] < clusterSaliencyValues[ clusterSaliencyOrdering[min] ])
                    min = j; 
            }
            int temp = clusterSaliencyOrdering[min];
            clusterSaliencyOrdering[min] = clusterSaliencyOrdering[i];
            clusterSaliencyOrdering[i] = temp;
        }
        // System.out.println("The saliency values: ");
        // for (int i =0; i  < numClusters; i++)
        //     System.out.println(clusterSaliencyOrdering[i] + " => " + clusterSaliencyValues[ clusterSaliencyOrdering[i] ]);

        return clusterSaliencyOrdering;
    }
    
    public static boolean isPointsAdjacent(int[] a, int[] b) { 
        return (Math.abs(a[0] - b[0]) <= 1 && Math.abs(a[1] - b[1]) <= 1);
    }
}
