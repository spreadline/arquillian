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

import javax.inject.Inject;

import org.jboss.arquillian.osgi.OSGiContainer;
import org.jboss.arquillian.protocol.jmx.ResourceCallbackHandler;
import org.jboss.arquillian.protocol.jmx.ResourceCallbackHandlerAssociation;
import org.jboss.arquillian.spi.Context;
import org.jboss.arquillian.spi.TestClass;
import org.jboss.arquillian.spi.TestEnricher;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

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
 */
public class OSGiTestEnricher implements TestEnricher, BundleContextInjector, BundleInjector
{
   private BundleContext bundleContext;
   private Bundle bundle;

   @Override
   public void inject(Bundle value)
   {
      bundle = value;
   }

   @Override
   public void inject(BundleContext value)
   {
      bundleContext = value;
   }

   @Override
   public void enrich(Context context, Object testCase)
   {
      // [TODO] Remove these hacks when it becomes possible to pass data to the enrichers
      inject(BundleContextAssociation.getBundleContext());
      inject(BundleAssociation.getBundle());

      // [TODO] Injected bundle can be null on the client side when
      // Arquillian incorrectly tries to enrich the test class
      Class<?> testClass = testCase.getClass();
      if (bundle == null || isInjectionTarget(testClass) == false)
         return;

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

   public static boolean isInjectionTarget(Class<?> testClass)
   {
      for (Field field : testClass.getDeclaredFields())
      {
         if (field.isAnnotationPresent(Inject.class))
         {
            if (field.getType().isAssignableFrom(OSGiContainer.class))
               return true;
            if (field.getType().isAssignableFrom(BundleContext.class))
               return true;
            if (field.getType().isAssignableFrom(Bundle.class))
               return true;
         }
      }
      return false;
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
         field.set(testCase, bundleContext);
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
         field.set(testCase, bundle);
      }
      catch (IllegalAccessException ex)
      {
         throw new IllegalStateException("Cannot inject Bundle", ex);
      }
   }

   private OSGiContainer getOSGiContainer(Context context, TestClass testClass)
   {
      ResourceCallbackHandler callbackHandler = ResourceCallbackHandlerAssociation.getCallbackHandler();
      return OSGiContainer.Factory.newInstance(bundleContext, testClass, callbackHandler);
   }
}
