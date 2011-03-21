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

import org.drools.agent.KnowledgeAgent;

import org.drools.command.Command;

import org.drools.event.process.ProcessEventListener;
import org.drools.event.process.ProcessEventManager;

import org.drools.event.rule.AgendaEventListener;
import org.drools.event.rule.WorkingMemoryEventListener;

import org.drools.impl.StatelessKnowledgeSessionImpl;
import org.drools.impl.KnowledgeBaseImpl;

import org.drools.reteoo.ReteooStatefulSession;

import org.drools.runtime.Calendars;
import org.drools.runtime.Channel;
import org.drools.runtime.CommandExecutor;
import org.drools.runtime.Environment;
import org.drools.runtime.ExecutionResults;
import org.drools.runtime.ExitPoint;
import org.drools.runtime.Globals;
import org.drools.runtime.KnowledgeRuntime;
import org.drools.runtime.KnowledgeSessionConfiguration;
import org.drools.runtime.ObjectFilter;

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

import org.drools.runtime.StatelessKnowledgeSession;
import org.drools.runtime.ExecutionResults;
import org.drools.runtime.Globals;

public class StatelessKnowledgeSessionUserConnection extends StatelessKnowledgeSessionImpl implements UserConnection<DroolsManagedConnection> {
  
  private final byte[] creatorLock;

  private StatelessKnowledgeSession delegate;

  private DroolsManagedConnection creator;

  private LazyAssociatableConnectionManager cm;

  private ManagedConnectionFactory mcf;

  private ConnectionRequestInfo cri; 


  /*
   * Constructors.
   */


  protected StatelessKnowledgeSessionUserConnection(final DroolsManagedConnection creator, final StatelessKnowledgeSession delegate, final ConnectionRequestInfo connectionRequestInfo) {
    super(null, extractKnowledgeAgent(delegate), null); // TODO: allow for passing KnowledgeSessionConfiguration
    this.creatorLock = new byte[0];
    this.setCreator(creator);
    this.setDelegate(delegate);
    this.cri = connectionRequestInfo;
  }

  private static KnowledgeAgent extractKnowledgeAgent(final StatelessKnowledgeSession delegate) {
    if (!(delegate instanceof StatelessKnowledgeSessionImpl)) {
      throw new IllegalStateException("!(delegate instanceof StatelessKnowledgeSessionImpl): " + delegate);
    }
    return ((StatelessKnowledgeSessionImpl)delegate).getKnowledgeAgent();
  }


  /*
   * Delegate property.
   */


  protected StatelessKnowledgeSession getDelegate() {
    return this.delegate;
  }

  protected void setDelegate(final StatelessKnowledgeSession delegate) {
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
      final StatelessKnowledgeSession delegate = this.getDelegate();
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
      final StatelessKnowledgeSession delegate = this.getDelegate();
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
    final StatelessKnowledgeSession delegate = this.getDelegate();
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
      final StatelessKnowledgeSession delegate = this.getDelegate();
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
      final StatelessKnowledgeSession delegate = this.getDelegate();
      if (delegate != null) {
        delegate.removeEventListener(l);
      }
      if (!associated) {
        this.close();
      }
    }
  }

  public Collection<WorkingMemoryEventListener> getWorkingMemoryEventListeners() {
    final StatelessKnowledgeSession delegate = this.getDelegate();
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
      final StatelessKnowledgeSession delegate = this.getDelegate();
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
      final StatelessKnowledgeSession delegate = this.getDelegate();
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
    final StatelessKnowledgeSession delegate = this.getDelegate();
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
   * StatelessRuleSession implementation.
   */


  @Override
  public void execute(final Object object) {
    final boolean associated = this.associateConnection();
    final StatelessKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      delegate.execute(object);
    }
    if (!associated) {
      this.close();    
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void execute(final Iterable stuff) {
    final boolean associated = this.associateConnection();
    final StatelessKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      delegate.execute(stuff);
    }
    if (!associated) {
      this.close();
    }
  }

  
  /*
   * StatelessKnowledgeSession implementation.
   */

  @Override
  public Globals getGlobals() {
    final StatelessKnowledgeSession delegate = this.getDelegate();
    if (delegate == null) {
      return null;
    }
    return delegate.getGlobals();
  }

  @Override
  public void setGlobal(final String id, final Object thing) {
    final StatelessKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      delegate.setGlobal(id, thing);
    }
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