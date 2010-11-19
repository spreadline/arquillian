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
package org.jboss.arquillian.container.jbossas.embedded_7;

import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * SimpleServiceActivator
 *
 * @author Thomas.Diesler@jboss.com
 * @since 17-Nov-2010
 */
public class SimpleServiceActivator implements ServiceActivator
{
   private final static Logger log = Logger.getLogger(SimpleServiceActivator.class);

   @Override
   public void activate(ServiceActivatorContext context)
   {
      log.infof("Activating: %s", context);
      context.getBatchBuilder().addService(SimpleService.SERVICE_NAME, new SimpleService());
   }

   static class SimpleService implements Service<SimpleService>
   {
      static ServiceName SERVICE_NAME = ServiceName.JBOSS.append("test", "simple");

      public int add(int a, int b)
      {
         log.infof("Add: %d + %d", a, b);
         return a + b;
      }

      @Override
      public void start(StartContext context) throws StartException
      {
         log.infof("Start service: %s", SERVICE_NAME);
      }

      @Override
      public void stop(StopContext context)
      {
         log.infof("Stop service: %s", SERVICE_NAME);
      }

      @Override
      public SimpleService getValue() throws IllegalStateException
      {
         return this;
      }
   }
}
