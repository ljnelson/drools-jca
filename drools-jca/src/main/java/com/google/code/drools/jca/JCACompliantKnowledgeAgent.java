/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil -*-
 *
 * $Id$
 *
 * Copyright (c) 2010, 2011 Laird Nelson.
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

import java.io.IOException;

import java.net.URL;

import java.util.ArrayList;
import java.util.Collection;

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

import org.drools.io.Resource;
import org.drools.io.ResourceFactory;

import org.drools.io.internal.InternalResource;

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
          this.workManager.scheduleWork(new Work() {
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

  @Override
  public ChangeSet getChangeSet(final Resource r) {    
    ChangeSet changeSet = null;
    final ChangeSet originalChangeSet = super.getChangeSet(r);
    if (originalChangeSet != null) {
      final Collection<Resource> added = originalChangeSet.getResourcesAdded();
      final Collection<Resource> modified = originalChangeSet.getResourcesModified();
      final Collection<Resource> removed = originalChangeSet.getResourcesRemoved();
      
      final Collection<Resource> newAdded = new ArrayList<Resource>();
      final Collection<Resource> newModified = new ArrayList<Resource>();
      final Collection<Resource> newRemoved = new ArrayList<Resource>();

      final Collection<Collection<Resource>> wholeBanana = new ArrayList<Collection<Resource>>();
      wholeBanana.add(added);
      wholeBanana.add(modified);
      wholeBanana.add(removed);

      for (final Collection<Resource> resources : wholeBanana) {
        if (resources != null && !resources.isEmpty()) {
          for (Resource resource : resources) {
            if (resource instanceof InternalResource) {
              InternalResource internalResource = (InternalResource)resource;
              if (internalResource.hasURL()) {
                URL url = null;
                try {
                  url = internalResource.getURL();
                } catch (final IOException boom) {
                  url = null;
                }
                if (url != null) {
                  final String rep = DroolsResourceAdapter.replaceVariables(url.toString());
                  if (rep != null) {
                    try {
                      resource = ResourceFactory.newUrlResource(rep);
                      if (resource instanceof InternalResource) {
                        ((InternalResource)resource).setResourceType(internalResource.getResourceType());
                      }
                    } catch (final IllegalArgumentException badUrl) {
                      // ignore; we tried our best; just use the old resource as-was.
                    }
                  }
                }
              }
            }
            if (resources == added) {
              newAdded.add(resource);
            } else if (resources == modified) {
              newModified.add(resource);
            } else if (resources == removed) {
              newRemoved.add(resource);
            }
          }
        }
      }

      added.clear();
      added.addAll(newAdded);
      assert added == originalChangeSet.getResourcesAdded();

      modified.clear();
      modified.addAll(newModified);
      assert modified == originalChangeSet.getResourcesModified();

      removed.clear();
      removed.addAll(newRemoved);
      assert removed == originalChangeSet.getResourcesRemoved();

    }
    if (changeSet == null) {
      changeSet = originalChangeSet;
    }
    return changeSet;
  }
  
}