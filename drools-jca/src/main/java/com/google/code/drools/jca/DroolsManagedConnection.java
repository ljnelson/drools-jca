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

import java.io.PrintWriter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.LinkedHashSet;

import java.util.concurrent.ExecutionException;

import javax.resource.ResourceException;
import javax.resource.NotSupportedException;

import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.DissociatableManagedConnection;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.resource.spi.ResourceAdapterInternalException;

import javax.security.auth.Subject;

import javax.transaction.xa.XAResource;

import org.drools.KnowledgeBase;

import org.drools.agent.KnowledgeAgent;

public class DroolsManagedConnection implements ManagedConnection, DissociatableManagedConnection {

  private transient PrintWriter logWriter;

  private DroolsManagedConnectionMetaData metaData;

  private final Set<ConnectionEventListener> listeners;

  private final DroolsManagedConnectionFactory creator;

  private final ConnectionRequestInfo connectionRequestInfo;

  private final Set<UserConnection> handles;

  public DroolsManagedConnection(final DroolsManagedConnectionFactory creator, final ConnectionRequestInfo connectionRequestInfo) {
    super();
    this.listeners = new LinkedHashSet<ConnectionEventListener>();
    this.handles = new HashSet<UserConnection>();
    this.creator = creator;
    this.connectionRequestInfo = connectionRequestInfo;
    if (creator == null) {
      throw new IllegalArgumentException("creator", new NullPointerException("creator == null"));
    }
  }

  public KnowledgeAgent getKnowledgeAgent() throws InterruptedException, ExecutionException {
    final DroolsManagedConnectionFactory creator = this.getCreator();
    if (creator != null) {
      return creator.getKnowledgeAgent();
    }
    return null;
  }

  protected final DroolsManagedConnectionFactory getCreator() {
    return this.creator;
  }

  public ConnectionRequestInfo getConnectionRequestInfo() {
    return this.connectionRequestInfo;
  }

  protected void fireConnectionClosedEvent(final UserConnection connectionHandle) {
    final ConnectionEventListener[] ls;
    synchronized (this.listeners) {
      if (this.listeners.isEmpty()) {
        ls = null;
        return;
      } else {
        ls = this.listeners.toArray(new ConnectionEventListener[this.listeners.size()]);
      }
    }
    assert ls != null;
    final ConnectionEvent event = new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED);
    event.setConnectionHandle(connectionHandle);
    for (final ConnectionEventListener l : ls) {
      if (l != null) {
        l.connectionClosed(event);
      }
    }
  }

  @Override
  public void addConnectionEventListener(final ConnectionEventListener listener) {
    if (listener != null) {
      synchronized (this.listeners) {
        this.listeners.add(listener);
      }
    }
  }

  @Override
  public void removeConnectionEventListener(final ConnectionEventListener listener) {
    if (listener != null) {
      synchronized (this.listeners) {
        if (!this.listeners.isEmpty()) {
          this.listeners.remove(listener);
        }
      }
    }
  }

  @Override
  public final void associateConnection(final Object connection) throws ResourceException {

    /*
      The container uses the associateConnection method to change the
      association of an application-level connection handle with a
      ManagedConnection instance. The container finds the right
      ManagedConnection instance, depending on the connection sharing
      scope, and calls the associateConnection method. To achieve
      this, the container needs to keep track of connection handles
      acquired by component instances and ManagedConnection instances
      using an implementation-specific mechanism.

      The associateConnection method implementation for a
      ManagedConnection should dissociate the connection handle passed
      as a parameter from its currently associated ManagedConnection
      and associate the new connection handle with itself.

      Note that the switching of connection associations must happen
      only for connection handles and ManagedConnection instances that
      correspond to the same ManagedConnectionFactory instance. The
      container should enforce this restriction in an
      implementation-specific manner. If a container cannot enforce
      the restriction, the container should not use the connection
      association mechanism.
    */

    if (connection instanceof UserConnection) {      
      this.associateConnection((UserConnection)connection);
    } else if (connection != null) {
      throw new ResourceException("Expecting a ManagedConnection that was an instance of " + UserConnection.class + "; instead got " + connection);
    }
  }

  public void associateConnection(final UserConnection handle) throws ResourceException {
    if (handle != null) {
      synchronized (this.handles) {
        this.handles.add(handle);
      }
      handle.setCreator(this);
    }
  }

  @Override
  public void dissociateConnections() {
    synchronized (this.handles) {
      final Iterator<UserConnection> i = this.handles.iterator();
      assert i != null;
      while (i.hasNext()) {
        final UserConnection handle = i.next();
        if (handle != null) {
          handle.setCreator(null);
        }
        i.remove();
      }
    }
  }

  @Override
  public LocalTransaction getLocalTransaction() throws ResourceException {
    throw new NotSupportedException();
  }

  @Override
  public XAResource getXAResource() throws ResourceException {
    /*
      Each time a ManagedConnection.getXAResource method is called,
      the same XAResource instance has to be returned.
    */
    throw new NotSupportedException();
  }

  @Override
  public ManagedConnectionMetaData getMetaData() throws ResourceException {
    if (this.metaData == null) {
      this.metaData = new DroolsManagedConnectionMetaData("user");
    }
    return this.metaData;
  }

  @Override
  public PrintWriter getLogWriter() {
    return this.logWriter;
  }

  @Override
  public void setLogWriter(final PrintWriter logWriter) {
    this.logWriter = logWriter;
  }

  /**
   * Closes the supplied connection handle by {@linkplain
   * #fireConnectionClosedEvent(UserConnection) firing a
   * <tt>ConnectionEvent</tt> indicating the closing}, and completely
   * severing ties between this {@link DroolsManagedConnection} and
   * the supplied {@link UserConnection}.
   *
   * @param handle the {@link UserConnection} to close.  If the value
   * of this parameter is {@code null}, then no action will be taken.
   */
  protected void closeAndDetach(final UserConnection handle) {
    if (handle != null) {
      this.fireConnectionClosedEvent(handle);
      handle.setCreator(null);
      synchronized (this.handles) {
        this.handles.remove(handle);
      }
    }
  }

  @Override
  public void cleanup() throws ResourceException {
    /*
      The method ManagedConnection.cleanup initiates a cleanup of any
      client-specific state maintained by a ManagedConnection
      instance.

      The cleanup must invalidate all connection handles created using
      the ManagedConnection instance.

      Any attempt by an application component to use the associated
      connection handle after cleanup of the underlying
      ManagedConnection should result in an exception.

      The container always drives the cleanup of a ManagedConnection
      instance. The container keeps track of created connection
      handles in an implementation specific mechanism. It invokes
      ManagedConnection.cleanup when it has to invalidate all
      connection handles associated with this ManagedConnection
      instance and put the ManagedConnection instance back in to the
      pool. This may be called after the end of a connection sharing
      scope or when the last associated connection handle is closed
      for a ManagedConnection instance.

      The invocation of the ManagedConnection.cleanup method on an
      already cleaned-up connection should not throw an exception.
    */
    this.dissociateConnections();
  }

  @Override
  public void destroy() throws ResourceException {
    /*
      An application server should explicitly call
      ManagedConnection.destroy to destroy a physical connection. An
      application server should destroy a physical connection to
      manage the size of its connection pool and to reclaim system
      resources.

      A resource adapter should destroy all allocated system resources
      for this ManagedConnection instance when the method destroy is
      called.
    */
  }

  @Override
  public Object getConnection(final Subject subject, final ConnectionRequestInfo requestInfo) throws ResourceException {
    /*
      Note - The connector architecture allows one or more
      ManagedConnection instances to be multiplexed over a single
      physical pipe to an EIS. However, for simplicity, this
      specification describes a ManagedConnection instance as being
      mapped 1-1 to a physical connection.

      [...]

      The getConnection method creates a new application-level
      connection handle. A connection handle is tied to an underlying
      physical connection represented by a ManagedConnection
      instance. [...] A connection handle is tied to its
      ManagedConnection instance in a resource adapter
      implementation-specific way.

      A ManagedConnection instance may use the getConnection method to
      change the state of the physical connection based on the Subject
      and ConnectionRequestInfo arguments. For example, a resource
      adapter can reauthenticate a physical connection to the
      underlying EIS when the application server calls the
      getConnection method. Section 9.1.9, "ManagedConnection" on page
      9-14 specifies re-authentication requirements in more detail.
      
      [...]

      It is strongly recommended that resource adapters provide
      support for concurrent access to a ManagedConnection instance
      from multiple connection handles. This may be required in a
      future release of the specification.
    */
    UserConnection handle = null;
    KnowledgeAgent knowledgeAgent = null;
    try {
      knowledgeAgent = this.getKnowledgeAgent();
    } catch (final InterruptedException i) {
      Thread.currentThread().interrupt();
      throw new ResourceAdapterInternalException(i);
    } catch (final ExecutionException e) {
      throw new ResourceAdapterInternalException(e);
    }
    if (knowledgeAgent != null) {
      if (requestInfo instanceof StatelessKnowledgeSessionConfiguration) {
        handle = new StatelessKnowledgeSessionUserConnection(this, knowledgeAgent.newStatelessKnowledgeSession(), requestInfo);
      } else if (requestInfo instanceof StatefulKnowledgeSessionConfiguration) {
        final KnowledgeBase kb = knowledgeAgent.getKnowledgeBase();
        if (kb == null) {
          throw new ResourceAdapterInternalException("The KnowledgeAgent (" + knowledgeAgent + ") could not return a KnowledgeBase");
        }
        handle = new StatefulKnowledgeSessionUserConnection(this, kb.newStatefulKnowledgeSession(), requestInfo);
      } else {
        throw new ResourceAdapterInternalException(String.format("Cannot create a new user-level connection with the following ConnectionRequestInfo: %s", requestInfo));
      }
      if (handle != null) {
        synchronized (this.handles) {
          this.handles.add(handle);
        }
      }
    } else {
      throw new ResourceAdapterInternalException("knowledgeAgent == null");
    }
    return handle;
  }

}