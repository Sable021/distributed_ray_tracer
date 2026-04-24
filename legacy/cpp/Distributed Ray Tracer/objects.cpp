#include <stdlib.h>
#include <math.h>
#include <iostream>

using namespace std;

#include "utility.h"
#include "objects.h"
#include "textures.h"

//Declaring of extern variables
Object* objects;
int num_objects;

#define PI 3.141592654


/////Function Declarations/////
void initialise_object_array(int size);

/***** The below lines describe the scene. Change the below code to change the picture *****/
void initialise_scene()
{
	initialise_object_array(20);

	//Floor
	objects[0].type = PLANE;
	set_vector(objects[0].vectors[0], 0.0, 1.0, 0.0);	//Set Normal
	objects[0].dist = 0.0;
	set_vector(objects[0].colour, 0.4, 0.4, 0.4);
	objects[0].diffuse = 0.8;
	objects[0].refl = 0.3;

	//Left Wall
	objects[1].type = PLANE;
	set_vector(objects[1].vectors[0], 1.0, 0.0, 0.0);	//Set Normal
	objects[1].dist = 9.0;
	set_vector(objects[1].colour, 0.7, 0.7, 0.4);
	objects[1].diffuse = 0.5;

	//Right Wall
	objects[2].type = PLANE;
	set_vector(objects[2].vectors[0], -1.0, 0.0, 0.0);	//Set Normal
	objects[2].dist = 9.0;
	set_vector(objects[2].colour, 0.7, 0.7, 0.4);
	objects[2].diffuse = 0.5;

	//Back Wall
	objects[3].type = PLANE;
	set_vector(objects[3].vectors[0], 0.0, 0.0, 1.0);	//Set Normal
	objects[3].dist = 5.0;
	set_vector(objects[3].colour, 0.2, 0.6, 0.6);
	objects[3].diffuse = 0.75;
	objects[3].refl = 0.2;

	//Ceiling
	objects[4].type = PLANE;
	set_vector(objects[4].vectors[0], 0.0, -1.0, 0.0);	//Set Normal
	objects[4].dist = 10.0;
	set_vector(objects[4].colour, 0.2, 0.6, 0.6);
	objects[4].diffuse = 0.7;
	objects[4].specular_r = 0.4;
	objects[4].n = 20;
	objects[4].refl = 0.4;

	//Reflective Sphere
	objects[5].type = SPHERE;
	set_vector(objects[5].vectors[0], -3.0, 3.0, -1.5);
	objects[5].radius = 2.25;
	set_vector(objects[5].colour, 0.15, 0.15, 0.15);
	objects[5].diffuse = 1.0;
	objects[5].specular_r = 1.0;
	objects[5].n = 100;
	objects[5].glossiness = 5.0;
	objects[5].refl = 1.0;

	//Stacked Bottom Sphere
	objects[6].type = SPHERE;
	set_vector(objects[6].vectors[0], 4.2, 2.0, 1.0);
	objects[6].radius = 1.5;
	set_vector(objects[6].colour, 1.0, 0.0, 0.0);
	objects[6].diffuse = 0.8;
	objects[6].specular_r = 0.75;
	objects[6].n = 40;
	objects[6].glossiness = 0.8;
	objects[6].refl = 0.5;

	//Stacked Top Sphere
	objects[7].type = SPHERE;
	set_vector(objects[7].vectors[0], 4.0, 6.2, 1.0);
	objects[7].radius = 1.5;
	set_vector(objects[7].colour, 0.9, 0.85, 0.0);
	objects[7].diffuse = 0.8;
	objects[7].specular_r = 0.5;
	objects[7].n = 20;
	objects[7].glossiness = 2.1;
	objects[7].refl = 0.3;

	//Transulent Sphere
	objects[8].type = SPHERE;
	set_vector(objects[8].vectors[0], 0.3, 2.0, 3.8);
	objects[8].radius = 1.3;
	set_vector(objects[8].colour, 0.1, 0.1, 0.1);
	objects[8].rindex = 1.5;
	objects[8].diffuse = 1.0;
	objects[8].specular_r = 0.85;
	objects[8].specular_t = 0.8;
	objects[8].n = 60;
	//objects[8].glossiness = 0.9;
	objects[8].refl = 0.15;
	objects[8].refr = 0.95;

	//Tetrahedron Bottom Face
	objects[9].type = TRIANGLE;
	set_vector(objects[9].vectors[0], -6.3, 0.0, 2.5);
	set_vector(objects[9].vectors[1], -3.5, 0.0, 4.5);
	set_vector(objects[9].vectors[2], -3.0, 0.0, 1.5);
	set_vector(objects[9].vectors[3], 0.0, -1.0, 0.0);
	set_vector(objects[9].colour, 1.0, 1.0, 0.0);
	objects[9].diffuse = 1.0;

	//Tetrahedron Left Face
	objects[10].type = TRIANGLE;
	set_vector(objects[10].vectors[0], -6.3, 0.0, 2.5);
	set_vector(objects[10].vectors[1], -3.5, 0.0, 4.5);
	set_vector(objects[10].vectors[2], -3.8, 3.5, 3.0);
	set_vector(objects[10].vectors[3], -0.556759, 0.287154, 0.779463);
	set_vector(objects[10].colour, 0.0, 1.0, 0.0);
	objects[10].diffuse = 1.0;

	//Tetrahedron Right Face
	objects[11].type = TRIANGLE;
	set_vector(objects[11].vectors[0], -3.5, 0.0, 4.5);
	set_vector(objects[11].vectors[1], -3.0, 0.0, 1.5);
	set_vector(objects[11].vectors[2], -3.8, 3.5, 3.0);
	set_vector(objects[11].vectors[3], 0.984405, 0.063464, 0.164068);
	set_vector(objects[11].colour, 0.0, 0.0, 1.0);
	objects[11].diffuse = 1.0;

	//Tetrahedron Back Face
	objects[12].type = TRIANGLE;
	set_vector(objects[12].vectors[0], -3.0, 0.0, 1.5);
	set_vector(objects[12].vectors[1], -6.3, 0.0, 2.5);
	set_vector(objects[12].vectors[2], -3.8, 3.5, 3.0);
	set_vector(objects[12].vectors[3], -0.274163, 0.326011, -0.904738);
	set_vector(objects[12].colour, 1.0, 0.0, 0.0);
	objects[12].diffuse = 1.0;

	//The below section define all light sources, area or point

	/*//Light Source 1
	objects[15].type = SPHERE;
	set_vector(objects[15].vectors[0], 2.0, 10.0, 1.5);
	set_vector(objects[15].vectors[1], 1.0, 1.0, 1.0);
	set_vector(objects[15].vectors[2], 1.0, 1.0, 1.0);
	objects[15].radius = 0.1;
	objects[15].isLight = true;

	//Light Source 2
	objects[16].type = SPHERE;
	set_vector(objects[16].vectors[0], -2.0, 5.0, 6.0);
	set_vector(objects[16].vectors[1], 1.0, 1.0, 1.0);
	set_vector(objects[16].vectors[2], 1.0, 1.0, 1.0);
	objects[16].radius = 0.1;
	objects[16].isLight = true;*/


	
	//Area Light Source 1
	objects[15].type = PLANE;
	set_vector(objects[15].vectors[0], 0.0, -1.0, 0.0);
	set_vector(objects[15].vectors[1], 1.0, 1.0, 1.0);
	set_vector(objects[15].vectors[2], 1.0, 1.0, 1.0);
	set_vector(objects[15].vectors[3], 1.0, 9.99, 0.5);
	set_vector(objects[15].vectors[4], 3.0, 9.99, 0.5);
	set_vector(objects[15].vectors[5], 3.0, 9.99, 2.5);
	set_vector(objects[15].vectors[6], 1.0, 9.99, 2.5);
	objects[15].dist = 9.99;
	set_vector(objects[15].colour, 1.0, 1.0, 1.0);
	objects[15].isLight = true;

	//Area Light Source 2
	objects[16].type = PLANE;
	set_vector(objects[16].vectors[0], 0.0, 0.0, -1.0);
	set_vector(objects[16].vectors[1], 1.0, 1.0, 1.0);
	set_vector(objects[16].vectors[2], 1.0, 1.0, 1.0);
	set_vector(objects[16].vectors[3], -1.0, 4.0, 6.0);
	set_vector(objects[16].vectors[4], -3.0, 4.0, 6.0);
	set_vector(objects[16].vectors[5], -3.0, 6.0, 6.0);
	set_vector(objects[16].vectors[6], -1.0, 6.0, 6.0);
	objects[16].dist = 6.0;
	set_vector(objects[16].colour, 1.0, 1.0, 1.0);
	objects[16].isLight = true;

	//Define number of objects
	num_objects = 17;
}

void initialise_object_array(int size)
{
	//Creates the array, and sets everything to 0 or false
	objects = new Object[20];

	for(int i=0; i<size;i++)
	{
		objects[i].type = UNASSIGNED;
		for(int j=0; j<7; j++)
		{
			for(int k=0; k<3; k++)
			{
				objects[i].vectors[j][k] = 0.0;
				objects[i].colour[k] = 0.0;
			}
		}

		objects[i].dist = 0.0;
		objects[i].radius = 0.0;

		objects[i].refl = objects[i].refr = objects[i].rindex = 0.0;
		objects[i].glossiness = 0.0;
		objects[i].diffuse = objects[i].specular_r = objects[i].specular_t = 0.0;
		objects[i].n = 0;

		objects[i].isLight = false;
		for(int k=0; k<16; k++)
			for(int l=0; l<16; l++)
				for(int j=0; j<3; j++){
					objects[i].light_grid_sample[k][l][j] = 0.0;
					objects[i].light_grid_DX[j] = 0.0;
					objects[i].light_grid_DY[j] = 0.0;
				}

		objects[i].light_DX = objects[i].light_DY = 0.0;
	}
}

void set_ray(RayObj &ray, double point[3], double direct[3])
{
	//This function will initialise the given ray

	copy_vertex(point, ray.ray_point);
	copy_vertex(direct, ray.ray_direct);
}

void get_object_colour(int index, double intersect[3], double colour[3])
{
	//Returns the required colour of the object at the intersect point.
	//This function will provide the 3D texturing equations for required objects

	if(index == 0)	//Floor object, must apply texture equation
		mixChecks(intersect, colour);	
	else if(index >= 9 && index <= 12)	//Tetrahedron, must apply texture equation
		strips(intersect, colour);
	else	//Non texture mapped objects, just return default object colour
		copy_vertex(objects[index].colour, colour);
}
