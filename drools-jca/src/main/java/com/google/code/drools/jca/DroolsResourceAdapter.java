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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.net.URL;

import java.security.AccessController;
import java.security.PrivilegedAction;

import java.util.Arrays;
import java.util.Timer;

import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Callable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.UnavailableException;

import javax.resource.spi.endpoint.MessageEndpointFactory;

import javax.resource.spi.work.WorkManager;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;

import javax.resource.ResourceException;

import javax.transaction.xa.XAResource;

import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseConfiguration;
import org.drools.KnowledgeBaseFactory;
import org.drools.SystemEventListenerFactory;

import org.drools.agent.KnowledgeAgent;
import org.drools.agent.KnowledgeAgentConfiguration;
import org.drools.agent.KnowledgeAgentFactory;

import org.drools.io.Resource;
import org.drools.io.ResourceFactory;
import org.drools.io.ResourceFactoryService;
import org.drools.io.ResourceChangeNotifier;
import org.drools.io.ResourceChangeScanner;
import org.drools.io.ResourceChangeScannerConfiguration;

public class DroolsResourceAdapter implements ResourceAdapter, Serializable {

  private static final PropertyChangeListener[] EMPTY_PROPERTY_CHANGE_LISTENER_ARRAY = new PropertyChangeListener[0];

  private static final Pattern environmentVariableSubstitutionPattern = Pattern.compile("\\$\\{(.+)\\}");

  private PropertyChangeSupport propertyChangeSupport;
  
  private KnowledgeAgent ka;

  private ResourceChangeScanner scanner;

  private ResourceChangeNotifier notifier;

  private String changeSetResourceName;

  private boolean scanResources;

  private boolean monitorChangeSetEvents;

  private boolean scanDirectories;

  private int scanningInterval;

  private boolean agentReusesKnowledgeBaseInstance;

  private final KnowledgeAgentFuture kaFuture;

  /**
   * Creates a new {@link DroolsResourceAdapter}.  This no-argument constructor is
   * required because {@link ResourceAdapter} implementations are required to be
   * Java beans.
   */
  public DroolsResourceAdapter() {
    super();
    this.kaFuture = new KnowledgeAgentFuture();
    this.setScanResources(true);
    this.setScanDirectories(true);
    this.setScanningInterval(60);
    this.setMonitorChangeSetEvents(true);
    this.setAgentReusesKnowledgeBaseInstance(true);
  }

  @Override
  public void endpointActivation(final MessageEndpointFactory mef, final ActivationSpec spec) throws ResourceException {

  }

  @Override
  public void endpointDeactivation(final MessageEndpointFactory mef, final ActivationSpec spec) {

  }

  @Override
  public XAResource[] getXAResources(final ActivationSpec[] specs) throws ResourceException {
    return null;
  }

  public String getChangeSetResourceName() {
    return this.changeSetResourceName;
  }

  public void setChangeSetResourceName(final String urlStringRepresentation) {
    final String old = this.getChangeSetResourceName();
    this.changeSetResourceName = urlStringRepresentation;
    this.firePropertyChange("changeSetResourceName", old, this.getChangeSetResourceName());
  }

  public boolean getScanResources() {
    return this.scanResources;
  }

  public void setScanResources(final boolean scanResources) {
    final boolean old = this.getScanResources();
    this.scanResources = scanResources;
    this.firePropertyChange("scanResources", old, this.getScanResources());
  }

  public boolean getScanDirectories() {
    return this.scanDirectories;
  }

  public void setScanDirectories(final boolean scanDirectories) {
    final boolean old = this.getScanDirectories();
    this.scanDirectories = scanDirectories;
    this.firePropertyChange("scanDirectories", old, this.getScanDirectories());
  }

  public int getScanningInterval() {
    return this.scanningInterval;
  }

  public void setScanningInterval(final int interval) {
    final int old = this.getScanningInterval();
    this.scanningInterval = interval;
    this.firePropertyChange("scanningInterval", old, this.getScanningInterval());
  }

  public boolean getMonitorChangeSetEvents() {
    return this.monitorChangeSetEvents;
  }

  public void setMonitorChangeSetEvents(final boolean monitorChangeSetEvents) {
    final boolean old = this.getMonitorChangeSetEvents();
    this.monitorChangeSetEvents = monitorChangeSetEvents;
    this.firePropertyChange("monitorChangeSetEvents", old, this.getMonitorChangeSetEvents());
  }

  public boolean getAgentReusesKnowledgeBaseInstance() {
    return this.agentReusesKnowledgeBaseInstance;
  }

  public void setAgentReusesKnowledgeBaseInstance(final boolean value) {
    final boolean old = this.getAgentReusesKnowledgeBaseInstance();
    this.agentReusesKnowledgeBaseInstance = value;
    this.firePropertyChange("agentReusesKnowledgeBaseInstance", old, this.getAgentReusesKnowledgeBaseInstance());
  }
  
  @Override
  public void start(final BootstrapContext context) throws ResourceAdapterInternalException {
    if (context != null) {
      SystemEventListenerFactory.setSystemEventListener(new LoggerSystemEventListener(this.getClass().getName()));

      // The WorkManager will let us effectively create a background
      // thread on which KnowledgeAgent initialization will happen.
      // KnowledgeAgent initialization takes forever.
      final WorkManager workManager = context.getWorkManager();

      // JCACompliantKnowledgeAgent is a class that makes a
      // KnowledgeAgent as JavaEE friendly as possible.  Its instances
      // will be those produced by
      // KnowledgeAgentFactory#newKnowledgeAgent(); we set that up
      // later.  For now, make it so that the WorkManager is available
      // to any such instances that might (will) be created.
      JCACompliantKnowledgeAgent.setWorkManager(workManager);

      // The Timer here will let us use the correct, spec-compliant
      // Java EE features to schedule the KnowledgeAgent scanner and
      // notifier to monitor the various knowledge bases for
      // recompilation.
      Timer timer = null;
      try {
        timer = context.createTimer();
      } catch (final UnavailableException boom) {
        throw new ResourceAdapterInternalException(boom);
      }

      // 
      try {
        installResourceFactoryService(workManager, timer);
      } catch (final Exception everything) {
        throw new ResourceAdapterInternalException(everything);
      }

      // Set up the ResourceChangeScanner that will repeatedly check the URL we
      // eventually configure our KnowledgeAgent with.
      this.scanner = ResourceFactory.getResourceChangeScannerService();
      assert this.scanner instanceof JCACompliantResourceChangeScanner;
      final ResourceChangeScannerConfiguration scannerConfiguration = this.scanner.newResourceChangeScannerConfiguration();
      assert scannerConfiguration != null;
      scannerConfiguration.setProperty("drools.resource.scanner.interval", String.valueOf(Math.max(0, this.getScanningInterval())));
      this.scanner.configure(scannerConfiguration);
      
      // The scanner is useless without a notifier.  The odd part is that the two
      // are only linked by the KnowledgeAgent.
      this.notifier = ResourceFactory.getResourceChangeNotifierService();
      assert this.notifier instanceof JCACompliantResourceChangeNotifier;

      // Start both the scanner and the notifier.  Currently--bizarrely--they
      // don't know about each other.  They will be married by the internals of
      // the KnowledgeAgent class.
      this.scanner.start();
      this.notifier.start();
      
      // Create and set our KnowledgeAgent.  This will cause the notifier and
      // scanner to link up and begin looking for rules resources.  We do this
      // on a separate thread because it can take a while.
      assert this.kaFuture != null;
      try {
        workManager.scheduleWork(this.kaFuture);
      } catch (final WorkException workException) {
        throw new ResourceAdapterInternalException(workException);
      }

    }
  }

  /**
   * Invokes the {@code private} {@link
   * ResourceFactory#setFactoryService(ResourceFactoryService)} method
   * on the {@link ResourceFactory} class, supplying it with a new
   * instance of the {@link JCACompliantResourceProvider} class.
   * Ideally the rather brute-force approach this method takes would
   * not be necessary, and the {@link
   * ResourceFactory#setFactoryService(ResourceFactoryService)} method
   * would be {@code public}.
   *
   * @param the {@link WorkManager} for the new {@link
   * JCACompliantResourceProvider} to use for thread scheduling.  The value of
   * this parameter may be {@code null}.
   *
   * @param the {@link Timer} for the {@link JCACompliantResourceProvider} to
   * use for task scheduling.  The value of this parameter may be {@code null}.
   *
   * @exception SecurityException if the {@link
   * AccessibleObject#setAccessible(boolean)} method may not be called
   * on the {@link Method} reference that represents the {@code
   * private} {@link
   * ResourceFactory#setFactoryService(ResourceFactoryService)}
   * method
   *
   * @exception IllegalAccessException if no amount of subterfuge can
   * make it so that the {@link
   * ResourceFactory#setFactoryService(ResourceFactoryService)}
   * method can be called successfully
   *
   * @exception NoSuchMethodException if Drools has quietly changed out from
   * under this class and the {@link
   * ResourceFactory#setFactoryService(ResourceFactoryService)} method no longer
   * exists.  This is exceedingly unlikely to happen and probably means that the
   * wrong version of Drools is being used.
   *
   * @exception InvocationTargetException if an {@link Exception} occurs during
   * invocation of the {@link
   * ResourceFactory#setFactoryService(ResourceFactoryService)} method
   */
  private static final void installResourceFactoryService(final WorkManager workManager, final Timer timer) throws SecurityException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
    final Method setFactoryService = ResourceFactory.class.getDeclaredMethod("setFactoryService", ResourceFactoryService.class);
    assert setFactoryService != null;
    if (!setFactoryService.isAccessible()) {
      setFactoryService.setAccessible(true);
    }
    final JCACompliantResourceProvider resourceFactoryService = new JCACompliantResourceProvider();
    resourceFactoryService.setWorkManager(workManager);
    resourceFactoryService.setTimer(timer);
    setFactoryService.invoke(ResourceFactory.class, resourceFactoryService);
  }

  final Future<KnowledgeAgent> getKnowledgeAgentFuture() {
    assert this.kaFuture != null;
    return this.kaFuture;
  }

  protected KnowledgeAgent createKnowledgeAgent() {
    String changeSetResourceName = this.getChangeSetResourceName();
    if (changeSetResourceName == null) {
      throw new IllegalStateException("this.getChangeSetResourceName() == null");
    }

    try {
      AccessController.doPrivileged(new PrivilegedAction<Void>() {
          @Override
          public final Void run() {
            System.setProperty(KnowledgeAgentFactory.PROVIDER_CLASS_NAME_PROPERTY_NAME, JCACompliantKnowledgeAgentProvider.class.getName());
            return null;
          }
        });
    } catch (final SecurityException ohWell) {
      // ignore; we tried our best
    }

    final KnowledgeBaseConfiguration kbc = this.createKnowledgeBaseConfiguration();
    final KnowledgeBase kb;
    if (kbc == null) {
      kb = KnowledgeBaseFactory.newKnowledgeBase();
    } else {
      kb = KnowledgeBaseFactory.newKnowledgeBase(kbc);
    }
    assert kb != null;

    final KnowledgeAgentConfiguration kac = this.createKnowledgeAgentConfiguration();
    final KnowledgeAgent ka = KnowledgeAgentFactory.newKnowledgeAgent(this.getClass().getName(), kb, kac);

    assert assertCorrectKnowledgeAgentImplementation(ka) : "WTF; ka is not a JCACompliantKnowledgeAgent: " + ka.getClass().getName();

    changeSetResourceName = replaceVariables(changeSetResourceName);
    if (changeSetResourceName == null) {
      throw new IllegalStateException("this.getChangeSetResourceName() == null after variable substitution");
    }

    String[] names = changeSetResourceName.split("\\s*[,|]+\\s*");
    if (names == null) {
      names = new String[] { changeSetResourceName };
    }

    Resource resource = null;
    for (String name : names) {
      if (name == null) {
        continue;
      }

      name = name.trim();
      if (name.length() <= 0) {
        continue;
      }

      // Maybe it's an explicit classpath resource.
      if (name.startsWith("classpath:")) {
        // That's easy; we're a classpath resource.
        resource = ResourceFactory.newClassPathResource(name.substring("classpath:".length()));
        assert resource != null;
        if (exists(resource)) {
          break;
        } else {
          resource = null;
        }
      }

      // OK, maybe it's a URL resource.
      try {
        resource = ResourceFactory.newUrlResource(name);
      } catch (final IllegalArgumentException oops) {
        resource = null;
      }
      if (exists(resource)) {
        break;
      } else {
        resource = null;
      }

      // OK, maybe it's a file.
      resource = ResourceFactory.newFileResource(name);
      if (exists(resource)) {
        break;
      } else {
        resource = null;
      }

      // OK, maybe it's a regular old classpath resource.
      resource = ResourceFactory.newClassPathResource(name);
      if (exists(resource)) {
        break;
      } else {
        resource = null;
      }
    }

    if (resource == null) {
      throw new IllegalStateException("Could not find an existent resource by any means that corresponds to " + Arrays.asList(names));
    }

    ka.applyChangeSet(resource);
    assert ka.getKnowledgeBase() != null;
    return ka;
  }

  private static final boolean exists(final Resource resource) {
    boolean returnValue = false;
    if (resource != null) {
      Reader reader = null;
      try {
        reader = resource.getReader();
        returnValue = reader != null;
      } catch (final IOException boom) {
        returnValue = false;
      } finally {
        if (reader != null) {
          try {
            reader.close();
          } catch (final IOException ignore) {
            // ignore
          }
        }
      }
    }
    return returnValue;
  }

  private static final boolean assertCorrectKnowledgeAgentImplementation(final KnowledgeAgent ka) {
    final String providerClassName = AccessController.doPrivileged(new PrivilegedAction<String>() {
        @Override
        public final String run() {
          return System.getProperty(KnowledgeAgentFactory.PROVIDER_CLASS_NAME_PROPERTY_NAME);
        }
      });
    if (JCACompliantKnowledgeAgentProvider.class.getName().equals(providerClassName)) {
      return ka instanceof JCACompliantKnowledgeAgent;
    } else {
      return true;
    }
  }

  protected KnowledgeAgentConfiguration createKnowledgeAgentConfiguration() {
    final KnowledgeAgentConfiguration c = KnowledgeAgentFactory.newKnowledgeAgentConfiguration();
    assert c != null;
    c.setProperty("drools.agent.newInstance", String.valueOf(!this.getAgentReusesKnowledgeBaseInstance()));
    c.setProperty("drools.agent.scanResources", String.valueOf(this.getScanResources()));
    c.setProperty("drools.agent.scanDirectories", String.valueOf(this.getScanDirectories()));
    c.setProperty("drools.agent.monitorChangeSetEvents", String.valueOf(this.getMonitorChangeSetEvents()));
    return c;
  }

  protected KnowledgeBaseConfiguration createKnowledgeBaseConfiguration() {
    return KnowledgeBaseFactory.newKnowledgeBaseConfiguration(null, Thread.currentThread().getContextClassLoader());
  }

  @Override
  public void stop() {
    assert this.kaFuture != null;
    this.kaFuture.cancel(true);
    if (this.notifier != null) {
      this.notifier.stop();
    }
    if (this.scanner != null) {
      this.scanner.stop();
    }
  }

  /*
   * PropertyChangeListener support.
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

  static final String replaceVariables(final String rep) {
    String returnValue = null;
    if (rep != null) {
      final Matcher matcher = environmentVariableSubstitutionPattern.matcher(rep);
      final StringBuffer sb = new StringBuffer();
      while (matcher.find()) {
        final String varName = matcher.group(1);
        assert varName != null;
        if (varName.startsWith("env.") && varName.length() > "env.".length()) {
          final String varValue = AccessController.doPrivileged(new PrivilegedAction<String>() {
              @Override
              public final String run() {
                return System.getenv(varName.substring("env.".length()));
              }
            });
          if (varValue != null) {
            matcher.appendReplacement(sb, varValue);
          } else {
            matcher.appendReplacement(sb, "\\${" + varName + "}");
          }
        } else {
          final String varValue = AccessController.doPrivileged(new PrivilegedAction<String>() {
              @Override
              public final String run() {
                return System.getProperty(varName, "\\${" + varName + "}");
              }
            });
          if (varValue != null) {
            matcher.appendReplacement(sb, varValue);
          } else {
            matcher.appendReplacement(sb, "\\${" + varName + "}");
          }
        }
      }
      matcher.appendTail(sb);
      returnValue = sb.toString();
    }
    return returnValue;
  }


  /*
   * Inner and nested classes.
   */

  public final class CreateKnowledgeAgentCallable implements Callable<KnowledgeAgent> {
    
    @Override
    public KnowledgeAgent call() throws Exception {
      return DroolsResourceAdapter.this.createKnowledgeAgent();
    }

  }

  public final class KnowledgeAgentFuture extends FutureTask<KnowledgeAgent> implements Work {

    public KnowledgeAgentFuture() {
      super(new CreateKnowledgeAgentCallable());
    }

    @Override
    public final void release() {
      this.cancel(false);
    }

  }

}