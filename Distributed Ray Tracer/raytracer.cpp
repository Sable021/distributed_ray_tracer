#include <stdlib.h>
#include <GL/glut.h>
#include <math.h>
#include <time.h>
#include <iostream>

using namespace std;

#include "utility.h"
#include "intersect.h"
#include "sampling.h"
#include "RayTracer.h"

/********** Global Variables **********/

#define MAX_DEPTH 6		//max ray tracing level
#define LARGE_FLOAT 99999999
#define GRID_X 8	//Not more than 8
#define GRID_Y 8	//Not more than 8
int grid_size  = GRID_X*GRID_Y;

//Depth of Field grid size
double DOF_lensHeight = 0.4;
double DOF_lensWidth = 0.4;


double eye[3] = {-0.3, 3.0, 11.0};	//eye position

// screen plane in world space coordinates
double scr_WXL = -3.0, scr_WXR = 3.0, scr_HYB = 1.25, scr_HYT = 5.75;

//deltas for screen interpolation
double scr_DX = (scr_WXR - scr_WXL)/scrWidth;
double scr_DY = (scr_HYT - scr_HYB)/scrHeight;

double global_amb[3] = {0.05, 0.05, 0.05};	//Global ambient colour, used for each ray

int indicator = 0;


/********** Functions Declarations **********/
void set_black(double colour[3]);
void set_light(double colour[3]);
int intersect_scene(RayObj &ray, double intersect[3], int depth);
bool intersect_object(RayObj &ray, int index, double &t);
void shade_object(int index, double intersect[3], double V[3], double colour[3], int ray_num);


/********** Functions Definitions **********/

bool render_rays()
{	
	double scr_X, scr_Y;	//Current screen world coordinates
	double acc_colour[3], curr_colour[3], curr_vertex[3], curr_sub_vertex[3];	//Keep current_colour, vertex of point on screen
	RayObj curr_ray;	//Current primary ray
	int i, j;	//Pixel coordinates

	time_t start = time(NULL);
	time_t diff, remain;

	//Initialise sampling grid for area light sources
	srand((unsigned)time(NULL));
	for(i=0; i<num_objects; i++)
	{
		if(objects[i].isLight == true && objects[i].type == PLANE)
			create_light_grid(GRID_X, GRID_Y, i);
	}

	/*The below section will perform the actual ray tracing, either normally
	or using the depth of field method. Only one portion can be uncommented at any
	one time */

	//////////Normal Ray Tracing Section//////////

	//Begin pixel by pixel ray tracing
	for(i=0; i<scrHeight; i++)
	{
		for(j=0; j<scrWidth; j++)
		{
			scr_X = scr_WXL + j*scr_DX;
			scr_Y = scr_HYB + i*scr_DY;

			//Debug Line
			//printf("Doing pixel location (%d, %d)\n", j, i);

			//Statements to update the output screen
			if(i >= 3*scrHeight/4 && j >= 3*scrWidth/4 && indicator == 2)
			{
				cout<<"(75% completed) Picture is finishing! Just a little longer... "<<endl;
				diff = time(NULL) - start;
				remain = diff / 3;
				cout<<"Time elasped: "<<diff<<" seconds.  Estimated time left: "<<remain<<" seconds."<<endl<<endl;
				indicator++;
			}
			else if(i >= scrHeight/2 && j >= scrWidth/2 && indicator == 1)
			{
				cout<<"(50% completed) Halfway there!"<<endl;
				diff = time(NULL) - start;
				cout<<"Time elasped: "<<diff<<" seconds.  Estimated time left: "<<diff<<" seconds."<<endl<<endl;
				indicator++;
			}
			else if(i >= scrHeight/4 && j >= scrWidth/4 && indicator == 0)
			{
				cout<<"(25% completed) Patience!"<<endl;
				diff = time(NULL) - start;
				remain = diff * 3;
				cout<<"Time elasped: "<<diff<<" seconds.  Estimated time left: "<<remain<<" seconds."<<endl<<endl;
				indicator++;
			}

			set_black(acc_colour);
			set_vector(curr_vertex, scr_X, scr_Y, 8.2);

			//////////Below is section for multiples ray per pixel//////////

			//Change here to fire 2x2 or 4x4 rays into the pixel
			for(int k=0; k<GRID_Y; k++)
			{
				for(int l =0; l<GRID_X; l++)
				{
					set_vector(curr_sub_vertex, 0.0, 0.0, 8.2);
					set_black(curr_colour);

					curr_sub_vertex[0] = curr_vertex[0] + (scr_DX/GRID_X)*l + random(0, ((double)scr_DX)/(double)GRID_X);
					curr_sub_vertex[1] = curr_vertex[1] + (scr_DY/GRID_Y)*k + random(0, ((double)scr_DY)/(double)GRID_Y);
					
					cal_direction(curr_ray.ray_direct, eye, curr_sub_vertex);
					normalize_vector(curr_ray.ray_direct);

					set_ray(curr_ray, eye, curr_ray.ray_direct);

					ray_trace(curr_ray, 1, 1.0, curr_colour, false, k*GRID_X+l);

					acc_colour[0] += curr_colour[0];
					acc_colour[1] += curr_colour[1];
					acc_colour[2] += curr_colour[2];
				}
			}

			//Average the acculmulated colours
			acc_colour[0] /= (double) grid_size;
			acc_colour[1] /= (double) grid_size;
			acc_colour[2] /= (double) grid_size;

			//////////End of multiple rays per pixel section//////////
			

			/*//////////Below is section for one ray per pixel//////////

			//Set Primary Ray
			set_black(acc_colour);	//Reset to black
			cal_direction(curr_ray.ray_direct, eye, curr_vertex);
			normalize_vector(curr_ray.ray_direct);

			set_ray(curr_ray, eye, curr_ray.ray_direct);


			//Debug Line
			//print_vector("ray origin: ", curr_ray.ray_point);
			//print_vector("ray direction: ", curr_ray.ray_direct);

			//Fire primary ray to pixel
			ray_trace(curr_ray, 1, 1.0, acc_colour, false, 0);

			//////////End of one ray per pixel section//////////*/

			//Clamp colour values to maximum of 1.0
			if(acc_colour[0] > 1.0){
				acc_colour[0] = 1.0;}
			if(acc_colour[1] > 1.0){
				acc_colour[1] = 1.0;}
			if(acc_colour[2] > 1.0){
				acc_colour[2] = 1.0;}
			
			//Set pixel in pixel_buffer
			pixel_buffer[i][j][0] = (GLubyte) (acc_colour[0] * 255);	//Red
			pixel_buffer[i][j][1] = (GLubyte) (acc_colour[1] * 255);	//Green
			pixel_buffer[i][j][2] = (GLubyte) (acc_colour[2] * 255);	//Blue
		}
	}

	//////////End of Normal Ray Tracing Section//////////


	
	/*//////////Depth of Field Ray Tracing Section//////////

	//Define Depth of Field focus plane
	Object DOF_plane;
	RayObj DOF_ray;
	double t, curr_origin[3];
	
	double DOF_DX = DOF_lensWidth/GRID_X;
	double DOF_DY = DOF_lensHeight/GRID_Y;

	DOF_plane.type = PLANE;
	set_vector(DOF_plane.vectors[0], 0.0, 0.0, -1.0);
	DOF_plane.dist = 3.6;	//Focus at refractive sphere

	for(i=0; i<scrHeight; i++)
	{
		for(j=0; j<scrWidth; j++)
		{
			scr_X = scr_WXL + j*scr_DX;
			scr_Y = scr_HYB + i*scr_DY;

			//Debug Line
			//printf("Doing pixel location (%d, %d)\n", j, i);

			//Statements to update the output screen
			if(i >= 3*scrHeight/4 && j >= 3*scrWidth/4 && indicator == 2)
			{
				cout<<"(75% completed) Picture is finishing! Just a little longer... "<<endl;
				diff = time(NULL) - start;
				remain = diff / 3;
				cout<<"Time elasped: "<<diff<<" seconds.  Estimated time left: "<<remain<<" seconds."<<endl<<endl;
				indicator++;
			}
			else if(i >= scrHeight/2 && j >= scrWidth/2 && indicator == 1)
			{
				cout<<"(50% completed) Halfway there!"<<endl;
				diff = time(NULL) - start;
				cout<<"Time elasped: "<<diff<<" seconds.  Estimated time left: "<<diff<<" seconds."<<endl<<endl;
				indicator++;
			}
			else if(i >= scrHeight/4 && j >= scrWidth/4 && indicator == 0)
			{
				cout<<"(25% completed) Patience!"<<endl;
				diff = time(NULL) - start;
				remain = diff * 3;
				cout<<"Time elasped: "<<diff<<" seconds.  Estimated time left: "<<remain<<" seconds."<<endl<<endl;
				indicator++;
			}

			set_black(acc_colour);
			set_vector(curr_vertex, scr_X, scr_Y, 8.2);

			//Set initial ray to determine focus point
			cal_direction(DOF_ray.ray_direct, eye, curr_vertex);
			normalize_vector(DOF_ray.ray_direct);
			set_ray(DOF_ray, curr_vertex, DOF_ray.ray_direct);

			t = ray_plane_intersect(DOF_ray, DOF_plane);

			if(t <= 0.0)
			{
				cout<<"Error! Eye ray does not intersect with focal plane!"<<endl;
				exit(1);
			}

			//Set current vertex as focus point
			cal_point_on_line(curr_vertex, DOF_ray.ray_point, DOF_ray.ray_direct, t);

			//Determine Len's bottom left vertex, this is used as starting point to determine ray origin on lens grid
			curr_origin[0] = scr_X - (DOF_lensWidth/2.0);
			curr_origin[1] = scr_Y - (DOF_lensHeight/2.0);
			curr_origin[2] = 8.2;

			//Fire rays from lens area towards focus point
			for(int k=0; k<GRID_Y; k++)
			{
				for(int l=0; l<GRID_X; l++)
				{
					set_vector(curr_sub_vertex, 0.0, 0.0, 8.2);
					set_black(curr_colour);

					curr_sub_vertex[0] = curr_origin[0] + DOF_DX*l + random(0.0, DOF_DX);
					curr_sub_vertex[1] = curr_origin[1] + DOF_DY*k + random(0.0, DOF_DY);

					cal_direction(curr_ray.ray_direct, curr_sub_vertex, curr_vertex);
					normalize_vector(curr_ray.ray_direct);
					
					set_ray(curr_ray, curr_sub_vertex, curr_ray.ray_direct);

					//Fire ray
					ray_trace(curr_ray, 1, 1.0, curr_colour, false, k*GRID_X+l);

					acc_colour[0] += curr_colour[0];
					acc_colour[1] += curr_colour[1];
					acc_colour[2] += curr_colour[2];
				}
			}

			//Average the acculmulated colours
			acc_colour[0] /= (double) grid_size;
			acc_colour[1] /= (double) grid_size;
			acc_colour[2] /= (double) grid_size;

			//Clamp colour values to maximum of 1.0
			if(acc_colour[0] > 1.0){
				acc_colour[0] = 1.0;}
			if(acc_colour[1] > 1.0){
				acc_colour[1] = 1.0;}
			if(acc_colour[2] > 1.0){
				acc_colour[2] = 1.0;}
			
			//Set pixel in pixel_buffer
			pixel_buffer[i][j][0] = (GLubyte) (acc_colour[0] * 255);	//Red
			pixel_buffer[i][j][1] = (GLubyte) (acc_colour[1] * 255);	//Green
			pixel_buffer[i][j][2] = (GLubyte) (acc_colour[2] * 255);	//Blue
			
		}
	}

	//////////End of Depth of Field Ray Tracing Section//////////*/
	
	

	return true;
}

void ray_trace(RayObj &ray, int depth, double rindex, double colour[3], bool inside, int ray_num)
{
	//Check Max Depth
	//Intersect scene
	//Return if no intersect
	//return if hit light
	//do and store local shading
	//if refr > 0.0 do refraction calculation
		//If inside and angle > critical angle, do total internal reflection
	//if refl > 0.0 and not inside ray, do reflection calculation

	//Combine and store all 3 colours 

	double intersect[3];	//Intersection coordinates of the ray
	double local_colour[3], reflect_colour[3], refract_colour[3], N[3], V[3];	//N is normal at intersection, V is veiwing angle
	double refl_direct[3], refr_direct[3];
	int object_index;
	RayObj reflect_ray, refract_ray;
	double object_rindex;

	if(depth > MAX_DEPTH)
	{
		set_black(colour);
	}
	else
	{
		object_index = intersect_scene(ray, intersect, depth);
		//if(inside == true){
			//cout<<"object index is "<<object_index<<endl;
			//print_vector("Intersection Point: ", intersect);
		//}

		if(object_index == -1)
		{
			set_black(colour);
		}

		else if(objects[object_index].isLight == true)
		{
			set_light(colour);
		}

		//Testing Section
		else if(object_index == 15 || object_index == 16)
			set_vector(colour, 1.0, 0.0, 1.0);

		else{	//Ray intersected with object. Calculate shading and spawn new rays

			//Initialise required vectors
			set_vector(local_colour, 0.0, 0.0, 0.0);
			set_vector(reflect_colour, 0.0, 0.0, 0.0);
			set_vector(refract_colour, 0.0, 0.0, 0.0);
			set_vector(refl_direct, 0.0, 0.0, 0.0);
			set_vector(refr_direct, 0.0, 0.0, 0.0);
			set_vector(V, -ray.ray_direct[0], -ray.ray_direct[1], -ray.ray_direct[2]);

			//do local shading here

			shade_object(object_index, intersect, V, local_colour, ray_num);


			/////Reflection and Refraction Section/////
			//Check Refraction condition
			if(objects[object_index].refr > 0.0 && depth != MAX_DEPTH)
			{
				object_rindex = objects[object_index].rindex;
				get_normal(objects[object_index], intersect, N);

				//Check if ray is inside an object through toggle
				if(inside == true)
				{
				
					N[0] = -N[0];
					N[1] = -N[1];
					N[2] = -N[2];

					//Swap reflective indexes
					object_rindex = 1.0;	//Ray only returns to outside
				}

				if(cal_refraction(ray.ray_direct, N, rindex, object_rindex, refr_direct) == true)
				{
					//Set up refracted ray
					set_ray(refract_ray, intersect, refr_direct);

					//Spawn refracted ray with toggled inside value
					ray_trace(refract_ray, depth+1, object_rindex, refract_colour, !inside, ray_num);
				}
				
				else{	//Total Internal Reflection, using refract ray

					cal_reflection(ray.ray_direct, N, refr_direct);
					
					//Set up internal reflected ray
					set_ray(refract_ray, intersect, refr_direct);

					//Spawn internal reflected ray, do not toggle inside value
					ray_trace(refract_ray, depth+1, rindex, refract_colour, !inside, ray_num);
				}

				
			}

			
			//Calculate and spawn reflection rays
			if(objects[object_index].refl > 0.0 && inside == false && depth != MAX_DEPTH)	//Only calculate for rays not inside any object
			{
				get_normal(objects[object_index], intersect, N);
				cal_reflection(ray.ray_direct, N, refl_direct);
				normalize_vector(refl_direct);

				if(objects[object_index].glossiness <= 0.0 ||depth > 4)	//Do perfect reflection
				{	
					//Set up reflected ray
					set_ray(reflect_ray, intersect, refl_direct);

					//Spawn Reflected Ray
					ray_trace(reflect_ray, depth+1, rindex, reflect_colour, inside, ray_num);
				}
				else{	//glossiness != 0.0 && depth <= 4

					//Sample variables for used for glossy reflections
					double grid_midpt[3], sample_refl[3], sample_vertex[3];

					//glossiness determines how far the sample grid is from the intersect point, the more glossy, the closer it is
					cal_point_on_line(grid_midpt, intersect, 
						refl_direct, objects[object_index].glossiness); 

					get_sample_vertex(sample_vertex, GRID_X, GRID_Y, refl_direct, grid_midpt, ray_num);

					cal_direction(sample_refl, intersect, sample_vertex);
					normalize_vector(sample_refl);

					set_ray(reflect_ray, intersect, sample_refl);
					
					//Fire ray in selected sample reflection direction
					ray_trace(reflect_ray, depth+1, rindex, reflect_colour, inside, ray_num);
				}
			}

			/////End of Reflection and Refraction Section/////

			//Combine all colours

			colour[0] = local_colour[0] + objects[object_index].refl*reflect_colour[0] + objects[object_index].refr*refract_colour[0];
			colour[1] = local_colour[1] + objects[object_index].refl*reflect_colour[1] + objects[object_index].refr*refract_colour[1];
			colour[2] = local_colour[2] + objects[object_index].refl*reflect_colour[2] + objects[object_index].refr*refract_colour[2];
		}
	}

	
}

/********** Local functions definitions **********/

void set_black(double colour[3])
{
	set_vector(colour, 0.0, 0.0, 0.0);
}

void set_light(double colour[3])
{
	//Set the colour of the light, change if desired
	set_vector(colour, 1.0, 1.0, 1.0);	//White
}

int intersect_scene(RayObj &ray, double intersect[3], int depth)
{
	//Returns the nearest object index number and stores the intersection
	//Searches the objects[] array created in objects.cpp
	//Returns -1 if no intersection found

	int i, object_index;
	double nearest_t, t;

	//Initialise variables
	object_index = -1;
	nearest_t = LARGE_FLOAT;
	
	//Search all objects
	for(i=0; i<num_objects; i++)
	{
		if(objects[i].type == UNASSIGNED)
			continue;

		else if(depth == 1 && i == 16)	//This specialised line is to prevent the front area light
			continue;				//from covering the screen
		else
		{
			if(intersect_object(ray, i, t) == true)
			{
				if(t < nearest_t)
				{
					cal_point_on_line(intersect, ray.ray_point, ray.ray_direct, t);
					object_index = i;
					nearest_t = t;
				}
			}
		}
		
	}

	return object_index;
}

bool intersect_object(RayObj &ray, int index, double &t)
{
	//This function will determine if the ray intersects with the required object
	//Returns true if it does

	if(objects[index].type == UNASSIGNED){
		//cout<<"error in intersect_object: object "<<object_index<<" is unassigned."<<endl;
		return false;
	}

	if(objects[index].type == SPHERE)
	{
		t = ray_sphere_intersect(ray, objects[index]);
	}
	else if(objects[index].type == PLANE)
	{
		t = ray_plane_intersect(ray, objects[index]);

		/////Testing Section Below Determines intersection for plane quadric/////

		//ONLY WORKS FOR AXIS-ALIGNED QUARDICS!!!

		if(t != -1.0 && (index == 15 || index == 16))
		{
			double boundaries[4], test_pt[2], intersect[3];

			cal_point_on_line(intersect, ray.ray_point, ray.ray_direct, t);

			int j=0, i;
			for(i=0; i<3; i++)
			{
				if(objects[index].vectors[0][i] == 0.0)
				{
					test_pt[j] = intersect[i];

					//Store boundary values
					if(objects[index].vectors[5][i] > objects[index].vectors[3][i])
					{
						boundaries[2*j] = objects[index].vectors[3][i];
						boundaries[(2*j)+1] = objects[index].vectors[5][i];
					}
					else{
						boundaries[2*j] = objects[index].vectors[5][i];
						boundaries[(2*j)+1] = objects[index].vectors[3][i];
					}

					j++;
				}
			}

			//Check if intersect point is within boundaries
			for(j=0; j<2; j++)
			{
				if(test_pt[j] < boundaries[2*j] || test_pt[j] > boundaries[(2*j)+1]){
					t = -1.0;
					break;
				}
			}
		}
	}

	else if(objects[index].type == TRIANGLE)
	{
		t = ray_tri_intersect(ray, objects[index]);	
	}

	if(t == -1.0)
		return false;
	else
		return true;
}

void shade_object(int index, double intersect[3], double V[3], double colour[3], int ray_num)
{
	//This function will do the shading for the object at the intersect point with all light sources
	//Stores final colour in colour

	int i, j, k;
	double N[3], L[3], R[3], diff_colour[3], spec_colour[3], object_colour[3];
	double NdotL, VdotR, shadow, kd, ks, t;
	RayObj shadow_ray;	//Shadow feeler

	bool spec_r_or_t = false;	//Toggle for relfected or transmitted light from light source	

	//Determine object colour at intersect point
	get_object_colour(index, intersect, object_colour);

	for(i=0; i<num_objects; i++)
	{
		if(objects[i].isLight == true)
		{
			//Initialise Colour vectors
			set_vector(diff_colour, 0.0, 0.0, 0.0);
			set_vector(spec_colour, 0.0, 0.0, 0.0);

			/////Point Light Source/////

			if(objects[i].type == SPHERE)
			{
				/////Do Shadow Feelers/////
				cal_direction(L, intersect, objects[i].vectors[0]);
				normalize_vector(L);

				set_ray(shadow_ray, intersect, L);
				
				shadow = 1.0;
				for(j=0; j<15; j++)
				{
					if(objects[j].type == PLANE)	//Shadow Rays should not cut the walls
						continue;
					
					if((objects[j].isLight == false) && 
						(intersect_object(shadow_ray, j, t) == true))
					{
						if(objects[j].refr > 0.0)
						{
							shadow *= 0.6;	//Translucent Object will only attenuate the shadow term	
							continue;	//Continue checking for opaque obstruction
						}
						else{
							shadow = 0.0;
							break;
						}
					}
				}
			
				if(shadow > 0.0)
				{
					//Calculate N for intersect point
					get_normal(objects[index], intersect, N);

					NdotL = cal_dot_product(N, L);
	
					if(NdotL> 0.0) //Point is facing the light
					{
						/////Do Diffuse Shading/////

						kd = objects[index].diffuse;	//diffuse component
					
						diff_colour[0] = shadow*kd*NdotL*object_colour[0]*objects[i].vectors[1][0];
						diff_colour[1] = shadow*kd*NdotL*object_colour[1]*objects[i].vectors[1][1];
						diff_colour[2] = shadow*kd*NdotL*object_colour[2]*objects[i].vectors[1][2];
	
						/////Do Specular Shading/////

						if(objects[index].specular_r > 0.0)
						{
							//Set R
							R[0] = 2*NdotL*N[0] - L[0];
							R[1] = 2*NdotL*N[1] - L[1];
							R[2] = 2*NdotL*N[2] - L[2];
							normalize_vector(R);
						
							VdotR= cal_dot_product(V, R);

							ks = objects[index].specular_r;

							spec_colour[0] = shadow*ks*powf(VdotR, objects[index].n)*objects[i].vectors[2][0];
							spec_colour[1] = shadow*ks*powf(VdotR, objects[index].n)*objects[i].vectors[2][1];
							spec_colour[2] = shadow*ks*powf(VdotR, objects[index].n)*objects[i].vectors[2][2];
						}
					}
				}

				/////Combine Shading and add to colour/////
				colour[0] += diff_colour[0] + spec_colour[0];
				colour[1] += diff_colour[1] + spec_colour[1];
				colour[2] += diff_colour[2] + spec_colour[2];

			}

			/////Area Light Source/////
			//For area light souce, we model the area light as multiple point light sources and 
			//determine the fraction of light seen at the intersect point
			//L vector is directed towards the centre of the light source
			
			else if(objects[i].type == PLANE)
			{
				double sample_vertex[3], sample_colour[3];
				double light_grid_DX[3], light_grid_DY[3], light_DX, light_DY;
				double randx, randy;
				int x, y;

				get_light_grid_delta(i, light_DX, light_DY, light_grid_DX, light_grid_DY);
				set_vector(sample_colour, 0.0, 0.0, 0.0);
				
				for(k=0; k<4; k++)	//Do 4 colour calculation for each ray, each at one quadrant of the light source
				{
					/////Do Shadow Feelers/////
					shadow = 1.0;

					get_grid_number((ray_num+k)%grid_size, GRID_X, GRID_Y, x, y);
				
					randx = random(0.0, 1.0);
					randy = random(0.0, 1.0);

					//Set the sample point in the light source we are testing the shadow ray with
					sample_vertex[0] = objects[i].light_grid_sample[y][x][0] + randx*light_grid_DX[0] + randy*light_grid_DY[0];
					sample_vertex[1] = objects[i].light_grid_sample[y][x][1] + randx*light_grid_DX[1] + randy*light_grid_DY[1];
					sample_vertex[2] = objects[i].light_grid_sample[y][x][2] + randx*light_grid_DX[2] + randy*light_grid_DY[2];

					cal_direction(L, intersect, sample_vertex);
					normalize_vector(L);
					set_ray(shadow_ray, intersect, L);

					for(j=0; j<num_objects; j++)
					{
						if(objects[j].type == PLANE || objects[j].isLight == true)	//Shadow Rays should not cut the walls
							continue;

						if((objects[j].isLight == false) && (intersect_object(shadow_ray, j, t) == true))
						{
							if(objects[j].refr > 0.0)	
							{
								shadow *= 0.6;	// Refractive surfaces cover less light, continue testing for opaque objects
								continue;
							}
							else{
								shadow = 0.0;
								break;	//Already intersected with opaque object, no need to test further
							}
						}
					}

					if(shadow > 0.0)
					{
						//Calculate N for intersect point
						get_normal(objects[index], intersect, N);
	
						NdotL = cal_dot_product(N, L);

						if(NdotL > 0.0)	//Point is facing the light
						{
							/////Do Diffuse Shading/////
	
							kd = objects[index].diffuse;	//diffuse component
	
							diff_colour[0] = shadow*kd*NdotL*object_colour[0]*objects[i].vectors[1][0];
							diff_colour[1] = shadow*kd*NdotL*object_colour[1]*objects[i].vectors[1][1];
							diff_colour[2] = shadow*kd*NdotL*object_colour[2]*objects[i].vectors[1][2];

							/////Do Specular Shading/////

							if(objects[index].specular_r > 0.0)
							{
								//Set R
								R[0] = 2*NdotL*N[0] - L[0];
								R[1] = 2*NdotL*N[1] - L[1];
								R[2] = 2*NdotL*N[2] - L[2];
								normalize_vector(R);
						
								VdotR= cal_dot_product(V, R);
	
								ks = objects[index].specular_r;
	
								spec_colour[0] = shadow*ks*powf(VdotR, objects[index].n)*objects[i].vectors[2][0];
								spec_colour[1] = shadow*ks*powf(VdotR, objects[index].n)*objects[i].vectors[2][1];
								spec_colour[2] = shadow*ks*powf(VdotR, objects[index].n)*objects[i].vectors[2][2];
							}
						}
					}

					sample_colour[0] += diff_colour[0] + spec_colour[0];
					sample_colour[1] += diff_colour[1] + spec_colour[1];
					sample_colour[2] += diff_colour[2] + spec_colour[2];
					
				}
				/////Combine Shading and add to colour/////
				colour[0] += sample_colour[0] / 4.0;
				colour[1] += sample_colour[1] / 4.0;
				colour[2] += sample_colour[2] / 4.0;
				
			}
		}
	}

	//Add in the global ambient component 
	colour[0] += global_amb[0];
	colour[1] += global_amb[1];
	colour[2] += global_amb[2];

	//Debug Line
	//print_vector("Final Colour: ", colour);
}
