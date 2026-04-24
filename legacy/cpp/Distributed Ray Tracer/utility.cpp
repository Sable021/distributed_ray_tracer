#include <math.h>
#include <iostream>

using namespace std;

#include "utility.h"

/**********Static Functions Declaration**********/

/**********Functions Definitions**********/

/////Vector Calculation Functions/////
void cal_direction(double direction[3], double vertex1[3], double vertex2[3])
{
	//Direction of vector is FROM vector1 TO vector2
	//WILL NOT NORMALIZE VECTOR!!!

	direction[0] = vertex2[0] - vertex1[0];
	direction[1] = vertex2[1] - vertex1[1];
	direction[2] = vertex2[2] - vertex1[2];

	//Debugging printout
	//print_vector("Direction vector; ", direction);

	//normalize_vector(direction);
}

void cal_cross_product(double cross[3], double vector1[3], double vector2[3])
{
	//This function will calculate and normalise the cross product
	cross[0] = vector1[1]*vector2[2] - vector1[2]*vector2[1];
	cross[1] = vector1[2]*vector2[0] - vector1[0]*vector2[2];
	cross[2] = vector1[0]*vector2[1] - vector1[1]*vector2[0];

	//Debugging output
	//print_vector("Cross Product: ", cross);

	normalize_vector(cross);
}

double cal_dot_product(double vector1[3], double vector2[3])
{
	double dot_product = (vector1[0]*vector2[0])+(vector1[1]*vector2[1])+(vector1[2]*vector2[2]);

	//Debugging output
	//cout<<"Dot Product is: "<<dot_product<<endl;

	return(dot_product);
}

double cal_determinant_3x3(double matrix[3][3])
{
	//This function will calculate and return the determinant of the 3x3 matrix

	double det;

	det = (matrix[0][0] * matrix[1][1] * matrix[2][2]) + (matrix[0][1] * matrix[1][2] * matrix[2][0])
		+ (matrix[0][2] * matrix[1][0] * matrix[2][1]) - (matrix[0][2] * matrix[1][1] * matrix[2][0])
		- (matrix[0][0] * matrix[1][2] * matrix[2][1]) - (matrix[0][1] * matrix[1][0] * matrix[2][2]);

	//Debug line
	//cout<<"Determinant is: "<< det<<endl;

	return(det);
}

double cal_magnitude(double vector[3])
{
	return(sqrt((vector[0]*vector[0])+(vector[1]*vector[1])+(vector[2]*vector[2])));
}

void cal_point_on_line(double point[3], double origin[3], double direction[3], double t)
{
	//Calculates the point on the line given a line equation and the t value

	point[0] = origin[0] + t*direction[0];
	point[1] = origin[1] + t*direction[1];
	point[2] = origin[2] + t*direction[2];

	//Debug Line
	//print_vector("Point on line: ", point);
}

void normalize_vector(double vector[3])
{
	double mag = cal_magnitude(vector);

	if(mag < 0.999999999 || mag > 1.000000001){	//Not a unit vector
		vector[0] /= mag;
		vector[1] /= mag;
		vector[2] /= mag;
	}

	//Debugging printout
	//print_vector("Normalised Vector: ", vector);	
}

void copy_vertex(double copy[3], double vertex[3])
{
	//Copies the vertex from copy to vertex
	vertex[0] = copy[0];
	vertex[1] = copy[1];
	vertex[2] = copy[2];

	//Debugging printout
	//print_vector("Copied Vertex: ", copy);
}

void set_vector(double vector[3], double a, double b, double c)
{
	//Copies the given values into vector, not normalized!

	vector[0] = a;
	vector[1] = b;
	vector[2] = c;
}

/////Debugging Functions/////
void print_vector(char *string, double vector[3])
{
	cout<<string;
	for(int i=0; i<3; i++)
		cout<<" "<<vector[i];
	cout<<endl;
}