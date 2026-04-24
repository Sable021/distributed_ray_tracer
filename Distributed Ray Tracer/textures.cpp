#include <iostream>
#include <math.h>
#include "utility.h"

using namespace std;

#define PI 3.141592654

#define s_curve(t) ( t * t * (3. - 2. * t) )

#define lerp(t, a, b) ( a + t * (b - a) )

#define setup(i,b0,b1,r0,r1)\
	t = vec[i] + N;\
	b0 = ((int)t) & BM;\
	b1 = (b0+1) & BM;\
	r0 = t - (int)t;\
	r1 = r0 - 1.;

#define B 0x100
#define BM 0xff

#define N 0x1000
#define NP 12   /* 2^N */
#define NM 0xfff

static int p[B + B + 2];
static double g3[B + B + 2][3];

double noise(double vec[3])
{
	int bx0, bx1, by0, by1, bz0, bz1, b00, b10, b01, b11;
	double rx0, rx1, ry0, ry1, rz0, rz1, *q, sy, sz, a, b, c, d, t, u, v;
	int register i, j;

	int i1, j1, k1;

	for (i1 = 0 ; i1 < B ; i1++)
	{
		p[i1] = i1;

		for (j1 = 0 ; j1 < 3 ; j1++)
			g3[i1][j1] = (double)((rand() % (B + B)) - B) / B;
		normalize_vector(g3[i1]);
	}

	while (--i1)
	{
		k1 = p[i1];
		p[i1] = p[j1 = rand() % B];
		p[j1] = k1;
	}

	for (i1 = 0 ; i1 < B + 2 ; i1++)
	{
		p[B + i1] = p[i1];

		for (j1 = 0 ; j1 < 3 ; j1++)
			g3[B + i1][j1] = g3[i1][j1];
	}

	setup(0, bx0,bx1, rx0,rx1);
	setup(1, by0,by1, ry0,ry1);
	setup(2, bz0,bz1, rz0,rz1);

	i = p[ bx0 ];
	j = p[ bx1 ];

	b00 = p[ i + by0 ];
	b10 = p[ j + by0 ];
	b01 = p[ i + by1 ];
	b11 = p[ j + by1 ];

	t  = s_curve(rx0);
	sy = s_curve(ry0);
	sz = s_curve(rz0);

#define at3(rx,ry,rz) ( rx * q[0] + ry * q[1] + rz * q[2] )

	q = g3[ b00 + bz0 ] ; u = at3(rx0,ry0,rz0);
	q = g3[ b10 + bz0 ] ; v = at3(rx1,ry0,rz0);
	a = lerp(t, u, v);

	q = g3[ b01 + bz0 ] ; u = at3(rx0,ry1,rz0);
	q = g3[ b11 + bz0 ] ; v = at3(rx1,ry1,rz0);
	b = lerp(t, u, v);

	c = lerp(sy, a, b);

	q = g3[ b00 + bz1 ] ; u = at3(rx0,ry0,rz1);
	q = g3[ b10 + bz1 ] ; v = at3(rx1,ry0,rz1);
	a = lerp(t, u, v);

	q = g3[ b01 + bz1 ] ; u = at3(rx0,ry1,rz1);
	q = g3[ b11 + bz1 ] ; v = at3(rx1,ry1,rz1);
	b = lerp(t, u, v);

	d = lerp(sy, a, b);

	return lerp(sz, c, d);
}

void checkerboard(double intersect[3], double colour[3])
{
	if (intersect[0] >= 0)
	{
		if ((int) (5 * intersect[2]) % 2 == 0)
		{
			if ((int) (5 * intersect[0]) % 2 == 0 && (int) (5 * intersect[1]) % 2 == 0)
				set_vector(colour, 0.5, 0.5, 0.5);
			else
				set_vector(colour, 0.0, 0.25, 0.4);
		}
		else
		{
			if ((int) (5 * intersect[0]) % 2 == 0 && (int) (5 * intersect[1]) % 2 == 0)
				set_vector(colour, 0.0, 0.25, 0.4);
			else
				set_vector(colour, 0.5, 0.5, 0.5);
		}
	}
	else
	{
		if ((int) (5 * intersect[2]) % 2 == 0)
		{
			if ((int) (5 * intersect[0]) % 2 == 0 && (int) (5 * intersect[1]) % 2 == 0)
				set_vector(colour, 0.0, 0.25, 0.4);
			else
				set_vector(colour, 0.5, 0.5, 0.5);
		}
		else
		{
			if ((int) (5 * intersect[0]) % 2 == 0 && (int) (5 * intersect[1]) % 2 == 0)
				set_vector(colour, 0.5, 0.5, 0.5);
			else
				set_vector(colour, 0.0, 0.25, 0.4);
		}
	}
}


void mixChecks(double intersect[3], double colour[3])
{
	double turbulent_pt[3];

	//We perturb the intersect point, but still keep the original one
	for(int i = 0; i < 3; i++)
		turbulent_pt[i] = sin(intersect[i]);

	if (turbulent_pt[0] >= 0)
	{
		if ((int) (5 * turbulent_pt[2]) % 2 == 0)
		{
			if ((int) (5 * turbulent_pt[0]) % 2 == 0 && (int) (5 * turbulent_pt[1]) % 2 == 0)
				set_vector(colour, 0.4, 0.4, 0.4);
			else
				set_vector(colour, 0.0, 0.15, 0.3);
		}
		else
		{
			if ((int) (5 * turbulent_pt[0]) % 2 == 0 && (int) (5 * turbulent_pt[1]) % 2 == 0)
				set_vector(colour, 0.0, 0.15, 0.3);
			else
				set_vector(colour, 0.4, 0.4, 0.4);
		}
	}
	else
	{
		if ((int) (5 * turbulent_pt[2]) % 2 == 0)
		{
			if ((int) (5 * turbulent_pt[0]) % 2 == 0 && (int) (5 * turbulent_pt[1]) % 2 == 0)
				set_vector(colour, 0.0, 0.15, 0.3);
			else
				set_vector(colour, 0.4, 0.4, 0.4);
		}
		else
		{
			if ((int) (5 * turbulent_pt[0]) % 2 == 0 && (int) (5 * turbulent_pt[1]) % 2 == 0)
				set_vector(colour, 0.4, 0.4, 0.4);
			else
				set_vector(colour, 0.0, 0.15, 0.3);
		}
	}
}

/*void woodgrain(double intersect[3], double colour[3])
{
	const double R_LIGHT = 0.72, G_LIGHT = 0.57, B_LIGHT = 0.26;
	const double R_DARK = 0.42, G_DARK = 0.26, B_DARK = 0.15;
	double radius, angle;
	int grain;

	radius = sqrt(intersect[0] * intersect[0] + intersect[1] * intersect[1]);

	if (intersect[2] == 0)
		angle = PI / 2;
	else
		angle = atan2(intersect[0], intersect[2]);

	if (angle < 0)
		angle += (2 * PI);

	radius = radius + 2 * sin(20 * angle + intersect[1] /150);
	grain = (int)(radius + 0.5) % 60;

	if (grain < 5)
	{
		colour[0] = R_LIGHT;
		colour[1] = G_LIGHT;
		colour[2] = B_LIGHT;
	}
	else
	{
		colour[0] = R_DARK;
		colour[1] = G_DARK;
		colour[2] = B_DARK;
	}
}*/

void colourful(double intersect[3], double colour[3])
{
	normalize_vector(intersect);
	
	for (int i = 0; i < 3; i++)
		colour[i] = fabs(sin(intersect[i]));
	/*colour[0] = intersect[0];
	colour[1] = (intersect[0] + intersect[1]) / 2;
	colour[2] = intersect[2];*/
}

void strips(double intersect[3], double colour[3])
{
	const double R_LIGHT = 0.82, G_LIGHT = 0.67, B_LIGHT = 0.56;
	const double R_DARK = 0.52, G_DARK = 0.36, B_DARK = 0.25;
	double radius, angle;
	int grain;

	double turbulent_pt[3];

	
	//We perturb the intersect point, but still keep the original one
	for (int i = 0; i < 3; i++)
		turbulent_pt[i] = fabs(sin(intersect[i]));

	for (int i = 0; i < 3; i++)
		turbulent_pt[i] = fabs(sin(turbulent_pt[i]));
	

	radius = sqrt(turbulent_pt[0] * turbulent_pt[0] + turbulent_pt[1] * turbulent_pt[1]);

	if (turbulent_pt[2] == 0)
		angle = PI / 2;
	else
		angle = atan2(turbulent_pt[0], turbulent_pt[2]);

	if (angle < 0)
		angle += (2 * PI);

	radius = radius + 2 * sin(20 * angle + turbulent_pt[1] /150);
	grain = (int)(radius + 0.5) % 5;

	if (grain < 1)
	{
		colour[0] = R_LIGHT;
		colour[1] = G_LIGHT;
		colour[2] = B_LIGHT;
	}
	else
	{
		colour[0] = R_DARK;
		colour[1] = G_DARK;
		colour[2] = B_DARK;
	}
}
