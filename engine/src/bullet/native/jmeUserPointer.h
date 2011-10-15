#ifndef _Included_jmeUserPointer
#define _Included_jmeUserPointer
#include <jni.h>
class jmeUserPointer {
public:
    jobject javaCollisionObject;
    jint group;
    jint groups;
    jmePhysicsSpace *space;
};
#endif