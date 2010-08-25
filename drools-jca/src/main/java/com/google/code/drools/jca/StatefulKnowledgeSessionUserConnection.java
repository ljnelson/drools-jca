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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.drools.KnowledgeBase;

import org.drools.time.SessionClock;

import org.drools.runtime.KnowledgeRuntime;
import org.drools.runtime.Calendars;
import org.drools.runtime.Channel;
import org.drools.runtime.Globals;
import org.drools.runtime.ExitPoint;
import org.drools.runtime.ObjectFilter;
import org.drools.runtime.Environment;
import org.drools.runtime.StatefulKnowledgeSession;

import org.drools.runtime.process.ProcessRuntime;
import org.drools.runtime.process.ProcessInstance;
import org.drools.runtime.process.WorkItemManager;

import org.drools.runtime.rule.StatefulRuleSession;
import org.drools.runtime.rule.AgendaFilter;
import org.drools.runtime.rule.WorkingMemoryEntryPoint;
import org.drools.runtime.rule.Agenda;
import org.drools.runtime.rule.FactHandle;
import org.drools.runtime.rule.LiveQuery;
import org.drools.runtime.rule.QueryResults;
import org.drools.runtime.rule.ViewChangedEventListener;

import javax.resource.spi.ConnectionRequestInfo;

public class StatefulKnowledgeSessionUserConnection extends AbstractKnowledgeSessionUserConnection<StatefulKnowledgeSession> implements StatefulKnowledgeSession {


  /*
   * Constructors.
   */


  protected StatefulKnowledgeSessionUserConnection(final DroolsManagedConnection creator, final StatefulKnowledgeSession delegate, final ConnectionRequestInfo connectionRequestInfo) {
    super(creator, connectionRequestInfo, delegate);
  }


  /*
   * StatefulRuleSession implementation.
   */

  @Override
  public int fireAllRules() {
    final boolean associated = this.associateConnection();
    final StatefulRuleSession delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.fireAllRules();
    }
    return 0;
  }

  @Override
  public int fireAllRules(final int max) {
    final boolean associated = this.associateConnection();
    final StatefulRuleSession delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.fireAllRules(max);
    }
    return 0;
  }

  @Override
  public int fireAllRules(final AgendaFilter filter) {
    final boolean associated = this.associateConnection();
    final StatefulRuleSession delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.fireAllRules(filter);
    }
    return 0;
  }
  
  @Override
  public void fireUntilHalt() {
    final boolean associated = this.associateConnection();
    final StatefulRuleSession delegate = this.getDelegate();
    if (delegate != null) {
      delegate.fireUntilHalt();
    }
  }

  @Override
  public void fireUntilHalt(final AgendaFilter filter) {
    final boolean associated = this.associateConnection();
    final StatefulRuleSession delegate = this.getDelegate();
    if (delegate != null) {
      delegate.fireUntilHalt(filter);
    }
  }
  

  /*
   * KnowledgeRuntime implementation.
   */

  
  @Override
  public Calendars getCalendars() {
    final boolean associated = this.associateConnection();
    final KnowledgeRuntime delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getCalendars();
    }
    return null;
  }

  @Override
  public Map<String, Channel> getChannels() {
    final boolean associated = this.associateConnection();
    final KnowledgeRuntime delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getChannels();
    }
    return null;
  }

  @Override
  public Globals getGlobals() {
    final boolean associated = this.associateConnection();
    final KnowledgeRuntime delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getGlobals();
    }
    return null;
  }

  @Override
  public Object getGlobal(final String global) {
    final boolean associated = this.associateConnection();
    final KnowledgeRuntime delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getGlobal(global);
    }
    return null;
  }

  @Override
  public void setGlobal(final String key, final Object value) {
    final boolean associated = this.associateConnection();
    final KnowledgeRuntime delegate = this.getDelegate();
    if (delegate != null) {
      delegate.setGlobal(key, value);
    }
  }

  @Override
  public Environment getEnvironment() {
    final boolean associated = this.associateConnection();
    final KnowledgeRuntime delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getEnvironment();
    }
    return null;
  }

  @Override
  public KnowledgeBase getKnowledgeBase() {
    final boolean associated = this.associateConnection();
    final KnowledgeRuntime delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getKnowledgeBase();
    }
    return null;
  }

  @Override
  public void registerChannel(final String name, final Channel channel) {
    final boolean associated = this.associateConnection();
    final KnowledgeRuntime delegate = this.getDelegate();
    if (delegate != null) {
      delegate.registerChannel(name, channel);
    }
  }

  @Override
  public void registerExitPoint(final String name, final ExitPoint exitPoint) {
    final boolean associated = this.associateConnection();
    final KnowledgeRuntime delegate = this.getDelegate();
    if (delegate != null) {
      delegate.registerExitPoint(name, exitPoint);
    }
  }

  @Override
  public void unregisterChannel(final String name) {
    final boolean associated = this.associateConnection();
    final KnowledgeRuntime delegate = this.getDelegate();
    if (delegate != null) {
      delegate.unregisterChannel(name);
    }
  }

  @Override
  public void unregisterExitPoint(final String name) {
    final boolean associated = this.associateConnection();
    final KnowledgeRuntime delegate = this.getDelegate();
    if (delegate != null) {
      delegate.unregisterExitPoint(name);
    }
  }

  @Override
  public <T extends SessionClock> T getSessionClock() {
    final boolean associated = this.associateConnection();
    final KnowledgeRuntime delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.<T>getSessionClock(); // see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6302954 for this syntax
    }
    return null;
  }


  /*
   * WorkingMemory implementation.
   */


  @Override
  public Agenda getAgenda() {
    final boolean associated = this.associateConnection();
    final KnowledgeRuntime delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getAgenda();
    }
    return null;
  }

  @Override
  public QueryResults getQueryResults(final String query) {
    final boolean associated = this.associateConnection();
    final KnowledgeRuntime delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getQueryResults(query);
    }
    return null;
  }

  @Override
  public QueryResults getQueryResults(final String query, final Object[] stuff) {
    final boolean associated = this.associateConnection();
    final KnowledgeRuntime delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getQueryResults(query, stuff);
    }
    return null;
  }

  @Override
  public WorkingMemoryEntryPoint getWorkingMemoryEntryPoint(final String name) {
    final boolean associated = this.associateConnection();
    final KnowledgeRuntime delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getWorkingMemoryEntryPoint(name);
    }
    return null;
  } 

  @Override
  public Collection<? extends WorkingMemoryEntryPoint> getWorkingMemoryEntryPoints() {
    final boolean associated = this.associateConnection();
    final KnowledgeRuntime delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getWorkingMemoryEntryPoints();
    }
    return null;
  } 

  @Override
  public void halt() {
    final boolean associated = this.associateConnection();
    final KnowledgeRuntime delegate = this.getDelegate();
    if (delegate != null) {
      delegate.halt();
    }
  }

  @Override
  public LiveQuery openLiveQuery(final String query, final Object[] arguments, final ViewChangedEventListener listener) {
    final boolean associated = this.associateConnection();
    final KnowledgeRuntime delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.openLiveQuery(query, arguments, listener);
    }
    return null;
  }
  
  /*
   * WorkingMemoryEntryPoint implementation.
   */

  @Override
  public String getEntryPointId() {
    final boolean associated = this.associateConnection();
    final WorkingMemoryEntryPoint delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getEntryPointId();
    }
    return null;
  } 

  @Override
  public long getFactCount() {
    final boolean associated = this.associateConnection();
    final WorkingMemoryEntryPoint delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getFactCount();
    }
    return 0L;
  } 

  @Override
  public FactHandle getFactHandle(final Object object) {
    final boolean associated = this.associateConnection();
    final WorkingMemoryEntryPoint delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getFactHandle(object);
    }
    return null;
  }

  @Override
  public <T extends FactHandle> Collection<T> getFactHandles() {
    final boolean associated = this.associateConnection();
    final WorkingMemoryEntryPoint delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getFactHandles();
    }
    return Collections.emptyList();
  }

  @Override
  public <T extends FactHandle> Collection<T> getFactHandles(final ObjectFilter filter) {
    final boolean associated = this.associateConnection();
    final WorkingMemoryEntryPoint delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getFactHandles(filter);
    }
    return Collections.emptyList();
  }

  @Override
  public Object getObject(final FactHandle handle) {
    final boolean associated = this.associateConnection();
    final WorkingMemoryEntryPoint delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getObject(handle);
    }
    return null;
  }

  @Override
  public Collection<Object> getObjects() {
    final boolean associated = this.associateConnection();
    final WorkingMemoryEntryPoint delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getObjects();
    }
    return Collections.emptyList();
  }

  @Override
  public Collection<Object> getObjects(final ObjectFilter filter) {
    final boolean associated = this.associateConnection();
    final WorkingMemoryEntryPoint delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getObjects(filter);
    }
    return Collections.emptyList();
  }
  
  @Override
  public FactHandle insert(final Object object) {
    final boolean associated = this.associateConnection();
    final WorkingMemoryEntryPoint delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.insert(object);
    }
    return null;
  }

  @Override
  public void retract(final FactHandle handle) {
    final boolean associated = this.associateConnection();
    final WorkingMemoryEntryPoint delegate = this.getDelegate();
    if (delegate != null) {
      delegate.retract(handle);
    }
  }

  @Override
  public void update(final FactHandle handle, final Object object) {
    final boolean associated = this.associateConnection();
    final WorkingMemoryEntryPoint delegate = this.getDelegate();
    if (delegate != null) {
      delegate.update(handle, object);
    }
  }

  
  /*
   * ProcessRuntime implementation.
   */


  @Override
  public void abortProcessInstance(final long id) {
    final boolean associated = this.associateConnection();
    final ProcessRuntime delegate = this.getDelegate();
    if (delegate != null) {
      delegate.abortProcessInstance(id);
    }
  }

  @Override
  public ProcessInstance getProcessInstance(final long id) {
    final boolean associated = this.associateConnection();
    final ProcessRuntime delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getProcessInstance(id);
    }
    return null;
  }
  
  @Override
  public Collection<ProcessInstance> getProcessInstances() {
    final boolean associated = this.associateConnection();
    final ProcessRuntime delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getProcessInstances();
    }
    return Collections.emptySet();
  }

  @Override
  public WorkItemManager getWorkItemManager() {
    final boolean associated = this.associateConnection();
    final ProcessRuntime delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getWorkItemManager();
    }
    return null;
  }

  @Override
  public void signalEvent(final String type, final Object event) {
    final boolean associated = this.associateConnection();
    final ProcessRuntime delegate = this.getDelegate();
    if (delegate != null) {
      delegate.signalEvent(type, event);
    }
  }

  @Override
  public void signalEvent(final String type, final Object event, final long processId) {
    final boolean associated = this.associateConnection();
    final ProcessRuntime delegate = this.getDelegate();
    if (delegate != null) {
      delegate.signalEvent(type, event, processId);
    }
  }

  @Override
  public ProcessInstance startProcess(final String processId) {
    final boolean associated = this.associateConnection();
    final ProcessRuntime delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.startProcess(processId);
    }
    return null;
  }

  @Override
  public ProcessInstance startProcess(final String processId, final Map<String, Object> parameters) {
    final boolean associated = this.associateConnection();
    final ProcessRuntime delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.startProcess(processId, parameters);
    }
    return null;
  }


  /*
   * StatefulKnowledgeSession implementation.
   */

  
  @Override
  public void dispose() {
    this.close();
  }

  @Override
  public int getId() {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getId();
    }
    return Integer.MIN_VALUE;
  }

  @Override
  public void close() {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      delegate.dispose();
    }
    super.close();
  }

}