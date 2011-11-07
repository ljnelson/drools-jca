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

import org.drools.SystemEventListener;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.util.logging.Logger;
import java.util.logging.Level;

public class LoggerSystemEventListener implements SystemEventListener, Serializable {

  /**
   * A serial version identifier uniquely identifying the version of
   * this class.  See the <a
   * href="http://download.oracle.com/javase/6/docs/api/java/io/Serializable.html">documentation
   * for the {@code Serializable} class</a> for details.
   */
  private static final long serialVersionUID = 1L;

  protected transient Logger logger;

  public LoggerSystemEventListener() {
    super();
    this.logger = this.createLogger(this.getClass().getName());
  }

  public LoggerSystemEventListener(final String loggerName) {
    super();
    this.logger = this.createLogger(this.getClass().getName());
  }

  protected Logger createLogger(final String loggerName) {
    return Logger.getLogger(loggerName);
  }

  private final void log(Level level, final String message, final Throwable t, final Object... o) {
    if (level == null) {
      level = Level.ALL;
    }
    if (this.logger != null && this.logger.isLoggable(level)) {      
      final StackTraceElement element = this.getCaller();
      if (element == null) {
        if (t != null) {
          this.logger.log(level, message, t);
        } else {
          this.logger.log(level, message, o);
        }
      } else if (t != null) {
        this.logger.logp(level, element.getClassName(), element.getMethodName(), message, t);
      } else {
        this.logger.logp(level, element.getClassName(), element.getMethodName(), message, o);
      }
    }
  }

  @Override
  public void info(final String m) {
    this.log(Level.INFO, m, null, (Object)null);
  }

  @Override
  public void info(final String m, final Object o) {
    this.log(Level.INFO, m, null, o);
  }

  @Override
  public void warning(final String m) {
    this.log(Level.WARNING, m, null, (Object)null);
  }

  @Override
  public void warning(final String m, final Object o) {
    this.log(Level.WARNING, m, null, o);
  }

  @Override
  public void exception(final Throwable t) {
    this.log(Level.SEVERE, t == null ? "" : t.toString(), t, (Object)null);
  }

  @Override
  public void exception(final String m, final Throwable t) {
    this.log(Level.SEVERE, m, t, (Object)null);
  }

  @Override
  public void debug(final String m) {
    this.log(Level.FINER, m, null, (Object)null);
  }

  @Override
  public void debug(final String m, final Object o) {
    this.log(Level.FINER, m, null, o);
  }

  private final void readObject(final ObjectInputStream ois) throws ClassNotFoundException, IOException {
    if (ois != null) {
      ois.defaultReadObject();
      if (this.logger == null) {
        this.logger = this.createLogger((String)ois.readObject());
        if (this.logger == null) {
          this.logger = Logger.getLogger(this.getClass().getName());
        }
      }
    }
  }

  private final void writeObject(final ObjectOutputStream out) throws IOException {
    if (out != null) {
      out.defaultWriteObject();
      String name = null;
      if (this.logger != null) {
        name = this.logger.getName();
      }
      if (name == null) {
        out.writeObject(this.getClass().getName());
      } else {
        out.writeObject(name);
      }
    }
  }

  private final StackTraceElement getCaller() {
    StackTraceElement element = null;
    final StackTraceElement[] stack = new Throwable().getStackTrace();
    assert stack != null;
    // Skip the first two frames.  They will always be this method, of course,
    // and its caller, which will be one of the info/warning/debug methods
    // above.
    final String myName = LoggerSystemEventListener.class.getName();
    int index = 3; // 0 == this method; 1 == private void log() method; 2 == info/warning/etc. method; 3 == caller?
    for (; index < stack.length; index++) {
      element = stack[index];
      final String name = element.getClassName();
      if (!name.equals(myName) && !name.equals("org.drools.util.DelegatingSystemEventListener")) {
        break;
      }
    }    
    return element;
  }

}