/*
 * Copyright (c) 2009-2010 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
#include "jmePhysicsSpace.h"
#include "jmeBulletUtil.h"

/**
 * Author: Normen Hansen
 */
jmePhysicsSpace::jmePhysicsSpace(JNIEnv* env, jobject javaSpace) {
    //TODO: global ref? maybe not -> cleaning, rather callback class?
    this->javaPhysicsSpace = env->NewWeakGlobalRef(javaSpace);
    this->env = env;
    env->GetJavaVM(&vm);
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }
}

void jmePhysicsSpace::attachThread() {
    vm->AttachCurrentThread((void **) &env, NULL);
}

JNIEnv* jmePhysicsSpace::getEnv() {
    attachThread();
    return this->env;
}

void jmePhysicsSpace::stepSimulation(jfloat tpf, jint maxSteps, jfloat accuracy) {
    dynamicsWorld->stepSimulation(tpf, maxSteps, accuracy);
}

btThreadSupportInterface* jmePhysicsSpace::createSolverThreadSupport(int maxNumThreads) {
#ifdef _WIN32
    Win32ThreadSupport::Win32ThreadConstructionInfo threadConstructionInfo("solverThreads", SolverThreadFunc, SolverlsMemoryFunc, maxNumThreads);
    Win32ThreadSupport* threadSupport = new Win32ThreadSupport(threadConstructionInfo);
    threadSupport->startSPU();
#elif defined (USE_PTHREADS)
    PosixThreadSupport::ThreadConstructionInfo constructionInfo("collision", SolverThreadFunc,
            SolverlsMemoryFunc, maxNumThreads);
    PosixThreadSupport* threadSupport = new PosixThreadSupport(constructionInfo);
    threadSupport->startSPU();
#else
    SequentialThreadSupport::SequentialThreadConstructionInfo tci("solverThreads", SolverThreadFunc, SolverlsMemoryFunc);
    SequentialThreadSupport* threadSupport = new SequentialThreadSupport(tci);
    threadSupport->startSPU();
#endif
    return threadSupport;
}

btThreadSupportInterface* jmePhysicsSpace::createDispatchThreadSupport(int maxNumThreads) {
#ifdef _WIN32
    Win32ThreadSupport::Win32ThreadConstructionInfo threadConstructionInfo("solverThreads", processCollisionTask, createCollisionLocalStoreMemory, maxNumThreads);
    Win32ThreadSupport* threadSupport = new Win32ThreadSupport(threadConstructionInfo);
    threadSupport->startSPU();
#elif defined (USE_PTHREADS)
    PosixThreadSupport::ThreadConstructionInfo solverConstructionInfo("solver", processCollisionTask,
            createCollisionLocalStoreMemory, maxNumThreads);
    PosixThreadSupport* threadSupport = new PosixThreadSupport(solverConstructionInfo);
    threadSupport->startSPU();
#else
    SequentialThreadSupport::SequentialThreadConstructionInfo tci("solverThreads", processCollisionTask, createCollisionLocalStoreMemory);
    SequentialThreadSupport* threadSupport = new SequentialThreadSupport(tci);
    threadSupport->startSPU();
#endif
    return threadSupport;
}

void jmePhysicsSpace::createPhysicsSpace(jfloat minX, jfloat minY, jfloat minZ, jfloat maxX, jfloat maxY, jfloat maxZ, jint broadphaseId, jboolean threading) {
    // collision configuration contains default setup for memory, collision setup
    btDefaultCollisionConstructionInfo cci;
    //    if(threading){
    //        cci.m_defaultMaxPersistentManifoldPoolSize = 32768;
    //    }
    btCollisionConfiguration* collisionConfiguration = new btDefaultCollisionConfiguration(cci);

    btVector3 min = btVector3(minX, minY, minZ);
    btVector3 max = btVector3(maxX, maxY, maxZ);

    btBroadphaseInterface* broadphase;

    switch (broadphaseId) {
        case 0:
            broadphase = new btSimpleBroadphase();
            break;
        case 1:
            broadphase = new btAxisSweep3(min, max);
            break;
        case 2:
            //TODO: 32bit!
            broadphase = new btAxisSweep3(min, max);
            break;
        case 3:
            broadphase = new btDbvtBroadphase();
            break;
        case 4:
            //            broadphase = new btGpu3DGridBroadphase(
            //                    min, max,
            //                    20, 20, 20,
            //                    10000, 1000, 25);
            break;
    }

    btCollisionDispatcher* dispatcher;
    btConstraintSolver* solver;
    // use the default collision dispatcher. For parallel processing you can use a diffent dispatcher (see Extras/BulletMultiThreaded)
    if (threading) {
        btThreadSupportInterface* dispatchThreads = createDispatchThreadSupport(4);
        dispatcher = new SpuGatheringCollisionDispatcher(dispatchThreads, 4, collisionConfiguration);
        dispatcher->setDispatcherFlags(btCollisionDispatcher::CD_DISABLE_CONTACTPOOL_DYNAMIC_ALLOCATION);
    } else {
        dispatcher = new btCollisionDispatcher(collisionConfiguration);
    }

    // the default constraint solver. For parallel processing you can use a different solver (see Extras/BulletMultiThreaded)
    if (threading) {
        btThreadSupportInterface* solverThreads = createSolverThreadSupport(4);
        solver = new btParallelConstraintSolver(solverThreads);
    } else {
        solver = new btSequentialImpulseConstraintSolver;
    }

    //create dynamics world
    btDiscreteDynamicsWorld* world = new btDiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfiguration);
    dynamicsWorld = world;
    dynamicsWorld->setWorldUserInfo(this);

    //parallel solver requires the contacts to be in a contiguous pool, so avoid dynamic allocation
    if (threading) {
        world->getSimulationIslandManager()->setSplitIslands(false);
        world->getSolverInfo().m_numIterations = 4;
        world->getSolverInfo().m_solverMode = SOLVER_SIMD + SOLVER_USE_WARMSTARTING; //+SOLVER_RANDMIZE_ORDER;
        world->getDispatchInfo().m_enableSPU = true;
    }

    broadphase->getOverlappingPairCache()->setInternalGhostPairCallback(new btGhostPairCallback());

    dynamicsWorld->setGravity(btVector3(0, -9.81f, 0));

    struct jmeFilterCallback : public btOverlapFilterCallback {
        // return true when pairs need collision

        virtual bool needBroadphaseCollision(btBroadphaseProxy* proxy0, btBroadphaseProxy * proxy1) const {
            bool collides = (proxy0->m_collisionFilterGroup & proxy1->m_collisionFilterMask) != 0;
            collides = collides && (proxy1->m_collisionFilterGroup & proxy0->m_collisionFilterMask);

            //add some additional logic here that modified 'collides'
            return collides;
        }
    };
    dynamicsWorld->getPairCache()->setOverlapFilterCallback(new jmeFilterCallback());
    dynamicsWorld->setInternalTickCallback(&jmePhysicsSpace::preTickCallback, static_cast<void *> (this), true);
    dynamicsWorld->setInternalTickCallback(&jmePhysicsSpace::postTickCallback, static_cast<void *> (this));
}

void jmePhysicsSpace::preTickCallback(btDynamicsWorld *world, btScalar timeStep) {
    jmePhysicsSpace* dynamicsWorld = (jmePhysicsSpace*) world->getWorldUserInfo();
    JNIEnv* env = dynamicsWorld->getEnv();
    env->CallVoidMethod(dynamicsWorld->getJavaPhysicsSpace(), jmeClasses::PhysicsSpace_preTick, timeStep);
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }
}

void jmePhysicsSpace::postTickCallback(btDynamicsWorld *world, btScalar timeStep) {
    jmePhysicsSpace* dynamicsWorld = (jmePhysicsSpace*) world->getWorldUserInfo();
    JNIEnv* env = dynamicsWorld->getEnv();
    env->CallVoidMethod(dynamicsWorld->getJavaPhysicsSpace(), jmeClasses::PhysicsSpace_postTick, timeStep);
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }
}

btDynamicsWorld* jmePhysicsSpace::getDynamicsWorld() {
    return dynamicsWorld;
}

jobject jmePhysicsSpace::getJavaPhysicsSpace() {
    return javaPhysicsSpace;
}

jmePhysicsSpace::~jmePhysicsSpace() {
    delete(dynamicsWorld);
}