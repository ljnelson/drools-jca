/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil -*-
 *
 * $Id$
 *
 * Copyright (c) 2010 Laird Nelson.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense and/or sell 
 * copies of the Software, and to permit persons to whom the Software is 
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 * 
 * THIS SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * The original copy of this license is available at
 * http://www.opensource.org/license/mit-license.html.
 */
package com.google.code.drools.jca;
// This package is deliberate.  The ProcessChangeSet constructor invoked as part
// of the start() method is package-level access protected.
// package org.drools.io.impl;

import java.util.Timer;
import java.util.TimerTask;

import org.drools.SystemEventListenerFactory;
import org.drools.SystemEventListener;

import org.drools.io.Resource;
import org.drools.io.ResourceChangeScannerConfiguration;
import org.drools.io.ResourceChangeNotifier;

import org.drools.io.impl.ResourceChangeScannerImpl;

public class JCACompliantResourceChangeScanner extends ResourceChangeScannerImpl {

  private Timer timer;

  private int interval;

  private TimerTask task;

  private final Object taskLock;

  private SystemEventListener listener;

  public JCACompliantResourceChangeScanner() {
    super();
    this.taskLock = new byte[0];
    this.setSystemEventListener(SystemEventListenerFactory.getSystemEventListener());
  }

  public JCACompliantResourceChangeScanner(final Timer timer) {
    this();
    this.setTimer(timer);
  }

  @Override
  public void setSystemEventListener(final SystemEventListener listener) {
    super.setSystemEventListener(listener);
    this.listener = listener;
  }

  public SystemEventListener getSystemEventListener() {
    return this.listener;
  }

  public Timer getTimer() {
    return this.timer;
  }

  public void setTimer(final Timer timer) {
    this.timer = timer;    
  }

  @Override
  public void start() {
    final long interval = Math.max(0, this.getInterval());
    final Timer timer = this.getTimer();
    if (timer == null) {
      throw new IllegalStateException(JCACompliantResourceChangeScanner.class.getName() + ".getTimer() == null");
    }
    synchronized (this.taskLock) {
      this.stop();
      this.task = new ScannerScheduler();
      timer.schedule(this.task, 0L, interval * 1000L);
    }
    final SystemEventListener listener = this.getSystemEventListener();
    if (listener != null) {
      listener.info("ResourceChangeNotification scanner has started");
    }
  }

  @Override
  public void stop() {
    synchronized (this.taskLock) {
      if (this.task != null) {
        this.task.cancel();
        this.task = null;
      }
    }
  }

  public void restart() {
    if (this.taskLock != null) {
      synchronized (this.taskLock) {
        if (this.task != null) {
          this.start();
        }
      }
    }
  }

  @Override
  public int getInterval() {
    return this.interval;
  }

  @Override
  public void setInterval(final int interval) {
    this.interval = interval;
    this.restart();
  }

  @Override
  public void configure(final ResourceChangeScannerConfiguration configuration) {
    if (configuration == null) {
      this.setInterval(60);
    } else {
      String s = configuration.getProperty("drools.resource.scanner.interval");
      if (s == null) {
        this.setInterval(60);
      } else {
        s = s.trim();
        if (s.length() <= 0) {
          this.setInterval(60);
        } else {
          this.setInterval(Integer.parseInt(s));
        }
      }
    }
    final SystemEventListener listener = this.getSystemEventListener();
    if (listener != null) {
      listener.info("ResourceChangeScanner reconfigured with interval=" + this.getInterval());
    }
    this.restart();
  }

  public class ScannerScheduler extends TimerTask {
    
    public ScannerScheduler() {
      super();
    }

    @Override
    public void run() {
      JCACompliantResourceChangeScanner.this.scan();
      final SystemEventListener listener = JCACompliantResourceChangeScanner.this.getSystemEventListener();
      if (listener != null) {
        listener.debug("ResourceChangeScanner thread is waiting for " + JCACompliantResourceChangeScanner.this.getInterval() + " seconds.");
      }
    }

  }

}