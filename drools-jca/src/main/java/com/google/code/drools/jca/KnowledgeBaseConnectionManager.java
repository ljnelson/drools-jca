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

import java.io.Serializable;

import javax.resource.ResourceException;

import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;

public class KnowledgeBaseConnectionManager implements ConnectionManager {

  /**
   * A serial version identifier uniquely identifying the version of
   * this class.  See the <a
   * href="http://download.oracle.com/javase/6/docs/api/java/io/Serializable.html">documentation
   * for the {@code Serializable} class</a> for details.
   */
  private static final long serialVersionUID = 1L;

  public KnowledgeBaseConnectionManager() {
    super();
  }

  @Override
  public Object allocateConnection(final ManagedConnectionFactory mcf, final ConnectionRequestInfo info) throws ResourceException {
    Object returnValue = null;
    if (mcf != null) {
      final ManagedConnection mc = mcf.createManagedConnection(null /* no Subject */, info);
      if (mc != null) {
        returnValue = mc.getConnection(null, info);
      }
    }
    return returnValue;
  }

}