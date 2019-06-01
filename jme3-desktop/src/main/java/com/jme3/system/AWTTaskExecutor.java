/*
 * Copyright (c) 2009-2019 jMonkeyEngine
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
package com.jme3.system;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>
 * This class is dedicated to the queuing of AWT related tasks and their execution.
 * It's able to store tasks that have to be executed within an AWT context and execute them at the specified time.
 * </p>
 * <p>
 * This class is an AWT implementation of the <a href="http://www.oracle.com/technetwork/java/javase/overview/javafx-overview-2158620.html">JavaFX</a> original code provided by Alexander Brui (see <a href="https://github.com/JavaSaBr/JME3-JFX">JME3-FX</a>)
 * </p>
 * @author Julien Seinturier - COMEX SA - <a href="http://www.seinturier.fr">http://www.seinturier.fr</a>
 * @author Alexander Brui (JavaSaBr)
 */
public class AWTTaskExecutor {
  
  private final ReadWriteLock  lock = new ReentrantReadWriteLock();
  
  private static final AWTTaskExecutor INSTANCE = new AWTTaskExecutor();

  /**
   * Get the instance of the executor.
   * @return the instance of executor.
   */
  public static AWTTaskExecutor getInstance() {
      return INSTANCE;
  }

  /**
   * The list of waiting tasks.
   */
  private final List<Runnable> waitTasks;

  private AWTTaskExecutor() {
    waitTasks = new LinkedList<Runnable>();
  }

  public List<Runnable> getWaitingTasks(){
    return waitTasks;
  }
  
  /**
   * Add the given {@link Runnable runnable} to the list of planned executions. 
   * @param task the task to add.
   * @see #execute()
   */
  public void addToExecute(final Runnable task) {
    lock.writeLock().lock();
    try {
      waitTasks.add(task);
    } catch (Exception e) {
      // This try catch block enable to free the lock in case of any unexpected error.
    }
    lock.writeLock().unlock();
  }

  /**
   * Execute all the tasks that are waiting.
   * @see #addToExecute(Runnable)
   */
  public void execute() {

      if (waitTasks.isEmpty()) return;

      lock.readLock().lock();
      
      try {
        for(Runnable runnable : waitTasks) {
          runnable.run();
        }
      } catch (Exception e) {
        // This try catch block enable to free the lock in case of any unexpected error.
      }
      
      waitTasks.clear();
      
      lock.readLock().unlock();
  }
}
