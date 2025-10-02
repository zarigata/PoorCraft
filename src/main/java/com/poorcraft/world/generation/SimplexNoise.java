package com.poorcraft.world.generation;

import java.util.Random;

/**
 * Simplex Noise implementation for terrain generation.
 * 
 * Based on Ken Perlin's improved noise algorithm.
 * This is the secret sauce that makes terrain look natural instead of random garbage.
 * I don't fully understand the math but it works so... don't touch it.
 */
public class SimplexNoise {
    
    // Gradient vectors for 3D noise
    private static final int[][] GRAD3 = {
        {1,1,0}, {-1,1,0}, {1,-1,0}, {-1,-1,0},
        {1,0,1}, {-1,0,1}, {1,0,-1}, {-1,0,-1},
        {0,1,1}, {0,-1,1}, {0,1,-1}, {0,-1,-1}
    };
    
    // Permutation table
    private final int[] perm = new int[512];
    private final int[] permMod12 = new int[512];
    
    private final long seed;
    
    /**
     * Creates a new SimplexNoise generator with the given seed.
     * 
     * @param seed Random seed for reproducible noise
     */
    public SimplexNoise(long seed) {
        this.seed = seed;
        
        // Generate permutation table based on seed
        Random random = new Random(seed);
        int[] p = new int[256];
        for (int i = 0; i < 256; i++) {
            p[i] = i;
        }
        
        // Shuffle using Fisher-Yates
        for (int i = 255; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temp = p[i];
            p[i] = p[j];
            p[j] = temp;
        }
        
        // Duplicate permutation table to avoid overflow
        for (int i = 0; i < 512; i++) {
            perm[i] = p[i & 255];
            permMod12[i] = perm[i] % 12;
        }
    }
    
    /**
     * Generates 2D simplex noise at the given coordinates.
     * 
     * @param xin X coordinate
     * @param yin Y coordinate
     * @return Noise value in range [-1, 1]
     */
    public double noise2D(double xin, double yin) {
        final double F2 = 0.5 * (Math.sqrt(3.0) - 1.0);
        final double G2 = (3.0 - Math.sqrt(3.0)) / 6.0;
        
        double n0, n1, n2;
        
        // Skew input space to determine which simplex cell we're in
        double s = (xin + yin) * F2;
        int i = fastFloor(xin + s);
        int j = fastFloor(yin + s);
        
        double t = (i + j) * G2;
        double X0 = i - t;
        double Y0 = j - t;
        double x0 = xin - X0;
        double y0 = yin - Y0;
        
        // Determine which simplex we're in
        int i1, j1;
        if (x0 > y0) {
            i1 = 1; j1 = 0;
        } else {
            i1 = 0; j1 = 1;
        }
        
        // Offsets for middle and last corners
        double x1 = x0 - i1 + G2;
        double y1 = y0 - j1 + G2;
        double x2 = x0 - 1.0 + 2.0 * G2;
        double y2 = y0 - 1.0 + 2.0 * G2;
        
        // Work out hashed gradient indices
        int ii = i & 255;
        int jj = j & 255;
        int gi0 = permMod12[ii + perm[jj]];
        int gi1 = permMod12[ii + i1 + perm[jj + j1]];
        int gi2 = permMod12[ii + 1 + perm[jj + 1]];
        
        // Calculate contribution from three corners
        double t0 = 0.5 - x0 * x0 - y0 * y0;
        if (t0 < 0) {
            n0 = 0.0;
        } else {
            t0 *= t0;
            n0 = t0 * t0 * dot(GRAD3[gi0], x0, y0);
        }
        
        double t1 = 0.5 - x1 * x1 - y1 * y1;
        if (t1 < 0) {
            n1 = 0.0;
        } else {
            t1 *= t1;
            n1 = t1 * t1 * dot(GRAD3[gi1], x1, y1);
        }
        
        double t2 = 0.5 - x2 * x2 - y2 * y2;
        if (t2 < 0) {
            n2 = 0.0;
        } else {
            t2 *= t2;
            n2 = t2 * t2 * dot(GRAD3[gi2], x2, y2);
        }
        
        // Add contributions and scale to [-1, 1]
        return 70.0 * (n0 + n1 + n2);
    }
    
    /**
     * Generates 3D simplex noise at the given coordinates.
     * 
     * @param xin X coordinate
     * @param yin Y coordinate
     * @param zin Z coordinate
     * @return Noise value in range [-1, 1]
     */
    public double noise3D(double xin, double yin, double zin) {
        final double F3 = 1.0 / 3.0;
        final double G3 = 1.0 / 6.0;
        
        double n0, n1, n2, n3;
        
        // Skew input space
        double s = (xin + yin + zin) * F3;
        int i = fastFloor(xin + s);
        int j = fastFloor(yin + s);
        int k = fastFloor(zin + s);
        
        double t = (i + j + k) * G3;
        double X0 = i - t;
        double Y0 = j - t;
        double Z0 = k - t;
        double x0 = xin - X0;
        double y0 = yin - Y0;
        double z0 = zin - Z0;
        
        // Determine which simplex we're in
        int i1, j1, k1;
        int i2, j2, k2;
        
        if (x0 >= y0) {
            if (y0 >= z0) {
                i1 = 1; j1 = 0; k1 = 0; i2 = 1; j2 = 1; k2 = 0;
            } else if (x0 >= z0) {
                i1 = 1; j1 = 0; k1 = 0; i2 = 1; j2 = 0; k2 = 1;
            } else {
                i1 = 0; j1 = 0; k1 = 1; i2 = 1; j2 = 0; k2 = 1;
            }
        } else {
            if (y0 < z0) {
                i1 = 0; j1 = 0; k1 = 1; i2 = 0; j2 = 1; k2 = 1;
            } else if (x0 < z0) {
                i1 = 0; j1 = 1; k1 = 0; i2 = 0; j2 = 1; k2 = 1;
            } else {
                i1 = 0; j1 = 1; k1 = 0; i2 = 1; j2 = 1; k2 = 0;
            }
        }
        
        // Offsets for corners
        double x1 = x0 - i1 + G3;
        double y1 = y0 - j1 + G3;
        double z1 = z0 - k1 + G3;
        double x2 = x0 - i2 + 2.0 * G3;
        double y2 = y0 - j2 + 2.0 * G3;
        double z2 = z0 - k2 + 2.0 * G3;
        double x3 = x0 - 1.0 + 3.0 * G3;
        double y3 = y0 - 1.0 + 3.0 * G3;
        double z3 = z0 - 1.0 + 3.0 * G3;
        
        // Hashed gradient indices
        int ii = i & 255;
        int jj = j & 255;
        int kk = k & 255;
        int gi0 = permMod12[ii + perm[jj + perm[kk]]];
        int gi1 = permMod12[ii + i1 + perm[jj + j1 + perm[kk + k1]]];
        int gi2 = permMod12[ii + i2 + perm[jj + j2 + perm[kk + k2]]];
        int gi3 = permMod12[ii + 1 + perm[jj + 1 + perm[kk + 1]]];
        
        // Calculate contributions
        double t0 = 0.6 - x0 * x0 - y0 * y0 - z0 * z0;
        if (t0 < 0) {
            n0 = 0.0;
        } else {
            t0 *= t0;
            n0 = t0 * t0 * dot(GRAD3[gi0], x0, y0, z0);
        }
        
        double t1 = 0.6 - x1 * x1 - y1 * y1 - z1 * z1;
        if (t1 < 0) {
            n1 = 0.0;
        } else {
            t1 *= t1;
            n1 = t1 * t1 * dot(GRAD3[gi1], x1, y1, z1);
        }
        
        double t2 = 0.6 - x2 * x2 - y2 * y2 - z2 * z2;
        if (t2 < 0) {
            n2 = 0.0;
        } else {
            t2 *= t2;
            n2 = t2 * t2 * dot(GRAD3[gi2], x2, y2, z2);
        }
        
        double t3 = 0.6 - x3 * x3 - y3 * y3 - z3 * z3;
        if (t3 < 0) {
            n3 = 0.0;
        } else {
            t3 *= t3;
            n3 = t3 * t3 * dot(GRAD3[gi3], x3, y3, z3);
        }
        
        // Add contributions and scale
        return 32.0 * (n0 + n1 + n2 + n3);
    }
    
    /**
     * Generates multi-octave 2D noise for more natural-looking terrain.
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @param octaves Number of noise layers to combine
     * @param persistence Amplitude multiplier per octave (typically 0.5)
     * @param lacunarity Frequency multiplier per octave (typically 2.0)
     * @return Noise value in range [-1, 1]
     */
    public double octaveNoise2D(double x, double y, int octaves, double persistence, double lacunarity) {
        double total = 0.0;
        double amplitude = 1.0;
        double frequency = 1.0;
        double maxValue = 0.0;
        
        for (int i = 0; i < octaves; i++) {
            total += noise2D(x * frequency, y * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= lacunarity;
        }
        
        return total / maxValue;
    }
    
    /**
     * Generates multi-octave 3D noise.
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param octaves Number of noise layers
     * @param persistence Amplitude multiplier per octave
     * @param lacunarity Frequency multiplier per octave
     * @return Noise value in range [-1, 1]
     */
    public double octaveNoise3D(double x, double y, double z, int octaves, double persistence, double lacunarity) {
        double total = 0.0;
        double amplitude = 1.0;
        double frequency = 1.0;
        double maxValue = 0.0;
        
        for (int i = 0; i < octaves; i++) {
            total += noise3D(x * frequency, y * frequency, z * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= lacunarity;
        }
        
        return total / maxValue;
    }
    
    /**
     * Fast floor function.
     * 
     * @param x Value to floor
     * @return Floored integer
     */
    private int fastFloor(double x) {
        int xi = (int) x;
        return x < xi ? xi - 1 : xi;
    }
    
    /**
     * 2D dot product.
     * 
     * @param g Gradient vector
     * @param x X coordinate
     * @param y Y coordinate
     * @return Dot product
     */
    private double dot(int[] g, double x, double y) {
        return g[0] * x + g[1] * y;
    }
    
    /**
     * 3D dot product.
     * 
     * @param g Gradient vector
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return Dot product
     */
    private double dot(int[] g, double x, double y, double z) {
        return g[0] * x + g[1] * y + g[2] * z;
    }
}
