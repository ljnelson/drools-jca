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

import java.util.Timer;

import javax.resource.spi.work.WorkManager;

import org.drools.io.ResourceChangeNotifier;
import org.drools.io.ResourceChangeScanner;

import org.drools.io.impl.ResourceFactoryServiceImpl;

public class JCACompliantResourceProvider extends ResourceFactoryServiceImpl {

  private final Object lock;

  private JCACompliantResourceChangeNotifier notifier;

  private JCACompliantResourceChangeScanner scanner;

  private WorkManager workManager;

  private Timer timer;

  public JCACompliantResourceProvider() {
    super();
    this.lock = new byte[0];
  }

  public WorkManager getWorkManager() {
    return this.workManager;
  }

  public void setWorkManager(final WorkManager workManager) {
    this.workManager = workManager;
  }

  public Timer getTimer() {
    return this.timer;
  }

  public void setTimer(final Timer timer) {
    this.timer = timer;
  }

  @Override
  public ResourceChangeScanner getResourceChangeScannerService() {
    synchronized (this.lock) {
      if (this.scanner == null) {
        this.scanner = new JCACompliantResourceChangeScanner(this.getTimer());
      }
      return this.scanner;
    }
  }

  @Override
  public ResourceChangeNotifier getResourceChangeNotifierService() {
    synchronized (this.lock) {
      if (this.notifier == null) {
        this.notifier = new JCACompliantResourceChangeNotifier();
        this.notifier.setWorkManager(this.getWorkManager());
      }
      return this.notifier;
    }
  }
  

}