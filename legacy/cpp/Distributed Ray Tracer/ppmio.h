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


#ifndef _PPMIO_H_
#define _PPMIO_H_

#ifdef __cplusplus
extern "C" {
#endif



extern unsigned char *ppm_read( const char *filename, 
							    int *img_height, int *img_width );

	// Reads and returns an RGB image from a PPM P6 image file.
	// Returns NULL if not successful.
    // If successful, image height and width are store in 
    // *img_height & *img_width respectively.



extern int ppm_write( const char *filename, const unsigned char *rgb_image,
			          int img_height, int img_width );

	// Writes a RGB image to a PPM P6 image file.
	// Returns nonzero (true) if successful, otherwise returns zero (false).



#ifdef __cplusplus
}
#endif


#endif  //_PPMIO_H_
