/*
 * Copyright (c) 2009-2023 jMonkeyEngine All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.system;

import java.util.ArrayList;

/**
 * This class holds all information about all monitors that where return from the glfwGetMonitors()
 * call. It stores them into an <ArrayList>
 * 
 * @author Kevin Bales
 */
public class Monitors {

  private ArrayList<MonitorInfo> monitors = new ArrayList<MonitorInfo>();

  public int addNewMonitor(long monitorID) {
    MonitorInfo info = new MonitorInfo();
    info.monitorID = monitorID;
    monitors.add(info);
    return monitors.size() - 1;
  }

  /**
   * This function returns the size of the monitor ArrayList
   * 
   * @return the
   */
  public int size() {
    return monitors.size();
  }

  /**
   * Call to get monitor information on a certain monitor.
   * 
   * @param pos the position in the arraylist of the monitor information that you want to get.
   * @return returns the MonitorInfo data for the monitor called for.
   */
  public MonitorInfo get(int pos) {
    if (pos < monitors.size())
      return monitors.get(pos);

    return null;
  }

  /**
   * Set information about this monitor stored in monPos position in the array list.
   * 
   * @param monPos arraylist position of monitor to update
   * @param width the current width the monitor is displaying
   * @param height the current height the monitor is displaying
   * @param rate the current refresh rate the monitor is set to
   */
  public void setInfo(int monPos, String name, int width, int height, int rate) {
    if (monPos < monitors.size()) {
      MonitorInfo info = monitors.get(monPos);
      if (info != null) {
        info.width = width;
        info.height = height;
        info.rate = rate;
        info.name = name;
      }
    }
  }

  /**
   * This function will mark a certain monitor as the primary monitor.
   * 
   * @param monPos the position in the arraylist of which monitor is the primary monitor
   */
  public void setPrimaryMonitor(int monPos) {
    if (monPos < monitors.size()) {
      MonitorInfo info = monitors.get(monPos);
      if (info != null)
        info.primary = true;
    }

  }



}
