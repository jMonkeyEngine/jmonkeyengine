# /*
# Bullet Continuous Collision Detection and Physics Library for Android NDK
# Copyright (c) 2006-2009 Noritsuna Imamura  <a href="http://www.siprop.org/" rel="nofollow">http://www.siprop.org/</a>
#
# This software is provided 'as-is', without any express or implied warranty.
# In no event will the authors be held liable for any damages arising from the use of this software.
# Permission is granted to anyone to use this software for any purpose,
# including commercial applications, and to alter it and redistribute it freely,
# subject to the following restrictions:
#
# 1. The origin of this software must not be misrepresented; you must not claim that you wrote the original software. If you use this software in a product, an acknowledgment in the product documentation would be appreciated but is not required.
# 2. Altered source versions must be plainly marked as such, and must not be misrepresented as being the original software.
# 3. This notice may not be removed or altered from any source distribution.
# */
LOCAL_PATH:= $(call my-dir)
BULLET_PATH:= ${LOCAL_PATH}/../

include $(CLEAR_VARS)

LOCAL_MODULE    := bulletjme
LOCAL_C_INCLUDES := $(BULLET_PATH)/\
    $(BULLET_PATH)/BulletCollision\
    $(BULLET_PATH)/BulletCollision/BroadphaseCollision\
    $(BULLET_PATH)/BulletCollision/CollisionDispatch\
    $(BULLET_PATH)/BulletCollision/CollisionShapes\
    $(BULLET_PATH)/BulletCollision/NarrowPhaseCollision\
    $(BULLET_PATH)/BulletCollision/Gimpact\
    $(BULLET_PATH)/BulletDynamics\
    $(BULLET_PATH)/BulletDynamics/ConstraintSolver\
    $(BULLET_PATH)/BulletDynamics/Dynamics\
    $(BULLET_PATH)/BulletDynamics/Vehicle\
    $(BULLET_PATH)/BulletDynamics/Character\
    $(BULLET_PATH)/BulletMultiThreaded\
    $(BULLET_PATH)/BulletMultiThreaded/GpuSoftBodySolvers\
    $(BULLET_PATH)/BulletMultiThreaded/SpuNarrowPhaseCollisionTask\
    $(BULLET_PATH)/BulletMultiThreaded/SpuSampleTask\
    $(BULLET_PATH)/BulletMultiThreaded/GpuSoftBodySolvers/CPU\
    $(BULLET_PATH)/BulletMultiThreaded/GpuSoftBodySolvers/DX11\
    $(BULLET_PATH)/BulletMultiThreaded/GpuSoftBodySolvers/OpenCL\
    $(BULLET_PATH)/BulletMultiThreaded/GpuSoftBodySolvers/DX11/HLSL\
    $(BULLET_PATH)/BulletMultiThreaded/GpuSoftBodySolvers/OpenCL/AMD\
    $(BULLET_PATH)/BulletMultiThreaded/GpuSoftBodySolvers/OpenCL/Apple\
    $(BULLET_PATH)/BulletMultiThreaded/GpuSoftBodySolvers/OpenCL/MiniCL\
    $(BULLET_PATH)/BulletMultiThreaded/GpuSoftBodySolvers/OpenCL/NVidia\
    $(BULLET_PATH)/BulletMultiThreaded/GpuSoftBodySolvers/OpenCL/OpenCLC\
    $(BULLET_PATH)/BulletMultiThreaded/GpuSoftBodySolvers/OpenCL/OpenCLC10\
    $(BULLET_PATH)/LinearMath\
    $(BULLET_PATH)/BulletSoftBody\
    $(BULLET_PATH)/LinearMath\
    $(BULLET_PATH)/MiniCL\
    $(BULLET_PATH)/MiniCL/MiniCLTask\
    $(BULLET_PATH)/vectormath\
    $(BULLET_PATH)/vectormath/scalar\
    $(BULLET_PATH)/vectormath/sse\
    $(BULLET_PATH)/vectormath/neon

#ARM mode more performant than thumb for old armeabi
ifeq ($(TARGET_ARCH_ABI),$(filter $(TARGET_ARCH_ABI), armeabi))
LOCAL_ARM_MODE := arm
endif 

#Enable neon for armv7
ifeq ($(TARGET_ARCH_ABI),$(filter $(TARGET_ARCH_ABI), armeabi-v7a))
LOCAL_ARM_NEON := true
endif

LOCAL_CFLAGS := $(LOCAL_C_INCLUDES:%=-I%) 
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -ldl -lm -llog

FILE_LIST := $(wildcard $(LOCAL_PATH)/*.cpp)
FILE_LIST += $(wildcard $(LOCAL_PATH)/**/*.cpp)
FILE_LIST += $(wildcard $(LOCAL_PATH)/**/**/*.cpp)
FILE_LIST := $(filter-out $(wildcard $(LOCAL_PATH)/Bullet3OpenCL/**/*.cpp), $(FILE_LIST))
LOCAL_SRC_FILES := $(FILE_LIST:$(LOCAL_PATH)/%=%)

include $(BUILD_SHARED_LIBRARY)
