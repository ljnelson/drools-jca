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

import java.io.Closeable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.resource.ResourceException;

import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LazyAssociatableConnectionManager;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;

import org.drools.KnowledgeBase;
import org.drools.RuntimeDroolsException;
import org.drools.SessionConfiguration;
import org.drools.StatefulSession;

import org.drools.command.Command;

import org.drools.event.process.ProcessEventListener;
import org.drools.event.process.ProcessEventManager;

import org.drools.event.rule.AgendaEventListener;
import org.drools.event.rule.WorkingMemoryEventListener;

import org.drools.impl.StatefulKnowledgeSessionImpl;
import org.drools.impl.KnowledgeBaseImpl;

import org.drools.reteoo.ReteooStatefulSession;

import org.drools.runtime.Calendars;
import org.drools.runtime.Channel;
import org.drools.runtime.CommandExecutor;
import org.drools.runtime.Environment;
import org.drools.runtime.ExecutionResults;
import org.drools.runtime.Globals;
import org.drools.runtime.KnowledgeRuntime;
import org.drools.runtime.KnowledgeSessionConfiguration;
import org.drools.runtime.ObjectFilter;
import org.drools.runtime.StatefulKnowledgeSession;

import org.drools.runtime.process.ProcessInstance;
import org.drools.runtime.process.ProcessRuntime;
import org.drools.runtime.process.WorkItemManager;

import org.drools.runtime.rule.Agenda;
import org.drools.runtime.rule.AgendaFilter;
import org.drools.runtime.rule.FactHandle;
import org.drools.runtime.rule.LiveQuery;
import org.drools.runtime.rule.QueryResults;
import org.drools.runtime.rule.StatefulRuleSession;
import org.drools.runtime.rule.ViewChangedEventListener;
import org.drools.runtime.rule.WorkingMemoryEntryPoint;

import org.drools.time.SessionClock;

public class StatefulKnowledgeSessionUserConnection extends StatefulKnowledgeSessionImpl implements UserConnection<DroolsManagedConnection> {

  /**
   * A serial version identifier uniquely identifying the version of
   * this class.  See the <a
   * href="http://download.oracle.com/javase/6/docs/api/java/io/Serializable.html">documentation
   * for the {@code Serializable} class</a> for details.
   */
  private static final long serialVersionUID = 1L;

  private DroolsManagedConnection creator;
  
  private final Object creatorLock;

  private LazyAssociatableConnectionManager cm;

  private ManagedConnectionFactory mcf;

  private ConnectionRequestInfo cri; 

  private StatefulKnowledgeSession delegate;


  /*
   * Constructors.
   */


  protected StatefulKnowledgeSessionUserConnection(final DroolsManagedConnection creator, final StatefulKnowledgeSession delegate, final ConnectionRequestInfo connectionRequestInfo) {
    super(extractReteooStatefulSession(delegate), delegate.getKnowledgeBase());
    this.creatorLock = new byte[0];
    this.setCreator(creator);
    this.setDelegate(delegate);
    this.cri = connectionRequestInfo;
  }

  /**
   * Returns a {@link ReteooStatefulSession} that is extracted from
   * the supplied {@link StatefulKnowledgeSession}.
   *
   * <h3>Design Notes</h3>
   *
   * <p>Drools likes to pretend that it is built around interfaces and
   * implementations, but in reality throughout the codebase a
   * particular implementation of an interface is assumed.  For
   * example, attempts to serialize a {@link StatefulKnowledgeSession}
   * will fail unless that {@link StatefulKnowledgeSession} is, in
   * fact, a descendant of the {@link StatefulKnowledgeSessionImpl}
   * class.</p>
   *
   * <p>This means, among other things that <em>this</em> class must
   * inherit from {@link StatefulKnowledgeSessionImpl}.  To do
   * <em>that</em>, we have to make sure that inside this class'
   * constructor we can come up with a {@link ReteooStatefulSession}
   * since it is one of the required parameters of the {@linkplain
   * StatefulKnowledgeSessionImpl#StatefulKnowledgeSessionImpl(ReteooStatefulSession,
   * KnowledgeBase) <tt>StatefulKnowledgeSessionImpl</tt>
   * constructor}.</p>
   *
   * <p>This method hides the gory details of extracting such an
   * instance from an ordinary {@link StatefulKnowledgeSession},
   * provided that the supplied {@link StatefulKnowledgeSession}
   * offers up a {@link KnowledgeBaseImpl} as the return value of its
   * {@link StatefulKnowledgeSession#getKnowledgeBase()} method.</p>
   *
   * @param delegate the {@link StatefulKnowledgeSession} from which
   * to perform the extraction.  The value of this parameter must not
   * be {@code null}.
   *
   * @return a {@link ReteooStatefulSession}.  This method never
   * returns {@code null}.
   *
   * @exception IllegalArgumentException if {@code delegate} is {@code null}
   *
   * @exception IllegalStateException if {@code
   * delegate.getKnowledgeBase()} is not an instance of {@link
   * KnowledgeBaseImpl}
   *
   * @exception ClassCastException if the innards of Drools change
   * such that all these implementation assumptions (that Drools
   * itself makes) change
   */
  private static final ReteooStatefulSession extractReteooStatefulSession(final StatefulKnowledgeSession delegate) {
    if (delegate == null) {
      throw new IllegalArgumentException("delegate", new NullPointerException("delegate == null"));
    }
    final KnowledgeBase kb = delegate.getKnowledgeBase();
    if (kb == null) {
      throw new IllegalStateException("delegate.getKnowledgeBase() == null");
    }
    assert kb instanceof KnowledgeBaseImpl;
    final StatefulSession statefulSession = ((KnowledgeBaseImpl)kb).ruleBase.newStatefulSession((SessionConfiguration)delegate.getSessionConfiguration(), delegate.getEnvironment());
    assert statefulSession instanceof ReteooStatefulSession;
    return (ReteooStatefulSession)statefulSession;
  }


  /*
   * Delegate property.
   */


  protected StatefulKnowledgeSession getDelegate() {
    return this.delegate;
  }

  protected void setDelegate(final StatefulKnowledgeSession delegate) {
    this.delegate = delegate;
  }


  /*
   * Creator property.
   */

  
  @Override
  public DroolsManagedConnection getCreator() {
    synchronized (this.creatorLock) {
      return this.creator;
    }
  }

  @Override
  public final void setCreator(final DroolsManagedConnection creator) {
    synchronized (this.creatorLock) {
      this.creator = creator;
      if (creator != null) {
        this.mcf = creator.getCreator();
      }
    }
  }


  /*
   * ConnectionRequestInfo property.
   */


  public ConnectionRequestInfo getConnectionRequestInfo() {
    return this.cri;
  }


  /*
   * LazyAssociatableConnectionManager property.  The J2EE Connector
   * Architecture 1.5 specification, figure 7-15, implies this should be a legal
   * reference to hold onto.
   */


  public LazyAssociatableConnectionManager getLazyAssociatableConnectionManager() {
    return this.cm;
  }

  public void setLazyAssociatableConnectionManager(final LazyAssociatableConnectionManager cm) {
    this.cm = cm;
  }
  

  protected final boolean associateConnection() {
    final LazyAssociatableConnectionManager cm = this.getLazyAssociatableConnectionManager();
    if (cm != null) {
      synchronized (this.creatorLock) {
        try {
          cm.associateConnection(this, this.mcf, this.getConnectionRequestInfo());
        } catch (final ResourceException kaboom) {
          throw new RuntimeDroolsException(kaboom);
        }
      }
      return true;
    }
    return false;
  }


  /*
   * CommandExecutor implementation.
   */


  @SuppressWarnings("unchecked")
  @Override
  public <T> T execute(final Command<T> command) {
    T returnValue = null;
    final boolean associated = this.associateConnection();
    final CommandExecutor delegate = this.getDelegate();
    if (delegate != null) {
      returnValue = delegate.execute(command);
    }
    if (!associated) {
      this.close();
    }
    return returnValue;
  }


  /*
   * WorkingMemoryEventManager implementation.
   */


  @Override
  public void addEventListener(final AgendaEventListener l) {
    if (l != null) {
      final boolean associated = this.associateConnection();
      final StatefulKnowledgeSession delegate = this.getDelegate();
      if (delegate != null) {
        delegate.addEventListener(l);
      }
      if (!associated) {
        this.close();
      }
    }
  }

  @Override
  public void removeEventListener(final AgendaEventListener l) {
    if (l != null) {
      final boolean associated = this.associateConnection();
      final StatefulKnowledgeSession delegate = this.getDelegate();
      if (delegate != null) {
        delegate.removeEventListener(l);
      }
      if (!associated) {
        this.close();
      }
    }
  }

  public Collection<AgendaEventListener> getAgendaEventListeners() {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate == null) {
      return Collections.emptySet();
    }    
    final Collection<AgendaEventListener> returnValue = delegate.getAgendaEventListeners();
    if (!associated) {
      this.close();
    }
    return returnValue;
  }

  @Override
  public void addEventListener(final WorkingMemoryEventListener l) {
    if (l != null) {
      final boolean associated = this.associateConnection();
      final StatefulKnowledgeSession delegate = this.getDelegate();
      if (delegate != null) {
        delegate.addEventListener(l);
      }
      if (!associated) {
        this.close();
      }
    }
  }

  @Override
  public void removeEventListener(final WorkingMemoryEventListener l) {
    if (l != null) {
      final boolean associated = this.associateConnection();
      final StatefulKnowledgeSession delegate = this.getDelegate();
      if (delegate != null) {
        delegate.removeEventListener(l);
      }
      if (!associated) {
        this.close();
      }
    }
  }

  public Collection<WorkingMemoryEventListener> getWorkingMemoryEventListeners() {
    final StatefulKnowledgeSession delegate = this.getDelegate();
    final boolean associated = this.associateConnection();
    if (delegate == null) {
      return Collections.emptySet();
    }
    final Collection<WorkingMemoryEventListener> returnValue = delegate.getWorkingMemoryEventListeners();
    if (!associated) {
      this.close();
    }
    return returnValue;
  }


  /*
   * ProcessEventManager implementation.
   */


  @Override
  public void addEventListener(final ProcessEventListener l) {
    if (l != null) {
      final boolean associated = this.associateConnection();
      final StatefulKnowledgeSession delegate = this.getDelegate();
      if (delegate != null) {
        delegate.addEventListener(l);
      }
      if (!associated) {
        this.close();
      }
    }
  }

  @Override
  public void removeEventListener(final ProcessEventListener l) {
    if (l != null) {
      final boolean associated = this.associateConnection();
      final StatefulKnowledgeSession delegate = this.getDelegate();
      if (delegate != null) {
        delegate.removeEventListener(l);
      }
      if (!associated) {
        this.close();
      }
    }
  }

  public Collection<ProcessEventListener> getProcessEventListeners() {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate == null) {      
      return Collections.emptySet();
    }
    final Collection<ProcessEventListener> returnValue = delegate.getProcessEventListeners();
    if (!associated) {
      this.close();
    }
    return returnValue;
  }

  /*
   * StatefulRuleSession implementation.
   */

  @Override
  public int fireAllRules() {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.fireAllRules();
    }
    return 0;
  }

  @Override
  public int fireAllRules(final int max) {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.fireAllRules(max);
    }
    return 0;
  }

  @Override
  public int fireAllRules(final AgendaFilter filter) {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.fireAllRules(filter);
    }
    return 0;
  }
  
  @Override
  public void fireUntilHalt() {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      delegate.fireUntilHalt();
    }
  }

  @Override
  public void fireUntilHalt(final AgendaFilter filter) {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
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
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getCalendars();
    }
    return null;
  }

  @Override
  public Map<String, Channel> getChannels() {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getChannels();
    }
    return null;
  }

  @Override
  public Globals getGlobals() {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getGlobals();
    }
    return null;
  }

  @Override
  public Object getGlobal(final String global) {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getGlobal(global);
    }
    return null;
  }

  @Override
  public void setGlobal(final String key, final Object value) {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      delegate.setGlobal(key, value);
    }
  }

  @Override
  public Environment getEnvironment() {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getEnvironment();
    }
    return null;
  }

  @Override
  public KnowledgeBase getKnowledgeBase() {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getKnowledgeBase();
    }
    return null;
  }

  @Override
  public void registerChannel(final String name, final Channel channel) {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      delegate.registerChannel(name, channel);
    }
  }

  @Deprecated
  @Override
  public void registerExitPoint(final String name, @Deprecated final org.drools.runtime.ExitPoint exitPoint) {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      delegate.registerExitPoint(name, exitPoint);
    }
  }

  @Override
  public void unregisterChannel(final String name) {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      delegate.unregisterChannel(name);
    }
  }

  @Deprecated
  @Override
  public void unregisterExitPoint(final String name) {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      delegate.unregisterExitPoint(name);
    }
  }

  @Override
  public <T extends SessionClock> T getSessionClock() {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.<T>getSessionClock(); // see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6302954 for this syntax
    }
    return null;
  }

  @Override
  public KnowledgeSessionConfiguration getSessionConfiguration() {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getSessionConfiguration();
    }
    return null;
  }


  /*
   * WorkingMemory implementation.
   */


  @Override
  public Agenda getAgenda() {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getAgenda();
    }
    return null;
  }

  @Override
  public QueryResults getQueryResults(final String query, final Object... arguments) {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getQueryResults(query, arguments);
    }
    return null;
  }

  @Override
  public WorkingMemoryEntryPoint getWorkingMemoryEntryPoint(final String name) {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getWorkingMemoryEntryPoint(name);
    }
    return null;
  } 

  @Override
  public Collection<? extends WorkingMemoryEntryPoint> getWorkingMemoryEntryPoints() {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getWorkingMemoryEntryPoints();
    }
    return null;
  } 

  @Override
  public void halt() {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      delegate.halt();
    }
  }

  @Override
  public LiveQuery openLiveQuery(final String query, final Object[] arguments, final ViewChangedEventListener listener) {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
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
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getEntryPointId();
    }
    return null;
  } 

  @Override
  public long getFactCount() {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getFactCount();
    }
    return 0L;
  } 

  @Override
  public FactHandle getFactHandle(final Object object) {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getFactHandle(object);
    }
    return null;
  }

  @Override
  public <T extends FactHandle> Collection<T> getFactHandles() {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getFactHandles();
    }
    return Collections.emptyList();
  }

  @Override
  public <T extends FactHandle> Collection<T> getFactHandles(final ObjectFilter filter) {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getFactHandles(filter);
    }
    return Collections.emptyList();
  }

  @Override
  public Object getObject(final FactHandle handle) {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getObject(handle);
    }
    return null;
  }

  @Override
  public Collection<Object> getObjects() {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getObjects();
    }
    return Collections.emptyList();
  }

  @Override
  public Collection<Object> getObjects(final ObjectFilter filter) {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getObjects(filter);
    }
    return Collections.emptyList();
  }
  
  @Override
  public FactHandle insert(final Object object) {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.insert(object);
    }
    return null;
  }

  @Override
  public void retract(final FactHandle handle) {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      delegate.retract(handle);
    }
  }

  @Override
  public void update(final FactHandle handle, final Object object) {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
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
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      delegate.abortProcessInstance(id);
    }
  }

  @Override
  public ProcessInstance getProcessInstance(final long id) {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getProcessInstance(id);
    }
    return null;
  }
  
  @Override
  public Collection<ProcessInstance> getProcessInstances() {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getProcessInstances();
    }
    return Collections.emptySet();
  }

  @Override
  public WorkItemManager getWorkItemManager() {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.getWorkItemManager();
    }
    return null;
  }

  @Override
  public void signalEvent(final String type, final Object event) {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      delegate.signalEvent(type, event);
    }
  }

  @Override
  public void signalEvent(final String type, final Object event, final long processId) {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      delegate.signalEvent(type, event, processId);
    }
  }

  @Override
  public ProcessInstance startProcess(final String processId) {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      return delegate.startProcess(processId);
    }
    return null;
  }

  @Override
  public ProcessInstance startProcess(final String processId, final Map<String, Object> parameters) {
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
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
    final boolean associated = this.associateConnection();
    final StatefulKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      delegate.dispose();
    }
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


  /*
   * Closeable implementation.
   */


  @Override
  public void close() {
    /*
      The [c]onnection [handle] instance delegates the [resource-adapter
      specific] close method to [its] associated ManagedConnection instance. The
      delegation happens through an association between ManagedConnection
      instance and the corresponding connection handle Connection instance. The
      mechanism by which this association is achieved is specific to the
      implementation of a resource adapter.
    */
    synchronized (this.creatorLock) {
      final DroolsManagedConnection creator = this.getCreator();
      if (creator != null) {
        creator.closeAndDetach(this);
      }
      this.setCreator(null);
    }
  }

}