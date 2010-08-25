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

import java.io.Closeable;

import java.util.Collection;
import java.util.Collections;

import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ConnectionRequestInfo;

import org.drools.command.Command;

import org.drools.runtime.StatelessKnowledgeSession;

import org.drools.runtime.ExecutionResults;
import org.drools.runtime.Globals;

import org.drools.event.process.ProcessEventListener;

import org.drools.event.rule.AgendaEventListener;
import org.drools.event.rule.WorkingMemoryEventListener;

public class StatelessKnowledgeSessionUserConnection extends AbstractKnowledgeSessionUserConnection<StatelessKnowledgeSession> implements StatelessKnowledgeSession {
  

  /*
   * Constructors.
   */


  protected StatelessKnowledgeSessionUserConnection(final DroolsManagedConnection creator, final StatelessKnowledgeSession delegate, final ConnectionRequestInfo connectionRequestInfo) {
    super(creator, connectionRequestInfo, delegate);
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


  public Globals getGlobals() {
    final StatelessKnowledgeSession delegate = this.getDelegate();
    if (delegate == null) {
      return null;
    }
    return delegate.getGlobals();
  }

  public void setGlobal(final String id, final Object thing) {
    final StatelessKnowledgeSession delegate = this.getDelegate();
    if (delegate != null) {
      delegate.setGlobal(id, thing);
    }
  }

}