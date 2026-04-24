//
// Author: Kok-Lim Low <lowkl@comp.nus.edu.sg>
// Date: 09 Feb 2007
//
//
// Support reading and writing of PPM P6 format.
//
// NOTE: The RGB image is stored in the PPM file in row-major,
// top-to-bottom order. The 1-D array returned by or given to the 
// functions are also assumed to be storing the RGB image in the 
// same order. Each pixel is stored as a RGB triplet of bytes, with
// R component in the lower memory address.
//
// NOTE: The functions here do not support multiple images per PPM,
// and double-byte color channel is not supported.
//
//
// See http://netpbm.sourceforge.net/doc/ppm.html for details of 
// PPM P6 format.


#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include "ppmio.h"


#ifdef __cplusplus
extern "C" {
#endif


#define TEXTLINE_LEN	255



unsigned char *ppm_read( const char *filename, 
					     int *img_height, int *img_width )

	// Reads and returns an RGB image from a PPM P6 image file.
	// Returns NULL if not successful.
    // If successful, image height and width are store in 
    // *img_height & *img_width respectively.
{

// Checking.

	if ( filename == NULL || img_height == NULL || img_width == NULL )
	{
		fprintf( stderr, "ppm_read() failed: Invalid input arguments.\n" );
		return NULL;
	}


// Open file.

	FILE *fp = fopen( filename, "rb" ); // binary file
	if ( fp == NULL )
	{
		fprintf( stderr, "ppm_read() failed: Cannot open file \"%s\" for reading.\n", filename );
		return NULL;
	}


// Read header (in ASCII).

	int height, width, maxval;
	unsigned char *rgb_image;

	// Line buffer
	char textLine[ TEXTLINE_LEN + 1 ];
	char textLine2[ TEXTLINE_LEN + 1 ];
	textLine[0] = '\0';


	do {
		fscanf( fp, "%s", textLine );
		if ( textLine[0] == '#' ) fgets( textLine2, TEXTLINE_LEN, fp );
	} while ( textLine[0] == '#' );

	if ( strcmp( textLine, "P6" ) != 0 )
	{
		fprintf( stderr, "ppm_read() failed: Invalid file format.\n" );
		fclose( fp );
		return 0;

	}


	do {
		fscanf( fp, "%s", textLine );
		if ( textLine[0] == '#' ) fgets( textLine2, TEXTLINE_LEN, fp );
	} while ( textLine[0] == '#' );

	if ( sscanf( textLine, "%d", &width ) != 1 || width <= 0 )
	{
		fprintf( stderr, "ppm_read() failed: Invalid file format.\n" );
		fclose( fp );
		return 0;

	}


	do {
		fscanf( fp, "%s", textLine );
		if ( textLine[0] == '#' ) fgets( textLine2, TEXTLINE_LEN, fp );
	} while ( textLine[0] == '#' );

	if ( sscanf( textLine, "%d", &height ) != 1 || height <= 0 )
	{
		fprintf( stderr, "ppm_read() failed: Invalid file format.\n" );
		fclose( fp );
		return 0;

	}


	do {
		fscanf( fp, "%s", textLine );
		if ( textLine[0] == '#' ) fgets( textLine2, TEXTLINE_LEN, fp );
	} while ( textLine[0] == '#' );

	if ( sscanf( textLine, "%d", &maxval ) != 1 || maxval < 0 || maxval > 255)
	{
		fprintf( stderr, "ppm_read() failed: Invalid file format.\n" );
		fclose( fp );
		return 0;

	}

	fscanf( fp, " ", textLine );


// Write image data (in binary).

	rgb_image = (unsigned char *) malloc( sizeof(unsigned char) * height * width * 3 );

	if ( rgb_image == NULL )
	{
		fprintf( stderr, "ppm_read() failed: Cannot allocate memory.\n" );
		fclose( fp );
		return NULL;
	}


	if ( fread( rgb_image, 1, height*width*3, fp ) != (unsigned) (height*width*3)  )
	{
		fprintf( stderr, "ppm_read() failed: "
			     "Error reading binary data from file \"%s\".\n", filename );
		fclose( fp );
		return NULL;
	}


	fclose( fp );
	*img_height = height;
	*img_width = width;
	return rgb_image;
}





int ppm_write( const char *filename, const unsigned char *rgb_image,
			   int img_height, int img_width )

	// Writes a RGB image to a PPM P6 image file.
	// Returns nonzero (true) if successful, otherwise returns zero (false).
{

// Checking.

	if ( filename == NULL || rgb_image == NULL ||
		 img_height <= 0 || img_width <= 0 )
	{
		fprintf( stderr, "ppm_write() failed: Invalid input arguments.\n" );
		return 0;
	}


// Open file.

	FILE *fp = fopen( filename, "wb" ); // binary file
	if ( fp == NULL )
	{
		fprintf( stderr, "ppm_write() failed: Cannot open file \"%s\" for writing.\n", filename );
		return 0;
	}


// Write header (in ASCII).

	if ( fprintf( fp, "P6 %d %d 255\n", img_width, img_height ) < 2 )
	{
		fprintf( stderr, "ppm_write() failed: Error writing header to file \"%s\".\n", filename );
		fclose( fp );
		return 0;
	} 


// Write image data (in binary).

	if ( fwrite( rgb_image, 1, img_height*img_width*3, fp ) != (unsigned) (img_height*img_width*3) )
	{
		fprintf( stderr, "ppm_write() failed: "
			     "Error writing binary data to file \"%s\".\n", filename );
		fclose( fp );
		return 0;
	}


	fclose( fp );
	return 1;
}




#ifdef __cplusplus
}
#endif

