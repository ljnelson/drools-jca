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

import java.util.Properties;

import javax.annotation.Resource;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ManagedConnectionFactory;

import org.apache.openejb.api.LocalClient;

import org.drools.KnowledgeBase;

import org.drools.runtime.StatelessKnowledgeSession;
import org.drools.runtime.StatefulKnowledgeSession;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

@LocalClient
public class TestCaseIntegrateWithOpenEJB {

  private final Object icLock;

  private InitialContext ic;

  // See
  // http://markmail.org/thread/kp4wft7lnwq4sybh#query:+page:1+mid:ebfybgjawllun4ke+state:results
  // for why this name is what it is.
  @Resource(name="java:openejb/Resource/ra/RulesEngine")
  private KnowledgeBase ra;

  public TestCaseIntegrateWithOpenEJB() {
    super();
    this.icLock = new byte[0];
  }

  @Test
  public void testStateless() throws Exception {
    assertTrue(this.ra instanceof KnowledgeBaseUserConnectionFactory);
    final StatelessKnowledgeSession sks = this.ra.newStatelessKnowledgeSession();
    assertNotNull(sks);
    sks.execute((Object)null);
  }

  @Test
  public void testStateful() throws Exception {
    assertTrue(this.ra instanceof KnowledgeBaseUserConnectionFactory);
    final StatefulKnowledgeSession sfks = this.ra.newStatefulKnowledgeSession();
    try {
      assertNotNull(sfks);
    } finally {
      sfks.dispose();
    }
  }

  @Before
  public void setUp() throws Exception {
    this.closeContext();
    final Properties p = new Properties();
    p.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.apache.openejb.client.LocalInitialContextFactory");
    // See http://blog.jonasbandi.net/2009/06/restarting-embedded-openejb-container.html:
    p.setProperty("openejb.embedded.initialcontext.close", "destroy");

    // These usernames and passwords are present by default in the OpenEJB
    // distribution; we use them here just to test authentication.
    p.setProperty(Context.SECURITY_PRINCIPAL, "jonathan");
    p.setProperty(Context.SECURITY_CREDENTIALS, "secret");

    synchronized (this.icLock) {
      this.ic = new InitialContext(p);
      this.ic.bind("inject", this);
    }
  }

  @After
  public void closeContext() throws NamingException {
    synchronized (this.icLock) {
      if (this.ic != null) {
        this.ic.close();
      }
    }
    this.ra = null;
  }

}