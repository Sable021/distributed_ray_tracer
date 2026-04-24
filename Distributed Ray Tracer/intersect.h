#include "objects.h"

/*This files includes all functions required to do: 
	- intersection testing between a ray and an object 
	- reflection and refraction calculation
	- Phong model calculation of pixels		*/

//All epsilon tests are done within the intersect functions so there is no need to perform it any where else

#define EPSILON	0.0001//This defines the minimum threshold value to accept an intersection


double ray_sphere_intersect(RayObj &ray, Object &sphere);

double ray_tri_intersect(RayObj &ray, Object &triangle);

double ray_plane_intersect(RayObj &ray, Object &plane);

void get_normal(Object &object, double intersect[3], double normal[3]);

void cal_reflection(double incident[3], double normal[3], double reflection[3]);

bool cal_refraction(double incident[3], double normal[3], double indexi, double indexr, double refraction[3]);