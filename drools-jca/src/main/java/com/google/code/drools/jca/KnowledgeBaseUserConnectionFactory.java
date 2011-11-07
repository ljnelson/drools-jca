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

import java.io.Serializable;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;

import javax.naming.Reference;

import javax.resource.Referenceable;
import javax.resource.ResourceException;

import javax.resource.spi.ConnectionManager;
import javax.resource.spi.LazyAssociatableConnectionManager;
import javax.resource.spi.ManagedConnectionFactory;

import org.drools.agent.KnowledgeAgent;

import org.drools.KnowledgeBase;
import org.drools.RuntimeDroolsException;

import org.drools.definition.KnowledgePackage;

import org.drools.definition.process.Process;

import org.drools.definition.rule.Query;
import org.drools.definition.rule.Rule;

import org.drools.definition.type.FactType;

import org.drools.event.knowledgebase.KnowledgeBaseEventListener;

import org.drools.runtime.Environment;
import org.drools.runtime.KnowledgeSessionConfiguration;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.StatelessKnowledgeSession;

public class KnowledgeBaseUserConnectionFactory implements KnowledgeBase, Serializable, Referenceable {

  /**
   * A serial version identifier uniquely identifying the version of
   * this class.  See the <a
   * href="http://download.oracle.com/javase/6/docs/api/java/io/Serializable.html">documentation
   * for the {@code Serializable} class</a> for details.
   */
  private static final long serialVersionUID = 1L;

  private Reference reference;
  
  private final ManagedConnectionFactory creator;

  private final ConnectionManager cm;

  private final Future<KnowledgeAgent> kaFuture;
  
  /*
   * Constructors.
   */


  public KnowledgeBaseUserConnectionFactory(final Future<KnowledgeAgent> kaFuture) {
    this(null, null, kaFuture);
  }

  public KnowledgeBaseUserConnectionFactory(final ManagedConnectionFactory creator, final ConnectionManager cm, final Future<KnowledgeAgent> kaFuture) {
    super();
    this.creator = creator;
    this.cm = cm;
    this.kaFuture = kaFuture;
    if (kaFuture == null) {
      throw new IllegalArgumentException("kaFuture", new NullPointerException("kaFuture == null"));
    }
  }


  /*
   * KnowledgeBase property.
   */


  private final KnowledgeBase getKnowledgeBase() {
    final KnowledgeAgent ka = this.getKnowledgeAgent();
    assert ka != null;
    return ka.getKnowledgeBase();
  }


  /*
   * KnowledgeAgent property.
   */


  private final KnowledgeAgent getKnowledgeAgent() {
    assert this.kaFuture != null;
    KnowledgeAgent ka = null;
    try {
      ka = this.kaFuture.get();
    } catch (final InterruptedException i) {
      Thread.currentThread().interrupt();
      throw new RuntimeDroolsException(i);
    } catch (final ExecutionException e) {
      throw new RuntimeDroolsException(e);
    }
    if (ka == null) {
      throw new IllegalStateException("No KnowledgeAgent available");
    }
    return ka;
  }


  /*
   * KnowledgeBaseEventManager implementation.
   */
  

  @Override
  public Collection<KnowledgeBaseEventListener> getKnowledgeBaseEventListeners() {
    final KnowledgeBase kb = this.getKnowledgeBase();
    if (kb == null) {
      return Collections.emptySet();
    }
    return kb.getKnowledgeBaseEventListeners();
  }

  @Override
  public void addEventListener(final KnowledgeBaseEventListener listener) {
    if (listener != null) {
      final KnowledgeBase kb = this.getKnowledgeBase();
      if (kb != null) {
        kb.addEventListener(listener);
      }
    }
  }

  @Override
  public void removeEventListener(final KnowledgeBaseEventListener listener) {
    if (listener != null) {
      final KnowledgeBase kb = this.getKnowledgeBase();
      if (kb != null) {
        kb.removeEventListener(listener);
      }
    }
  }


  /*
   * KnowledgeBase implementation.
   */

  @Override
  public Set<String> getEntryPointIds() {
    final KnowledgeBase kb = this.getKnowledgeBase();
    if (kb == null) {
      return Collections.emptySet();
    }
    return kb.getEntryPointIds();
  }

  @Override
  public void addKnowledgePackages(final Collection<KnowledgePackage> c) {
    if (c != null && !c.isEmpty()) {
      final KnowledgeBase kb = this.getKnowledgeBase();
      if (kb != null) {
        kb.addKnowledgePackages(c);
      }
    }
  }

  @Override
  public Collection<KnowledgePackage> getKnowledgePackages() {
    final KnowledgeBase kb = this.getKnowledgeBase();
    if (kb == null) {
      return Collections.emptySet();
    }
    return kb.getKnowledgePackages();
  }

  @Override
  public KnowledgePackage getKnowledgePackage(final String name) {
    final KnowledgeBase kb = this.getKnowledgeBase();
    if (kb == null) {
      return null;
    }
    return kb.getKnowledgePackage(name);
  }
  
  @Override
  public void removeKnowledgePackage(final String name) {
    final KnowledgeBase kb = this.getKnowledgeBase();
    if (kb != null) {
      kb.removeKnowledgePackage(name);
    }
  }

  @Override
  public Rule getRule(final String packageName, final String ruleName) {
    final KnowledgeBase kb = this.getKnowledgeBase();
    if (kb == null) {
      return null;
    }
    return kb.getRule(packageName, ruleName);
  }

  @Override
  public void removeRule(final String packageName, final String ruleName) {
    final KnowledgeBase kb = this.getKnowledgeBase();
    if (kb != null) {
      kb.removeRule(packageName, ruleName);
    }
  }

  @Override
  public void removeFunction(final String packageName, final String functionName) {
    final KnowledgeBase kb = this.getKnowledgeBase();
    if (kb != null) {
      kb.removeFunction(packageName, functionName);
    }
  }

  @Override
  public FactType getFactType(final String packageName, final String typeName) {
    final KnowledgeBase kb = this.getKnowledgeBase();
    if (kb == null) {
      return null;
    }
    return kb.getFactType(packageName, typeName);
  }

  @Override
  public Process getProcess(final String processName) {
    final KnowledgeBase kb = this.getKnowledgeBase();
    if (kb == null) {
      return null;
    }
    return kb.getProcess(processName);
  }

  @Override
  public void removeProcess(final String processName) {
    final KnowledgeBase kb = this.getKnowledgeBase();
    if (kb != null) {
      kb.removeProcess(processName);
    }
  }

  @Override
  public Collection<Process> getProcesses() {
    final KnowledgeBase kb = this.getKnowledgeBase();
    if (kb == null) {
      return null;
    }
    return kb.getProcesses();
  }

  @Override
  public Query getQuery(final String packageName, final String queryName) {
    final KnowledgeBase kb = this.getKnowledgeBase();
    if (kb == null) {
      return null;
    }
    return kb.getQuery(packageName, queryName);
  }

  @Override
  public void removeQuery(final String packageName, final String queryName) {
    final KnowledgeBase kb = this.getKnowledgeBase();
    if (kb != null) {
      kb.removeQuery(packageName, queryName);
    }
  }

  @Override
  public final StatelessKnowledgeSession newStatelessKnowledgeSession() {
    return this.newStatelessKnowledgeSession(null);
  }

  @Override
  public StatelessKnowledgeSession newStatelessKnowledgeSession(final KnowledgeSessionConfiguration conf) {
    StatelessKnowledgeSession returnValue = null;
    if (this.cm == null) {
      final KnowledgeAgent ka = this.getKnowledgeAgent();
      assert ka != null;
      returnValue = ka.newStatelessKnowledgeSession(conf);
    } else {
      if (this.creator == null) {
        throw new IllegalStateException("this.creator == null");
      }
      try {
        returnValue = (StatelessKnowledgeSession)this.cm.allocateConnection(this.creator, new StatelessKnowledgeSessionConfiguration(conf));
      } catch (final ResourceException kaboom) {
        throw new RuntimeDroolsException(kaboom);
      }
      if (returnValue instanceof StatelessKnowledgeSessionUserConnection && cm instanceof LazyAssociatableConnectionManager) {
        ((StatelessKnowledgeSessionUserConnection)returnValue).setLazyAssociatableConnectionManager((LazyAssociatableConnectionManager)cm);
      }
    }
    return returnValue;
  }

  @Override
  public final StatefulKnowledgeSession newStatefulKnowledgeSession() {
    return this.newStatefulKnowledgeSession(null, null);
  }

  @Override
  public StatefulKnowledgeSession newStatefulKnowledgeSession(final KnowledgeSessionConfiguration conf, final Environment env) {
    StatefulKnowledgeSession returnValue = null;
    if (this.cm == null) {
      final KnowledgeAgent ka = this.getKnowledgeAgent();
      assert ka != null;
      final KnowledgeBase kb = ka.getKnowledgeBase();
      if (kb != null) {
        throw new IllegalStateException("KnowledgeAgent.getKnowledgeBase() == null");
      }
      returnValue = kb.newStatefulKnowledgeSession(conf, env);
    } else {
      if (this.creator == null) {
        throw new IllegalStateException("this.creator == null");
      }
      try {
        returnValue = (StatefulKnowledgeSession)this.cm.allocateConnection(this.creator, new StatefulKnowledgeSessionConfiguration(conf, env));
      } catch (final ResourceException kaboom) {
        throw new RuntimeDroolsException(kaboom);
      }
      if (returnValue instanceof AbstractKnowledgeSessionUserConnection && cm instanceof LazyAssociatableConnectionManager) {
        ((AbstractKnowledgeSessionUserConnection)returnValue).setLazyAssociatableConnectionManager((LazyAssociatableConnectionManager)cm);
      }
    }
    return returnValue;
  }

  public Collection<StatefulKnowledgeSession> getStatefulKnowledgeSessions() {
    final KnowledgeBase kb = this.getKnowledgeBase();
    if (kb == null) {
      return Collections.emptySet();
    }
    return kb.getStatefulKnowledgeSessions();
  }


  /*
   * Referenceable implementation.
   *
   * "The implementation class for a connection factory interface must implement
   * both the java.io.Serializable and javax.resource.Referenceable interfaces
   * to support JNDI registration."
  */


  @Override
  public Reference getReference() {
    return this.reference;
  }

  @Override
  public void setReference(final Reference reference) {
    this.reference = reference;
  }

}