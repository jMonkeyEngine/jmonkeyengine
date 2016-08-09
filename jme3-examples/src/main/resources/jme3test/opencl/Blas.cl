__kernel void Fill (__global TYPE* data, TYPE a)
{
	data[get_global_id(0)] = a;
}