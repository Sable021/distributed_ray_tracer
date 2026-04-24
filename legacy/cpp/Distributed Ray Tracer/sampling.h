#define MAX_GRID_X 8
#define MAX_GRID_Y 8

void create_light_grid(int gsize_x, int gsize_y, int index);

void get_light_grid_delta(int index, double &light_DX, double &light_DY, double light_grid_DX[3], double light_grid_DY[3]);

double random(double min, double max);

void create_sample_grid(double sample_grid[MAX_GRID_X][MAX_GRID_Y][3], int gsize_x, int gsize_y, double N[3], double midpt[3]);

void get_sample_vertex(double sample_vertex[3], int gsize_x, int gsize_y, double N[3], double midpoint[3], int trace_num);

void get_grid_number(int trace_num, int gsize_x, int gsize_y, int &x, int &y);