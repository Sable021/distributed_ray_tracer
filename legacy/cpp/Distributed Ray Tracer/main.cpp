#include <stdlib.h>
#include <GL/glut.h>
#include <iostream>

using namespace std;

#include "utility.h"
#include "intersect.h"	//This files includes object.h as well
#include "RayTracer.h"
#include "ppmio.h"

GLubyte pixel_buffer[scrHeight][scrWidth][3];

void outputFile()
{
	//This function transfers the pixel buffer into the output file format in PPM
	unsigned char *image;
	int i_begin = 0;

	cout<<"Outputting file to .ppm format..."<<endl;

	image = (unsigned char *) malloc( sizeof(unsigned char) * scrHeight * scrWidth * 3 );
	
	for (int i = scrHeight - 1; i >= 0; i--) //flip the image about the x-axis to get image in correct orientation
	{
		for (int j = 0; j < scrWidth; j++)
		{
			for (int k = 0; k < 3; k++)			
				image[(i_begin * scrWidth + j) * 3 + k] = pixel_buffer[i][j][k];
		}

		i_begin++;
	}

	if (ppm_write("raytracing.ppm", image, scrHeight, scrWidth) == 0 ) exit(EXIT_FAILURE);
}

void doRayTracing()
{
	//This functions will begin the ray tracing program and update the pixel_buffer
	cout<<"Rendering Scene... Please be Patient"<<endl;
	
	if(render_rays() == true)
	{
		outputFile();
		cout<<"Rendering Done, Displaying..."<<endl;
	}
	else{
		cout<<"Unable to render ray tracing scene. Exiting..."<<endl;
		exit(1);
	}
}
void init(void)
{
	glClearColor(0.0, 0.0, 0.0, 0.0);
	glShadeModel(GL_FLAT);

	//Set the screen to draw pixels
	glViewport(0, 0, scrWidth, scrHeight);
	glMatrixMode(GL_PROJECTION);
	glLoadIdentity();
	glFrustum(-1.0, 1.0, -1.0, 1.0, 0.0, 10.0);
	glMatrixMode(GL_MODELVIEW);
	glLoadIdentity();

	glRasterPos3f(-1.0, -1.0, -0.15);

	//Set up the scene
	initialise_scene();
	//Start Ray Tracing
	doRayTracing();
	glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
}

/*
void reshapeFcn(GLint newWidth GLint newHeight)
{
	glViewport(0, 0, newWidth, newHeight);

	glMatrixMode(GL_PROJECTION);
	glLoadIdentity();
	glFrustum(-1.0, 1.0, -1.0, 1.0, 0.0, 10.0);
	glMatrixMode(GL_MODELVIEW);
	glLoadIdentity();

	winWidth = newWidth;
	winHeight = newHeight;
}
*/

void display(void)
{
	GLboolean *valid, valid2 = false;

	valid = &valid2;

	glClear(GL_COLOR_BUFFER_BIT);
	glGetBooleanv(GL_CURRENT_RASTER_POSITION_VALID, valid);
	if(*valid == false)
		cout<<"current raster position is invalid!"<<endl;
	else
	glDrawPixels(scrWidth, scrHeight, GL_RGB, GL_UNSIGNED_BYTE, pixel_buffer);

	glFlush();
	glutSwapBuffers();
}


int main(int argc, char *argv[])
{
	glutInit(&argc, argv);
	glutInitDisplayMode(GLUT_DOUBLE|GLUT_RGB);
	glutInitWindowPosition(50, 50);
	glutInitWindowSize(scrWidth, scrHeight);
	glutCreateWindow("Ray Tracing");

	init();

	glutDisplayFunc(display);
	//glutReshapeFunc(reshapeFcn);

	glutMainLoop();
	return 0;
}