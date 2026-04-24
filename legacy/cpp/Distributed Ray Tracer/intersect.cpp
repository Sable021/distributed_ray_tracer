#include <math.h>
#include <iostream>

using namespace std;

#include "utility.h"
#include "intersect.h"

/********** Function Declarations **********/

double ray_sphere_intersect(RayObj &ray, Object &sphere)
{
	double a, b, c, det, t1, t2, t;
	double V[3];

	cal_direction(V, sphere.vectors[0], ray.ray_point);

	//Find coefficients for equation
	a = 1;	//Since ray_direct is always an unit vector
	b = 2*cal_dot_product(ray.ray_direct, V);
	c = cal_dot_product(V, V) - (2*sphere.radius*sphere.radius);

	det = (b*b) - 4*a* c;

	//Check for real roots
	if(det < 0.0){	//Complex roots
		return (-1.0);
	}

	else if(det == 0.0){
		t = (-b) / 2*a;

		if(t < EPSILON)
			return (-1.0);
	}

	else{	//det > 0.0

		t1 = ((-b) - sqrt(det)) / (2*a);
		t2 = ((-b) + sqrt(det)) / (2*a);

		//Testing conditions with EPSILON test
		//Change if required!!!

		if(t2 < EPSILON)	//Both negative, intersection is behind ray
			return (-1.0);
		else if(t1 < EPSILON)	//Ray inside object, intersecting at t1
		{
			t = t2;
		}
		else		//both t1 and t2 are larger than EPSILON but t1 is nearer the ray origin
			t = t1;		
	}

	return t;
}

double ray_tri_intersect(RayObj &ray, Object &triangle)
{
	/* Works out the intersection using Cramer's Rule and barycentric coordinates */

	double matrix_A[3][3], matrix_beta[3][3], matrix_gamma[3][3], matrix_t[3][3];
	double det_A, beta, gamma, t;
	int i;

	//Save the original matrix used as denominator for Cramer's Rule
	for(i=0; i<3; i++)
		matrix_A[i][0] = (triangle.vectors[0][i] - triangle.vectors[1][i]);
	for(i=0; i<3; i++)
		matrix_A[i][1] = (triangle.vectors[0][i] - triangle.vectors[2][i]);
	for(i=0; i<3; i++)
		matrix_A[i][2] = ray.ray_direct[i];

	//Save the matrix for calculating beta
	for(i=0; i<3; i++)
		matrix_beta[i][0] = (triangle.vectors[0][i] - ray.ray_point[i]);
	for(i=0; i<3; i++)
		matrix_beta[i][1] = (triangle.vectors[0][i] - triangle.vectors[2][i]);
	for(i=0; i<3; i++)
		matrix_beta[i][2] = ray.ray_direct[i];

	//Save the matrix for calculating gamma
	for(i=0; i<3; i++)
		matrix_gamma[i][0] = (triangle.vectors[0][i] - triangle.vectors[1][i]);
	for(i=0; i<3; i++)
		matrix_gamma[i][1] = (triangle.vectors[0][i] - ray.ray_point[i]);
	for(i=0; i<3; i++)
		matrix_gamma[i][2] = ray.ray_direct[i];

	//Save the matrix for calculating parameter t
	for(i=0; i<3; i++)
		matrix_t[i][0] = (triangle.vectors[0][i] - triangle.vectors[1][i]);
	for(i=0; i<3; i++)
		matrix_t[i][1] = (triangle.vectors[0][i] - triangle.vectors[2][i]);
	for(i=0; i<3; i++)
		matrix_t[i][2] = (triangle.vectors[0][i] - ray.ray_point[i]);

	//Calculate test values
	det_A = cal_determinant_3x3(matrix_A);

	beta = (cal_determinant_3x3(matrix_beta) / det_A);
	gamma = (cal_determinant_3x3(matrix_gamma) / det_A);
	t = (cal_determinant_3x3(matrix_t) / det_A);


	//Checking for intersection
	if((beta + gamma < 1.0) && (beta > 0.0) && (gamma > 0.0) && (t > 0.0))	//true condition
	{
		//Checking for epsilon value
		if(t < EPSILON){
			return (-1.0);
		}
		
		//Debug section
		//cout<<"Intersection t: "<<t<<endl;

		return t;
	}
	else{
		return (-1.0);
	}
}

double ray_plane_intersect(RayObj &ray, Object &plane)
{
	double denominator, numerator, t;
	
	denominator = cal_dot_product(plane.vectors[0], ray.ray_direct);

	//Check for 0 denominator, ray parallel to plane
	if(denominator == 0.0)
	{
		return (-1.0);
	}

	numerator = -(plane.dist + cal_dot_product(plane.vectors[0], ray.ray_point));
	t = numerator / denominator;

	//Checking for intersection
	if(t < EPSILON)
	{
		return (-1.0);
	}

	//Debug section
	//cout<<"Intersection t: "<<t<<endl;

	return t;
}

void get_normal(Object &object, double intersect[3], double normal[3])
{
	//Will calculate the normal of of the object at the intersection pt given by intersect

	//Plane
	if(object.type == PLANE)
	{
		copy_vertex(object.vectors[0], normal);
	}
	
	//Triangle
	else if(object.type == TRIANGLE)
	{
		copy_vertex(object.vectors[3], normal);

		//Can change this portion to include interpolation for triangle normals
	}
	
	//Sphere
	else if(object.type == SPHERE)
	{
		normal[0] = (intersect[0] - object.vectors[0][0]) / object.radius;
		normal[1] = (intersect[1] - object.vectors[0][1]) / object.radius;
		normal[2] = (intersect[2] - object.vectors[0][2]) / object.radius;
	}

	else{
		cout<<"Error in get_normal. Object is unassigned"<<endl;
	}

	normalize_vector(normal);
}

void cal_reflection(double incident[3], double normal[3], double reflection[3])
{
	double IdotN = cal_dot_product(incident, normal);

	reflection[0] = incident[0] - 2*IdotN*normal[0];
	reflection[1] = incident[1] - 2*IdotN*normal[1];
	reflection[2] = incident[2] - 2*IdotN*normal[2];

	normalize_vector(reflection);
}

bool cal_refraction(double incident[3], double normal[3], double indexi, double indexr, double refraction[3])
{
	//Note that u is the ratio of the refractive indices of the incident and refracted surfaces
	double IdotN = cal_dot_product(incident, normal);
	double u = indexi/indexr;
	double u_sq = u*u;
	double sqrt_coeff = (1.0 - u_sq*(1.0 - (IdotN*IdotN)));

	if(sqrt_coeff < 0.0)		//No refraction
		return false;	

	double cos_theta = sqrt(sqrt_coeff);
	double cos_phi = -IdotN;

	double Ncoeff = cos_theta + u*cos_phi;
	

	refraction[0] = u*incident[0] - Ncoeff*normal[0];
	refraction[1] = u*incident[1] - Ncoeff*normal[1];
	refraction[2] = u*incident[2] - Ncoeff*normal[2];

	normalize_vector(refraction);

	return true;
}