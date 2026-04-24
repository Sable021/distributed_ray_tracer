/* This file contains all definitions of required object structures 
	within the scene */

struct RayObj{
	//This structure defines the ray equation used for all
	//ray-tracing arithmetic
	double ray_point[3];
	double ray_direct[3];	//unit vector
};

typedef enum t_object {UNASSIGNED, SPHERE, PLANE, TRIANGLE};

/*	The below Structure stores all required information for an object on screen.	*
*	The use of vectors[][] is as follows:												*
*			- Plane: vectors[0] is normal											*
*			- Sphere: vectors[0] is centre											*
*			- Triangles: vectors[0-2] store the 3 vertices in anti-clockwise		*
*				manner, vectors[3] store the normal of the plane					*
*			- Light Source: vectors[0] is centre or normal, vectors[1] is diffuse,	*
*				vectors[2] is specular, vectors[3 - 6] are the corners for area 	*
*				light sources														*
*	All other parameters are self-explanatory, unused parameters are set to 0		*
*	All normal vectors should be stored in their normalized form					*/

struct Object{
	t_object type;
	double vectors[7][3];
	double dist;	//Use for plane only, distance to origin
	double radius;	//Use for sphere only

	//Material Properties
	double colour[3];
	double refl, refr, rindex, glossiness, diffuse, specular_r, specular_t; 
	int n;	//Shinniness

	//Note: 2 specular term only one can be non zero, _r for reflected light source
	//_t for refracted light source

	//Light properties
	bool isLight;
	double light_grid_sample[16][16][3];	//[[y][x][3]used for storing of grid vertices for area light
	double light_grid_DX[3], light_grid_DY[3], light_DX, light_DY;
};

/***** The below lines describe the scene. Change the below code to change the picture *****/

//Objects array
extern Object* objects;
extern int num_objects;
extern double **light_sampling[3];	//Stores all vertices used for sampling of light source

/***** Scene Functions *****/

void initialise_scene();

void set_ray(RayObj &ray, double point[3], double direct[3]);

void get_object_colour(int index, double intersect[3], double colour[3]);