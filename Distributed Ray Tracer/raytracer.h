//////Screen Resolution is defined here! Fixed!/////
#define scrWidth 1024
#define scrHeight 768

extern GLubyte pixel_buffer[scrHeight][scrWidth][3];	//buffer to store image colour components

//The above variables are used by the main openGL file and so are defined here


bool render_rays();	//Function to begin the raytrace

void ray_trace(RayObj &ray, int depth, double rindex, double colour[3], bool inside, int ray_num);	//Recursive Function