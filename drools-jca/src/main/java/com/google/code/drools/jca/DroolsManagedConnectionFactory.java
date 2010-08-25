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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import java.io.PrintWriter;
import java.io.Serializable;

import java.util.Collections;
import java.util.Set;

import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;

import javax.resource.ResourceException;
import javax.resource.NotSupportedException;

import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.ResourceAdapterAssociation;

import javax.security.auth.Subject;

import org.drools.agent.KnowledgeAgent;
import org.drools.agent.KnowledgeAgentFactory;

import org.drools.io.ResourceFactory;
import org.drools.io.ResourceChangeNotifier;
import org.drools.io.ResourceChangeScanner;
import org.drools.io.ResourceChangeScannerConfiguration;

 /**
  * A {@link Serializable} {@link ManagedConnectionFactory} that vends both
  * {@link ManagedConnection} instances as well as {@link
  * KnowledgeBaseConnectionFactory} instances in accordance with the Java
  * Connector Architecture specification, version 1.5.
  *
  * @author <a href="mailto:ljnelson@gmail.com">Laird Nelson</a>
  * @version <script type="text/javascript"><!--
  * document.write("$Revision$".match(/\d*.?\d+/)[0]);
  * --></script><noscript>$Revision$</noscript>
  * @since January 3, 2010
  */
public class DroolsManagedConnectionFactory implements ManagedConnectionFactory, ResourceAdapterAssociation {

  private static final PropertyChangeListener[] EMPTY_PROPERTY_CHANGE_LISTENER_ARRAY = new PropertyChangeListener[0];

  private transient PrintWriter logWriter;

  private ResourceAdapter ra;

  private Future<KnowledgeAgent> kaFuture;

  private PropertyChangeSupport propertyChangeSupport;


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link DroolsManagedConnectionFactory}.
   * This no-argument constructor is required because {@link
   * ManagedConnectionFactory} instances are required to be Java beans.
   */
  public DroolsManagedConnectionFactory() {
    super();
  }


  /*
   * KnowledgeAgentFuture property.
   */


  public Future<KnowledgeAgent> getKnowledgeAgentFuture() {
    return this.kaFuture;
  }

  public void setKnowledgeAgentFuture(final Future<KnowledgeAgent> kaFuture) {
    this.kaFuture = kaFuture;
  }


  /*
   * KnowledgeAgent property.
   */


  public final KnowledgeAgent getKnowledgeAgent() throws InterruptedException, ExecutionException {
    final Future<KnowledgeAgent> kaFuture = this.getKnowledgeAgentFuture();
    if (kaFuture != null) {
      return kaFuture.get();
    }
    return null;
  }


  /*
   * LogWriter property.
   */


  @Override
  public PrintWriter getLogWriter() {
    return this.logWriter;
  }

  @Override
  public void setLogWriter(final PrintWriter out) {
    final PrintWriter old = this.getLogWriter();
    this.logWriter = out;
    this.firePropertyChange("logWriter", old, this.getLogWriter());
  }


  /*
   * User-visible ConnectionFactory creation.
   */


  @Override
  public final Object createConnectionFactory() throws ResourceException {
    // This method will be called only in a non-managed environment (i.e. not
    // Java EE).
    return this.createConnectionFactory(new KnowledgeBaseConnectionManager());
  }

  @Override
  public Object createConnectionFactory(final ConnectionManager connectionManager /* supplied by app server */) throws ResourceException {
    /*
      The ConnectionFactory instance initially handles the connection request
      from the application component in a resource adapter-specific way. It then
      delegates the connection request to the associated ConnectionManager
      instance. The ConnectionManager instance has been associated with the
      ConnectionFactory instance when the ConnectionFactory was instantiated.
    */
    return new KnowledgeBaseUserConnectionFactory(this, connectionManager, this.getKnowledgeAgentFuture());
  }


  /*
   * ManagedConnection creation and matching.
   */


  @Override
  public ManagedConnection createManagedConnection(final Subject subject, final ConnectionRequestInfo requestInfo) throws ResourceException {
    /*
      The method createManagedConnection creates a new physical connection to
      the underlying EIS instance. The ManagedConnectionFactory instance uses
      the security information (passed as a Subject instance) and an optional
      ConnectionRequestInfo instance to create this new physical connection....
    */ 
    final ManagedConnection mc = new DroolsManagedConnection(this, requestInfo);
    mc.setLogWriter(this.getLogWriter());
    return mc;
  }

  @Override
  public ManagedConnection matchManagedConnections(final Set/*<ManagedConnection>*/ suppliedManagedConnectionSet, final Subject subject, final ConnectionRequestInfo suppliedCRI) {
    /*
      The matchManagedConnections method enables the application server to use
      resource adapter-specific criteria for matching a ManagedConnection
      instance to service a connection request. The application server finds a
      candidate set of ManagedConnection instances from its connection pool
      based on application server-specific criteria, and passes this candidate
      set to the matchManagedConnections method. If the application server
      implements connection pooling, it must use the matchManagedConnections
      method to choose a suitable connection.

      The matchManagedConnections method matches a candidate set of connections
      using criteria known internally to the resource adapter. The criteria used
      for matching connections is specific to a resource adapter and is not
      specified by the connector architecture.

      A ManagedConnection instance has specific internal state information based
      on its security context and physical connection. The
      ManagedConnectionFactory implementation compares this information for each
      ManagedConnection instance in the candidate set against the information
      passed in through the matchManagedConnections method and the configuration
      of this ManagedConnectionFactory instance. The ManagedConnectionFactory
      uses the results of this comparison to choose the ManagedConnection
      instance that can best satisfy the current connection request.

      If the resource adapter cannot find an acceptable ManagedConnection
      instance, it returns a null value. In this case, the application server
      requests the resource adapter to create a new connection instance.

      If the resource adapter does not support connection matching, it must
      throw a NotSupportedException when matchManagedConnections method is
      invoked.  This allows an application server to avoid pooling connections
      obtained from that resource adapter.
    */

    // We require a ConnectionRequestInfo to do any connection matching, and we
    // can't match anything if we weren't given anything.
    if (suppliedCRI == null || suppliedManagedConnectionSet == null || suppliedManagedConnectionSet.isEmpty()) {
      return null;
    }

    // Next, let's see if there's one in there that matches.
    ManagedConnection returnValue = null;
    @SuppressWarnings("unchecked")
    final Set<? extends ManagedConnection> managedConnectionSet = (Set<ManagedConnection>)suppliedManagedConnectionSet;
    for (final ManagedConnection m : managedConnectionSet) {
      if (m instanceof DroolsManagedConnection && suppliedCRI.equals(((DroolsManagedConnection)m).getConnectionRequestInfo())) {
        returnValue = m;
        break;
      }
    }
    return returnValue;
  }

  
  /*
   * Hashcode, equals and other equality-related methods.
   */


  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(Object other) {
    return super.equals(other);
  }


  /*
   * ResourceAdapterAssociation implementation.
   */


  @Override
  public ResourceAdapter getResourceAdapter() {
    return this.ra;
  }

  @Override
  public void setResourceAdapter(final ResourceAdapter ra) throws ResourceException {
    final ResourceAdapter old = this.getResourceAdapter();
    this.ra = ra;
    if (ra instanceof DroolsResourceAdapter) {
      final DroolsResourceAdapter dra = (DroolsResourceAdapter)ra;
      this.setKnowledgeAgentFuture(dra.getKnowledgeAgentFuture());
    }
    this.firePropertyChange("resourceAdapter", old, this.getResourceAdapter());
  }


  /*
   * PropertyChangeListener support.  The JCA specification indicates that any
   * configuration properties of a ManagedConnectionFactory implementation must
   * be either bound or constrained.
   */
  
  
  public void addPropertyChangeListener(final String name, final PropertyChangeListener listener) {
    if (listener != null) {
      if (this.propertyChangeSupport == null) {
        this.propertyChangeSupport = new PropertyChangeSupport(this);
      }
      this.propertyChangeSupport.addPropertyChangeListener(name, listener);
    }
  }

  public void addPropertyChangeListener(final PropertyChangeListener listener) {
    if (listener != null) {
      if (this.propertyChangeSupport == null) {
        this.propertyChangeSupport = new PropertyChangeSupport(this);
      }
      this.propertyChangeSupport.addPropertyChangeListener(listener);
    }
  }

  public void removePropertyChangeListener(final String name, final PropertyChangeListener listener) {
    if (listener != null && this.propertyChangeSupport != null) {
      this.propertyChangeSupport.removePropertyChangeListener(name, listener);
    }
  }

  public void removePropertyChangeListener(final PropertyChangeListener listener) {
    if (listener != null && this.propertyChangeSupport != null) {
      this.propertyChangeSupport.removePropertyChangeListener(listener);
    }
  }

  public PropertyChangeListener[] getPropertyChangeListeners(final String name) {
    if (this.propertyChangeSupport != null) {
      return this.propertyChangeSupport.getPropertyChangeListeners(name);
    }
    return EMPTY_PROPERTY_CHANGE_LISTENER_ARRAY;
  }

  public PropertyChangeListener[] getPropertyChangeListeners() {
    if (this.propertyChangeSupport != null) {
      return this.propertyChangeSupport.getPropertyChangeListeners();
    }
    return EMPTY_PROPERTY_CHANGE_LISTENER_ARRAY;
  }

  protected final void firePropertyChange(final String propertyName, final Object old, final Object newValue) {
    if (this.propertyChangeSupport != null) {
      this.propertyChangeSupport.firePropertyChange(propertyName, old, newValue);
    }
  }

  protected final void firePropertyChange(final String propertyName, final int old, final int newValue) {
    if (this.propertyChangeSupport != null) {
      this.propertyChangeSupport.firePropertyChange(propertyName, old, newValue);
    }
  }

  protected void firePropertyChange(final String name, final boolean old, final boolean newValue) {
    if (this.propertyChangeSupport != null) {
      this.propertyChangeSupport.firePropertyChange(name, old, newValue);
    }
  }
  
}