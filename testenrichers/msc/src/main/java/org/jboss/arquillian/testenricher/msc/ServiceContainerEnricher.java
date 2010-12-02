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
package org.jboss.arquillian.testenricher.msc;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.inject.Inject;

import org.jboss.arquillian.spi.Context;
import org.jboss.arquillian.spi.TestEnricher;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.service.ServiceContainer;

/**
 * The MSC TestEnricher
 *
 * The enricher supports the injection of the {@link ServiceContainer}.
 *
 * <pre><code>
 *    @Inject
 *    ServiceContainer container;
 * </code></pre>
 *
 * @author thomas.diesler@jboss.com
 * @since 18-Nov-2010
 */
public class ServiceContainerEnricher implements TestEnricher, ServiceContainerInjector
{
   private static ServiceContainer serviceContainer;

   @Override
   public void inject(ServiceContainer value) throws InjectionException
   {
      serviceContainer = value;
   }

   @Override
   public void uninject()
   {
   }

   public void enrich(Context context, Object testCase)
   {
      Class<? extends Object> testClass = testCase.getClass();
      for (Field field : testClass.getDeclaredFields())
      {
         if (field.isAnnotationPresent(Inject.class))
         {
            if (field.getType().isAssignableFrom(ServiceContainer.class))
            {
               injectServiceContainer(context, testCase, field);
            }
         }
      }
   }

   public Object[] resolve(Context context, Method method)
   {
      return null;
   }

   private void injectServiceContainer(Context context, Object testCase, Field field)
   {
      try
      {
         field.set(testCase, serviceContainer);
      }
      catch (IllegalAccessException ex)
      {
         throw new IllegalStateException("Cannot inject ServiceContainer", ex);
      }
   }
}
