

//2 component vector to hold the real and imaginary parts of a complex number:
typedef float2 cfloat;

#define I ((cfloat)(0.0, 1.0))

inline float real(cfloat a){
     return a.x;
}
inline float imag(cfloat a){
     return a.y;
}

inline float cmod(cfloat a){
    return (sqrt(a.x*a.x + a.y*a.y));
}

inline cfloat cadd(cfloat a, cfloat b){
	return (cfloat)( a.x + b.x, a.y + b.y);
}

inline float carg(cfloat a){
    if(a.x > 0){
        return atan(a.y / a.x);

    }else if(a.x < 0 && a.y >= 0){
        return atan(a.y / a.x) + M_PI_F;

    }else if(a.x < 0 && a.y < 0){
        return atan(a.y / a.x) - M_PI_F;

    }else if(a.x == 0 && a.y > 0){
        return M_PI_F/2;

    }else if(a.x == 0 && a.y < 0){
        return -M_PI_F/2;

    }else{
        return 0;
    }
}

inline cfloat  cmult(cfloat a, cfloat b){
    return (cfloat)( a.x*b.x - a.y*b.y, a.x*b.y + a.y*b.x);
}

inline cfloat csqrt(cfloat a){
    return (cfloat)( sqrt(cmod(a)) * cos(carg(a)/2),  sqrt(cmod(a)) * sin(carg(a)/2));
}

inline float4 getColor(int iteration, int numIterations) {
	//color transition: black -> red -> blue -> white
	int step = numIterations / 2;
	if (iteration < step) {
		return mix( (float4)(0,0,0,1), (float4)(1,0,0,1), iteration / (float) step);
	} else {
		return mix( (float4)(1,0,0,1), (float4)(0,0,1,1), (iteration-step) / (float) (numIterations - step));
	}
}

__kernel void JuliaSet(write_only image2d_t outputImage, const cfloat C, int numIterations)
{
	// get id of element in array
	int x = get_global_id(0);
	int y = get_global_id(1);
	int w = get_global_size(0);
	int h = get_global_size(1);

	cfloat Z = { ( -w / 2 + x) / (w/4.0f) , ( -h / 2 + y) / (h/4.0f) };
	int iteration = 0;

	while (iteration < numIterations)
	{
		 cfloat Zpow2 = cmult(Z, Z); 
		 cfloat Zn = cadd(Zpow2, C);
		 Z.x = Zn.x;
		 Z.y = Zn.y;
		 iteration++;
		 if(cmod(Z) > 2)
		 {
			break;
		 }
	}

	float4 color;

	// threshold reached mark pixel as white
	if (iteration == numIterations)
	{
		color = (float4)(1,1,1,1);
	}
	else
	{
		color = getColor(iteration, numIterations);
	}

	write_imagef(outputImage, (int2)(x, y), color);
}