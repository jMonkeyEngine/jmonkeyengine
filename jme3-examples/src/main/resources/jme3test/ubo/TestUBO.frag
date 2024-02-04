
struct PointLight {
    vec3 position;
    float radius;
    vec4 color;
};

struct DirectLight {
    vec3 direction;
    vec4 color;
};

struct TestStruct{
    int nDirectLights;
    DirectLight test1;
    int test2;
    PointLight test3;
    int test4;
    mat3 test5;
    int test6;
    mat4 test7;
    int test8;
    DirectLight directLights[1];
    int nPointLights;
    PointLight pointLights[2];
    PointLight pointLights2[2];
    int test13;
};

layout (std140) uniform m_TestStruct1 {
    TestStruct testStruct1;
};

layout (std140) uniform m_TestStruct2 {
    TestStruct testStruct2;
};

out vec4 outFragColor;

void main(){
    if(
        testStruct1.nDirectLights==1
        &&testStruct1.test1.direction==vec3(0,1,0)
        &&testStruct1.test1.color==vec4(0,0,1,1)
        &&testStruct1.test2==111
        &&testStruct1.test3.position==vec3(7.,9.,7.)    
        &&testStruct1.test3.radius==99.
        &&testStruct1.test3.color==vec4(1.,0.,0.,1.)
        &&testStruct1.test4==222        
        &&testStruct1.test5==transpose(mat3(1,2,3,4,5,6,7,8,9) )               
        &&testStruct1.test6==333
        &&testStruct1.test7==transpose(mat4(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16))
        &&testStruct1.test8==444
        &&testStruct1.directLights[0].direction==vec3(0.,0.,1.)
        &&testStruct1.directLights[0].color==vec4(0.,1.,0.,1.)        
        &&testStruct1.nPointLights==2
        &&testStruct1.pointLights[0].position==vec3(5.,9.,7.)
        &&testStruct1.pointLights[0].radius==9.
        &&testStruct1.pointLights[0].color==vec4(1.,0.,0.,1.)        
        &&testStruct1.pointLights[1].position==vec3(5.,10.,7.)    
        &&testStruct1.pointLights[1].radius==8.
        &&testStruct1.pointLights[1].color==vec4(0.,1.,0.,1.)   
        &&testStruct1.pointLights2[0].position==vec3(3.,9.,7.)
        &&testStruct1.pointLights2[0].radius==91.
        &&testStruct1.pointLights2[0].color==vec4(0.,1.,0.,1.)   
        &&testStruct1.pointLights2[1].position==vec3(3.,10.,7.)
        &&testStruct1.pointLights2[1].radius==90.
        &&testStruct1.pointLights2[1].color==vec4(0.,0.,1.,1.)
        &&testStruct1.test13==555

        &&

        testStruct2.nDirectLights==1
        &&testStruct2.test1.direction==vec3(0,1,0)
        &&testStruct2.test1.color==vec4(0,0,1,1)
        &&testStruct2.test2==111
        &&testStruct2.test3.position==vec3(7.,9.,7.)    
        &&testStruct2.test3.radius==99.
        &&testStruct2.test3.color==vec4(1.,0.,0.,1.)
        &&testStruct2.test4==222        
        &&testStruct2.test5==transpose(mat3(1,2,3,4,5,6,7,8,9) )               
        &&testStruct2.test6==333
        &&testStruct2.test7==transpose(mat4(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16))
        &&testStruct2.test8==999999
        &&testStruct2.directLights[0].direction==vec3(0.,0.,1.)
        &&testStruct2.directLights[0].color==vec4(0.,1.,0.,1.)        
        &&testStruct2.nPointLights==2
        &&testStruct2.pointLights[0].position==vec3(5.,9.,7.)
        &&testStruct2.pointLights[0].radius==9.
        &&testStruct2.pointLights[0].color==vec4(1.,0.,0.,1.)        
        &&testStruct2.pointLights[1].position==vec3(5.,10.,7.)    
        &&testStruct2.pointLights[1].radius==8.
        &&testStruct2.pointLights[1].color==vec4(0.,1.,0.,1.)   
        &&testStruct2.pointLights2[0].position==vec3(3.,9.,7.)
        &&testStruct2.pointLights2[0].radius==91.
        &&testStruct2.pointLights2[0].color==vec4(0.,1.,0.,1.)   
        &&testStruct2.pointLights2[1].position==vec3(3.,10.,7.)
        &&testStruct2.pointLights2[1].radius==90.
        &&testStruct2.pointLights2[1].color==vec4(0.,0.,1.,1.)
        &&testStruct2.test13==111

    ){
        outFragColor=vec4(0,1,0,1);
    }else{
        outFragColor=vec4(1,0,0,1);
    }
}