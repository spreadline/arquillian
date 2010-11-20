/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.arquillian.testenricher.osgi;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import javax.inject.Inject;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;

import org.jboss.arquillian.osgi.OSGiContainer;
import org.jboss.arquillian.osgi.internal.EmbeddedOSGiContainer;
import org.jboss.arquillian.osgi.internal.RemoteOSGiContainer;
import org.jboss.arquillian.protocol.jmx.ExecutionTypeAssociation;
import org.jboss.arquillian.protocol.jmx.ExecutionTypeInjector;
import org.jboss.arquillian.protocol.jmx.JMXMethodExecutor.ExecutionType;
import org.jboss.arquillian.spi.Context;
import org.jboss.arquillian.spi.TestClass;
import org.jboss.arquillian.spi.TestEnricher;
import org.jboss.arquillian.spi.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * The OSGi TestEnricher
 *
 * The enricher supports the injection of the system BundleContext and the test Bundle.
 *
 * <pre><code>
 *    @Inject
 *    BundleContext context;
 *
 *    @Inject
 *    Bundle bundle;
 * </code></pre>
 *
 * @author thomas.diesler@jboss.com
 * @version $Revision: $
 */
public class OSGiTestEnricher implements TestEnricher, ExecutionTypeInjector
{
   // Provide logging
   private static Logger log = Logger.getLogger(OSGiTestEnricher.class);

   private ExecutionType executionType;

   @Override
   public void inject(ExecutionType value)
   {
      executionType = value;
   }

   @Override
   public void enrich(Context context, Object testCase)
   {
      // Get the ExecutionType associated with the thread.
      // [TODO] Remove this hack when it becomes possible to pass data to the enrichers
      inject(ExecutionTypeAssociation.getExecutionType());

      Class<? extends Object> testClass = testCase.getClass();
      for (Field field : testClass.getDeclaredFields())
      {
         if (field.isAnnotationPresent(Inject.class))
         {
            if (field.getType().isAssignableFrom(OSGiContainer.class))
            {
               injectOSGiContainer(context, testCase, field);
            }
            else if (field.getType().isAssignableFrom(BundleContext.class))
            {
               injectBundleContext(context, testCase, field);
            }
            if (field.getType().isAssignableFrom(Bundle.class))
            {
               injectBundle(context, testCase, field);
            }
         }
      }
   }

   @Override
   public Object[] resolve(Context context, Method method)
   {
      return null;
   }

   private void injectOSGiContainer(Context context, Object testCase, Field field)
   {
      try
      {
         TestClass testClass = new TestClass(testCase.getClass());
         field.set(testCase, getOSGiContainer(context, testClass));
      }
      catch (IllegalAccessException ex)
      {
         throw new IllegalStateException("Cannot inject BundleContext", ex);
      }
   }

   private void injectBundleContext(Context context, Object testCase, Field field)
   {
      try
      {
         field.set(testCase, getBundleContext(context));
      }
      catch (IllegalAccessException ex)
      {
         throw new IllegalStateException("Cannot inject BundleContext", ex);
      }
   }

   private void injectBundle(Context context, Object testCase, Field field)
   {
      try
      {
         field.set(testCase, getTestBundle(context, testCase.getClass()));
      }
      catch (IllegalAccessException ex)
      {
         throw new IllegalStateException("Cannot inject Bundle", ex);
      }
   }

   private OSGiContainer getOSGiContainer(Context context, TestClass testClass)
   {
      BundleContext bundleContext = getBundleContext(context);

      OSGiContainer result = null;
      if (executionType == ExecutionType.EMBEDDED)
         result =  new EmbeddedOSGiContainer(bundleContext, testClass);
      else if (executionType == ExecutionType.REMOTE)
         result = new RemoteOSGiContainer(bundleContext, testClass);

      return result;
   }

   private BundleContext getBundleContext(Context context)
   {
      BundleContext bundleContext = context.get(BundleContext.class);
      if (bundleContext == null)
         bundleContext = getBundleContextFromHolder();

      // Make sure this is really the system context
      bundleContext = bundleContext.getBundle(0).getBundleContext();
      return bundleContext;
   }

   private Bundle getTestBundle(Context context, Class<?> testClass)
   {
      Bundle testbundle = context.get(Bundle.class);
      if (testbundle == null)
      {
         // Get the test bundle from PackageAdmin with the test class as key
         BundleContext bundleContext = getBundleContext(context);
         ServiceReference sref = bundleContext.getServiceReference(PackageAdmin.class.getName());
         PackageAdmin pa = (PackageAdmin)bundleContext.getService(sref);
         testbundle = pa.getBundle(testClass);
      }
      return testbundle;
   }

   /**
    * Get the BundleContext associated with the arquillian-bundle
    */
   private BundleContext getBundleContextFromHolder()
   {
      try
      {
         MBeanServer mbeanServer = findOrCreateMBeanServer();
         ObjectName oname = new ObjectName(BundleContextHolder.OBJECT_NAME);
         BundleContextHolder holder = MBeanServerInvocationHandler.newProxyInstance(mbeanServer, oname, BundleContextHolder.class, false);
         return holder.getBundleContext();
      }
      catch (JMException ex)
      {
         throw new IllegalStateException("Cannot obtain arquillian-bundle context", ex);
      }
   }

   /**
    * Find or create the MBeanServer
    */
   public static MBeanServer findOrCreateMBeanServer()
   {
      MBeanServer mbeanServer = null;

      ArrayList<MBeanServer> serverArr = MBeanServerFactory.findMBeanServer(null);
      if (serverArr.size() > 1)
         log.warning("Multiple MBeanServer instances: " + serverArr);

      if (serverArr.size() > 0)
      {
         mbeanServer = serverArr.get(0);
         log.fine("Found MBeanServer: " + mbeanServer.getDefaultDomain());
      }

      if (mbeanServer == null)
      {
         log.fine("No MBeanServer, create one ...");
         mbeanServer = MBeanServerFactory.createMBeanServer();
      }

      return mbeanServer;
   }
}
