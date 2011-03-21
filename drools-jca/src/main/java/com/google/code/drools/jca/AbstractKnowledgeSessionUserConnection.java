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

import javax.resource.ResourceException;

import javax.resource.spi.LazyAssociatableConnectionManager;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ConnectionRequestInfo;

import org.drools.RuntimeDroolsException;

import org.drools.command.Command;

import org.drools.runtime.CommandExecutor;
import org.drools.runtime.ExecutionResults;

import org.drools.event.process.ProcessEventListener;
import org.drools.event.process.ProcessEventManager;

import org.drools.event.rule.AgendaEventListener;
import org.drools.event.rule.WorkingMemoryEventListener;
import org.drools.event.rule.WorkingMemoryEventManager;

public abstract class AbstractKnowledgeSessionUserConnection<T extends CommandExecutor & ProcessEventManager & WorkingMemoryEventManager> implements Closeable, WorkingMemoryEventManager, ProcessEventManager, CommandExecutor, UserConnection<DroolsManagedConnection> {

  private DroolsManagedConnection creator;
  
  private final Object creatorLock;

  private LazyAssociatableConnectionManager cm;

  private ManagedConnectionFactory mcf;

  private ConnectionRequestInfo cri; 

  private T delegate;
  
  /*
   * Constructors.
   */


  protected AbstractKnowledgeSessionUserConnection(final DroolsManagedConnection creator, final ConnectionRequestInfo cri, final T delegate) {
    super();
    this.creatorLock = new byte[0];
    this.setCreator(creator);
    this.setDelegate(delegate);
  }


  /*
   * Delegate property.
   */


  protected T getDelegate() {
    return this.delegate;
  }

  protected void setDelegate(final T delegate) {
    this.delegate = delegate;
  }


  /*
   * Creator property.
   */


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
      final T delegate = this.getDelegate();
      if (delegate != null) {
        delegate.addEventListener(l);
      }
    }
  }

  @Override
  public void removeEventListener(final AgendaEventListener l) {
    if (l != null) {
      final T delegate = this.getDelegate();
      if (delegate != null) {
        delegate.removeEventListener(l);
      }
    }
  }

  public Collection<AgendaEventListener> getAgendaEventListeners() {
    final T delegate = this.getDelegate();
    if (delegate == null) {
      return Collections.emptySet();
    }
    return delegate.getAgendaEventListeners();
  }

  @Override
  public void addEventListener(final WorkingMemoryEventListener l) {
    if (l != null) {
      final T delegate = this.getDelegate();
      if (delegate != null) {
        delegate.addEventListener(l);
      }
    }
  }

  @Override
  public void removeEventListener(final WorkingMemoryEventListener l) {
    if (l != null) {
      final T delegate = this.getDelegate();
      if (delegate != null) {
        delegate.removeEventListener(l);
      }
    }
  }

  public Collection<WorkingMemoryEventListener> getWorkingMemoryEventListeners() {
    final T delegate = this.getDelegate();
    if (delegate == null) {
      return Collections.emptySet();
    }
    return delegate.getWorkingMemoryEventListeners();
  }


  /*
   * ProcessEventManager implementation.
   */


  @Override
  public void addEventListener(final ProcessEventListener l) {
    if (l != null) {
      final T delegate = this.getDelegate();
      if (delegate != null) {
        delegate.addEventListener(l);
      }
    }
  }

  @Override
  public void removeEventListener(final ProcessEventListener l) {
    if (l != null) {
      final T delegate = this.getDelegate();
      if (delegate != null) {
        delegate.removeEventListener(l);
      }
    }
  }

  public Collection<ProcessEventListener> getProcessEventListeners() {
    final T delegate = this.getDelegate();
    if (delegate == null) {
      return Collections.emptySet();
    }
    return delegate.getProcessEventListeners();
  }
  

}