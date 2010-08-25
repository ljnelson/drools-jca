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

import java.util.concurrent.LinkedBlockingQueue;

import javax.resource.spi.work.WorkManager;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;

import org.drools.ChangeSet;
import org.drools.KnowledgeBase;
import org.drools.SystemEventListener;
import org.drools.SystemEventListenerFactory;

import org.drools.agent.KnowledgeAgentConfiguration;

import org.drools.agent.impl.KnowledgeAgentImpl;

import org.drools.builder.KnowledgeBuilderConfiguration;

public class JCACompliantKnowledgeAgent extends KnowledgeAgentImpl {

  private LinkedBlockingQueue<ChangeSet> queue;

  private KnowledgeAgentImpl.ChangeSetNotificationDetector changeSetNotificationDetector;

  private SystemEventListener listener;

  private static WorkManager workManager;

  public JCACompliantKnowledgeAgent(final String name, final KnowledgeBase knowledgeBase, final KnowledgeAgentConfiguration configuration, final KnowledgeBuilderConfiguration anotherConfiguration) {
    super(name, knowledgeBase, configuration, anotherConfiguration);
  }

  public static WorkManager getWorkManager() {
    return workManager;
  }

  public static void setWorkManager(final WorkManager wm) {
    workManager = wm;
  }

  @Override
  public void monitorResourceChangeEvents(final boolean monitor) {
    final WorkManager workManager = getWorkManager();
    if (workManager == null) {
      throw new IllegalStateException("getWorkManager() == null");
    }
    if (this.queue == null) {
      this.queue = new LinkedBlockingQueue<ChangeSet>();
    }
    if (this.listener == null) {
      this.listener = SystemEventListenerFactory.getSystemEventListener();
    }
    assert this.listener != null;
    if (monitor) {
      if (this.changeSetNotificationDetector == null) {
        this.changeSetNotificationDetector = new KnowledgeAgentImpl.ChangeSetNotificationDetector(this, this.queue, this.listener);
        try {
          this.workManager.startWork(new Work() {
              @Override
              public final void run() {
                JCACompliantKnowledgeAgent.this.changeSetNotificationDetector.run();
              }
              @Override
              public final void release() {
                // Nothing that we can do.
              }
            });
        } catch (final WorkException wrapMe) {
          throw new RuntimeException(wrapMe);
        }
      }
    } else if (this.changeSetNotificationDetector != null) {
      assert !monitor;
      this.changeSetNotificationDetector.stop();
      this.changeSetNotificationDetector = null;
    }
  }

  @Override
  public void resourcesChanged(final ChangeSet changeSet) {
    if (this.queue == null) {
      this.queue = new LinkedBlockingQueue<ChangeSet>();
    }
    try {
      this.listener.debug("KnowledgeAgent received ChangeSet changed notification");
      this.queue.put(changeSet);
    } catch (final InterruptedException ie) {
      this.listener.exception(new RuntimeException("KnowledgeAgent error while adding ChangeSet notification to queue", ie));
    }
  }
  
}