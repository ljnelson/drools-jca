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

import java.lang.reflect.Constructor;

import java.util.concurrent.LinkedBlockingQueue;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkManager;

import org.drools.ChangeSet;
import org.drools.SystemEventListener;
import org.drools.SystemEventListenerFactory;

import org.drools.io.impl.ResourceChangeNotifierImpl;

public class JCACompliantResourceChangeNotifier extends ResourceChangeNotifierImpl {

  private WorkManager workManager;

  private final SystemEventListener listener;

  private final LinkedBlockingQueue<ChangeSet> queue;

  private ProcessChangeSet processChangeSet;

  public JCACompliantResourceChangeNotifier() {
    super();
    this.listener = SystemEventListenerFactory.getSystemEventListener();
    this.queue = new LinkedBlockingQueue<ChangeSet>();
  }

  public WorkManager getWorkManager() {
    return this.workManager;
  }

  public void setWorkManager(final WorkManager workManager) {
    this.workManager = workManager;
  }

  @Override
  public void publishChangeSet(final ChangeSet changeSet) {
    try {
      this.listener.debug("ResourceChangeNotifier received ChangeSet notification");
      this.queue.put(changeSet);
    } catch (final InterruptedException e) {
      this.listener.exception(new RuntimeException("ResourceChangeNotifier Exception while adding to notification queue", e));
    }
  }
  
  /**
   * Overrides {@link ResourceChangeNotifierImpl#start()} so that
   * {@link Thread}s are not used.  Instead, this {@link {@link
   * JCACompliantResourceChangeNotifier}'s {@link #getWorkManager()
   * WorkManager} is used.
   */
  @Override
  public void start() {
    final WorkManager workManager = this.getWorkManager();
    if (workManager == null) {
      throw new IllegalStateException("this.getWorkManager() == null");
    }

    try {
      final Constructor<ProcessChangeSet> c = ProcessChangeSet.class.getDeclaredConstructor(LinkedBlockingQueue.class, ResourceChangeNotifierImpl.class, SystemEventListener.class);
      assert c != null;
      c.setAccessible(true);
      this.processChangeSet = c.newInstance(this.queue, this, this.listener);
    } catch (final RuntimeException throwMe) {
      throw throwMe;
    } catch (final Exception everythingElse) {
      throw new RuntimeException(everythingElse);
    }

    assert this.processChangeSet != null;

    try {
      workManager.startWork(new Work() {
          @Override
          public final void run() {
            JCACompliantResourceChangeNotifier.this.processChangeSet.run();
          }

          @Override
          public void release() {
            JCACompliantResourceChangeNotifier.this.stop();
          }
        });
    } catch (final WorkException boom) {
      throw new RuntimeException(boom);
    }
  }

  @Override
  public void stop() {
    if (this.processChangeSet != null) {
      this.processChangeSet.stop();
      this.processChangeSet = null;
    }
  }

}