/* This file includes all simple utility files required for this program */

/////Vector calculation functions/////
void cal_direction(double direction[3], double vertex1[3], double vertex2[3]);

void cal_cross_product(double cross[3], double vector1[3], double vector2[3]);

double cal_dot_product(double vector1[3], double vector2[3]);

double cal_determinant_3x3(double matrix[3][3]);

double cal_magnitude(double vector[3]);

void cal_point_on_line(double point[3], double origin[3], double direction[3], double t);

void normalize_vector(double vector[3]);

void copy_vertex(double copy[3], double vertex[3]);

void set_vector(double vector[3], double a, double b, double c);

void print_vector(char *string, double vector[3]);