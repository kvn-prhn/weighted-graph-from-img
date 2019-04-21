
package polyfromimagecombinedspring;

import java.awt.Color;
import java.util.Set;

/**
 * This class is used to describe the texture of some window
 * The class contains lots of values which will all make up
 * a texture. This value can be returned as a feature vector.
 * @author Kevin
 */
public class TextureDescription {

    protected enum Channel { RED, GREEN, BLUE };
    
    public int cluster; // the index of the cluster this texture description is a member of.
    public double avgRed, avgBlue, avgGreen;  // Color averages
    public double stdRed, stdBlue, stdGreen;  // Color standard deviation
    public double localStdRed, localStdBlue, localStdGreen; // average standard deviations of smaller window sizes within the texture.
    public double avgGsThreshold, constantGsThreshold;
    public double avgRedThreshold, avgGreenThreshold, avgBlueThreshold;
    int n; // the width and height of the texture sample.
    
    /**
     * Create at texture description describing 
     * the image which is inside of the given region
     * @param imageRegion A 2D array of colors representing an image region.
     */
    public TextureDescription(Color imageRegion[]) {
        // calculate the values
        cluster = 0;
        n = (int)(Math.sqrt(imageRegion.length)); // assuming the imageRegion[0].length == imageRegion.length
        int n2 = n;
        double totalRed = 0;
        double totalGreen = 0;
        double totalBlue = 0;
        // calculate the means
        for (int i = 0; i < n; i++) { 
            totalRed += imageRegion[i].getRed();
            totalBlue += imageRegion[i].getGreen();
            totalGreen += imageRegion[i].getBlue();
        }
        avgRed = totalRed / n2;
        avgBlue = totalBlue / n2;
        avgGreen = totalGreen / n2;
        // calculate global standard deviation
        stdRed = stdOfRegion(imageRegion, n, Channel.RED, avgRed, 0, n, 0, n);
        stdBlue = stdOfRegion(imageRegion, n, Channel.BLUE, avgBlue, 0, n, 0, n);
        stdGreen = stdOfRegion(imageRegion, n, Channel.GREEN, avgGreen, 0, n, 0, n); 
        
        localStdRed = 0;
        localStdBlue = 0;
        localStdGreen = 0;
        // calculating the local standard deviation
        int window_split = 2; // on what axiss to split the window.
        int wsize = n / window_split;
        for (int out_i = 0; out_i < window_split; out_i++) {
            for (int out_j = 0; out_j < window_split; out_j++) {
                double localAvgRed = 0;
                double localAvgBlue = 0;
                double localAvgGreen = 0;
                for (int i = out_i * wsize; i < (out_i + 1) * wsize; i++) {
                    for (int j = out_j * wsize; j < (out_j + 1) * wsize; j++) {
                        localAvgRed += imageRegion[(i * n) + j].getRed();
                        localAvgBlue += imageRegion[(i * n) + j].getBlue();
                        localAvgGreen += imageRegion[(i * n) + j].getGreen();
                    }
                }
                localAvgRed /= wsize * wsize;
                localAvgBlue /= wsize * wsize;
                localAvgGreen /= wsize * wsize;
                
                localStdRed += stdOfRegion(imageRegion, n, Channel.RED, localAvgRed, 
                        out_i * wsize, (out_i + 1) * wsize, out_j * wsize, (out_j + 1) * wsize);
                localStdBlue += stdOfRegion(imageRegion, n, Channel.BLUE, localAvgBlue, 
                        out_i * wsize, (out_i + 1) * wsize, out_j * wsize, (out_j + 1) * wsize);
                localStdGreen += stdOfRegion(imageRegion, n, Channel.GREEN, localAvgGreen, 
                        out_i * wsize, (out_i + 1) * wsize, out_j * wsize, (out_j + 1) * wsize);
            }
        }
        // average the standard deviation totals.
        localStdRed /= window_split * window_split;
        localStdBlue /= window_split * window_split;
        localStdGreen /= window_split * window_split;
        
        
        // threshold the image and find the ratio of white to black images
        int[] gsRegion = new int[ imageRegion.length ];
        int totalGS = 0;
        for (int i = 0; i < gsRegion.length; i++) {
            gsRegion[i] = (imageRegion[i].getRed() + imageRegion[i].getGreen() + imageRegion[i].getBlue()) / 3;
            totalGS += gsRegion[i];
        }
        totalGS /= gsRegion.length;
        boolean[] thresholdGSRegion = new boolean[ gsRegion.length ];
        boolean[] thresholdConstact = new boolean[ gsRegion.length ];
        int numAvgTrue = 0, numConstTrue = 0;
        int numAvgRedTrue = 0,  numAvgGreenTrue = 0,  numAvgBlueTrue = 0;
        for (int i = 0; i < gsRegion.length; i++) {
            thresholdGSRegion[i] = gsRegion[i] >= totalGS; 
            numAvgTrue += (thresholdGSRegion[i] ? 1 : 0);
            numConstTrue += (gsRegion[i] > 128 ? 1 : 0);
        }
        avgGsThreshold = (int)( 255 * ((double)numAvgTrue / (double)thresholdGSRegion.length));
        constantGsThreshold = (int)( 255 * ((double)numConstTrue / (double)thresholdGSRegion.length));
        avgRedThreshold = (int)( 255 * ((double)numAvgRedTrue / (double)thresholdGSRegion.length));
        avgGreenThreshold = (int)( 255 * ((double)numAvgGreenTrue / (double)thresholdGSRegion.length));
        avgBlueThreshold = (int)( 255 * ((double)numAvgBlueTrue / (double)thresholdGSRegion.length));
    }
    
   /**
    * Create a texture description that is the 
    * mean texture description of a set of
    * texture descriptions.
    * @param arr 
    */
    public TextureDescription(Set<TextureDescription> arr) {
        avgRed = 0; avgBlue = 0; avgGreen = 0;
        stdRed = 0; stdBlue = 0; stdGreen = 0;
        //localStdRed = 0; localStdBlue = 0; localStdGreen = 0;
        arr.stream().forEach((desc) -> {
            avgRed += desc.avgRed;
            avgBlue += desc.avgBlue;
            avgGreen += desc.avgGreen;
            stdRed += desc.stdRed;
            stdBlue += desc.stdBlue;
            stdGreen += desc.stdGreen;
            localStdRed += desc.localStdRed;
            localStdBlue += desc.localStdBlue;
            localStdGreen += desc.localStdGreen; 
            avgGsThreshold += desc.avgGsThreshold;
            constantGsThreshold += desc.constantGsThreshold;
            avgRedThreshold += desc.avgRedThreshold;
            avgGreenThreshold += desc.avgGreenThreshold;
            avgBlueThreshold += desc.avgBlueThreshold;
        });
        avgRed /= arr.size();
        avgBlue /= arr.size();
        avgGreen /= arr.size();
        stdRed /= arr.size();
        stdBlue /= arr.size();
        stdGreen /= arr.size();
        localStdRed /= arr.size();
        localStdBlue /= arr.size();
        localStdGreen /= arr.size(); 
        avgGsThreshold /= arr.size();
        constantGsThreshold /= arr.size();
        avgRedThreshold /= arr.size();
        avgGreenThreshold /= arr.size();
        avgBlueThreshold /= arr.size();
    }
    
    // private function for making calculating the local average standard deviation easier 
    private double stdOfRegion(Color imgReg[], int n, Channel c, double avg, int startI, int endI, int startJ, int endJ) {
        //System.out.println("startI=" + startI + ", endI=" + endI + ", startJ=" + startJ + ", endJ=" + endJ);
        double std = 0;
        for (int i = startI; i < endI; i++) {
            for (int j = startJ; j < endJ; j++) {
                switch(c) {
                    case RED:   std += Math.pow(avg - imgReg[(i * n) + j].getRed(), 2);      break;
                    case GREEN: std += Math.pow(avg - imgReg[(i * n) + j].getGreen(), 2);    break;
                    case BLUE:  std += Math.pow(avg - imgReg[(i * n) + j].getBlue(), 2);      break;
                }   
            }
        }
        int regionSize = endI - startI;
        //System.out.println("The standard deviation is: " + Math.sqrt(std / Math.pow(imgReg.length - 1, 2)));
        return Math.sqrt(std / Math.pow(regionSize - 1, 2));
    }
    /**
     * Computes the average difference of all features
     * with some other texture description, giving a
     * similarity difference.
     * @param other The other texture description.
     * @return The average difference of features. If the 
     * two textures are exactly equal then this is 0.
     */
    public double similarity(TextureDescription other) {
        return (Math.abs(avgRed - other.avgRed) + 
                Math.abs(avgBlue - other.avgBlue) + 
                Math.abs(avgGreen - other.avgGreen) + 
                Math.abs(stdRed - other.stdRed) + 
                Math.abs(stdBlue - other.stdBlue) + 
                Math.abs(stdGreen - other.stdGreen) +
                Math.abs(localStdRed - other.localStdRed) +
                Math.abs(localStdBlue - other.localStdBlue) +
                Math.abs(localStdGreen - other.localStdGreen) +  
                Math.abs(avgGsThreshold - other.avgGsThreshold) +
                Math.abs(constantGsThreshold - other.constantGsThreshold) + 
                Math.abs(avgRedThreshold - other.avgRedThreshold) + 
                Math.abs(avgGreenThreshold - other.avgGreenThreshold) + 
                Math.abs(avgBlueThreshold - other.avgBlueThreshold)) / 15.0; 
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("\navgRed=");
        str.append(avgRed);
        str.append("\navgBlue=");
        str.append(avgBlue);
        str.append("\navgGreen=");
        str.append(avgGreen);
        str.append("\nstdRed=");
        str.append(stdRed);
        str.append("\nstdBlue=");
        str.append(stdBlue);
        str.append("\nstdGreen=");
        str.append(stdGreen);
        str.append("\nlocalStdRed=");
        str.append(localStdRed);
        str.append("\nlocalStdBlue=");
        str.append(localStdBlue);
        str.append("\nlocalStdGreen=");
        str.append(localStdGreen);   
        str.append("\navgGsThreshold=");
        str.append(avgGsThreshold);
        str.append("\nconstantGsThreshold=");
        str.append(constantGsThreshold);
        str.append("\navgRedThreshold=");
        str.append(avgRedThreshold);
        str.append("\navgGreenThreshold=");
        str.append(avgGreenThreshold);
        str.append("\navgBlueThreshold=");
        str.append(avgBlueThreshold);
        return str.toString();
    }
    
    
    
}
