package com.raytracer;

public class SceneObject {

    public enum ObjectType { UNASSIGNED, SPHERE, PLANE, TRIANGLE }

    public ObjectType type = ObjectType.UNASSIGNED;

    /**
     * vectors[0..6][3] usage:
     *   Plane:    [0] = normal
     *   Sphere:   [0] = centre
     *   Triangle: [0..2] = vertices (anticlockwise), [3] = normal
     *   Light:    [0] = centre/normal, [1] = diffuse, [2] = specular, [3..6] = corners (area light)
     */
    public double[][] vectors = new double[7][3];

    public double dist;    // plane only: distance to origin
    public double radius;  // sphere only

    // Material
    public double[] colour     = new double[3];
    public double refl, refr, rindex, glossiness, diffuse, specular_r, specular_t;
    public int n;  // shininess (Phong exponent)

    // Light source
    public boolean isLight;
    public double[][][] lightGridSample = new double[16][16][3];
    public double[]     lightGridDX     = new double[3];
    public double[]     lightGridDY     = new double[3];
    public double       lightDX, lightDY;
}
