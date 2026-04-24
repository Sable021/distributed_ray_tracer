#include <math.h>
#include <iostream>

using namespace std;

#include "utility.h"
#include "objects.h"
#include "sampling.h"

#define RANDMAX 10000

/////Functions Declarations/////



///// Functions Definitions ////
void create_light_grid(int gsize_x, int gsize_y, int index)
{
	double direct_x[3], direct_y[3];	//Grid Axis direction
	double grid_DX[3], grid_DY[3], grid_origin[3];	//Grid parameters, use multiples of DX or DY to determine vertex on grid
	double length_x, length_y;	
	
	//Makes the sample grid given grid size x, y

	if(objects[index].isLight)
	{
		cal_direction(direct_x, objects[index].vectors[3], objects[index].vectors[4]);
		cal_direction(direct_y, objects[index].vectors[3], objects[index].vectors[6]);

		length_x = cal_magnitude(direct_x);
		length_y = cal_magnitude(direct_y);

		normalize_vector(direct_x);
		normalize_vector(direct_y);

		//Set parameters to calculate points on the grid

		copy_vertex(objects[index].vectors[3], grid_origin);

		grid_DX[0] = (length_x/gsize_x)*direct_x[0];
		grid_DX[1] = (length_x/gsize_x)*direct_x[1];
		grid_DX[2] = (length_x/gsize_x)*direct_x[2];

		grid_DY[0] = (length_y/gsize_y)*direct_y[0];
		grid_DY[1] = (length_y/gsize_y)*direct_y[1];
		grid_DY[2] = (length_y/gsize_y)*direct_y[2];

		//Copy Vertex into object storage
		copy_vertex(grid_DX, objects[index].light_grid_DX);
		copy_vertex(grid_DY, objects[index].light_grid_DY);
		objects[index].light_DX = length_x/gsize_x;
		objects[index].light_DY = length_y/gsize_y;

		for(int i=0; i<gsize_y; i++)
		{
			for(int j=0; j<gsize_x; j++)
			{
				
				objects[index].light_grid_sample[i][j][0] = grid_origin[0] + j*grid_DX[0] + i*grid_DY[0];

				objects[index].light_grid_sample[i][j][1] = grid_origin[1] + j*grid_DX[1] + i*grid_DY[1];

				objects[index].light_grid_sample[i][j][2] = grid_origin[2] + j*grid_DX[2] + i*grid_DY[2];
			}
		}

		
	}
	else
	{
		cout<<"Error in create_light_grid. Object is not a light."<<endl;
	}
}

void get_light_grid_delta(int index, double &light_DX, double &light_DY, double light_grid_DX[3], double light_grid_DY[3])
{
	//Returns values of the light grid for use in stratified sampling
	light_DX = objects[index].light_DX;
	light_DY = objects[index].light_DY;

	copy_vertex(objects[index].light_grid_DX, light_grid_DX);
	copy_vertex(objects[index].light_grid_DY, light_grid_DY);
}

double random(double min, double max)
{
	//Creates a random number between min and max


	double R = ((double) rand()/(double) RAND_MAX);

	return (R*(max-min) + min);
}

void create_sample_grid(double sample_grid[MAX_GRID_X][MAX_GRID_Y][3],  int gsize_x, int gsize_y, double N[3], double midpt[3])
{
	//Creats the sample grid used for reflection or refraction. All sample grids are squares of side 0.5,
	//giving a diagonal distance of sqrt(0.125)
	
	double dia1[3], dia2[3]; //Vectors for diagonal
	double gridx[3], gridy[3];	//Vectors for the grid axes
	double grid_origin[3];	//Starting point to calculate the grid points
	double DX = 0.5/gsize_x, DY = 0.5/gsize_y;
	double dia_dist = 0.35355339;

	//Define Starting vector
	double V[3] = {0.5, 0.5, 0.5};

	double randx, randy;

	cal_cross_product(dia1, V, N);
	cal_cross_product(dia2, dia1, N);

	//Calculate the starting point for the grid
	cal_point_on_line(grid_origin, midpt, dia1, dia_dist);

	//Calculate the grid axes
	cal_direction(gridx, dia1, dia2);
	normalize_vector(gridx);

	cal_cross_product(gridy, gridx, N);

	//Set the proper magnitudes for gridx and gridy, setting length of grid to 0.5
	gridx[0] *= DX;
	gridx[1] *= DX;
	gridx[2] *= DX;

	gridy[0] *= DY;
	gridy[1] *= DY;
	gridy[2] *= DY;

	//Calculate and store points on the grid

	for(int i=0; i<gsize_y; i++)
	{
		for(int j=0; j<gsize_x; j++)
		{
			randx = random(0.0, DX);
			randy = random(0.0, DY);
			
			sample_grid[i][j][0] = grid_origin[0] + ((j+randx)*gridx[0]) + ((i+randy)*gridy[0]);
			sample_grid[i][j][1] = grid_origin[1] + ((j+randx)*gridx[1]) + ((i+randy)*gridy[1]);
			sample_grid[i][j][2] = grid_origin[2] + ((j+randx)*gridx[2]) + ((i+randy)*gridy[2]);
		}
	}
}

void get_sample_vertex(double sample_vertex[3], int gsize_x, int gsize_y, double N[3], double midpt[3],  int trace_num)
{
	//Creats the sample grid used for reflection or refraction. All sample grids are squares of side 0.5,
	//giving a diagonal distance of sqrt(0.125)
	//Sample vertex already includes the randomness required for stratified sampling
	
	double dia1[3], dia2[3]; //Vectors for diagonal
	double gridx[3], gridy[3];	//Vectors for the grid axes
	double grid_origin[3];	//Starting point to calculate the grid points
	double DX = 0.5/gsize_x, DY = 0.5/gsize_y;
	double dia_dist = 0.35355339;
	int x, y;

	//Define Starting vector
	double V[3] = {0.5, 0.5, 0.5};

	double randx, randy;

	cal_cross_product(dia1, V, N);
	cal_cross_product(dia2, dia1, N);

	//Calculate the starting point for the grid
	cal_point_on_line(grid_origin, midpt, dia1, dia_dist);

	//Calculate the grid axes
	cal_direction(gridx, dia1, dia2);
	normalize_vector(gridx);

	cal_cross_product(gridy, gridx, N);

	//Set the proper magnitudes for gridx and gridy, setting length of grid to 0.5
	gridx[0] *= DX;
	gridx[1] *= DX;
	gridx[2] *= DX;

	gridy[0] *= DY;
	gridy[1] *= DY;
	gridy[2] *= DY;

	randx = random(0.0, 1.0);
	randy = random(0.0, 1.0);

	get_grid_number(trace_num, gsize_x, gsize_y, x, y);

	sample_vertex[0] = grid_origin[0] + (x+randx)*gridx[0] + (y+randy)*gridy[0];
	sample_vertex[1] = grid_origin[1] + (x+randx)*gridx[1] + (y+randy)*gridy[1];
	sample_vertex[2] = grid_origin[2] + (x+randx)*gridx[2] + (y+randy)*gridy[2];
}

void get_grid_number(int trace_num, int gsize_x, int gsize_y, int &x, int &y)
{
	//This function will calculate and return the grid number of the axes that the
	//ray should fire through

	int grid_size = gsize_y * gsize_x;
	int grid_num = (trace_num*7) %grid_size;

	x = grid_num % gsize_x;
	y= grid_num / gsize_y;
}