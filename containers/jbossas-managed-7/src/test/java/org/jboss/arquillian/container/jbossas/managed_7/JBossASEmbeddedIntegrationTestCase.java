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
package org.jboss.arquillian.container.jbossas.managed_7;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.net.URL;

import javax.inject.Inject;

import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.container.jbossas.managed_7.SimpleServiceActivator.SimpleService;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * JBossASRemoteIntegrationTestCase
 *
 * @author Thomas.Diesler@jboss.com
 * @since 17-Nov-2010
 */
@RunWith(Arquillian.class)
public class JBossASEmbeddedIntegrationTestCase
{
   final static Logger log = Logger.getLogger(JBossASEmbeddedIntegrationTestCase.class);

   @Inject
   public ServiceContainer container;

   @Deployment
   public static JavaArchive createDeployment() throws Exception
   {
      JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "test.jar");
      archive.addClasses(SimpleServiceActivator.class);
      String path = "META-INF/services/" + ServiceActivator.class.getName();
      URL resourceURL = JBossASEmbeddedIntegrationTestCase.class.getResource("/module/" + path);
      archive.addResource(new File(resourceURL.getFile()), path);
      return archive;
   }

   @Test
   public void testDeployedService() throws Exception
   {
      assertNotNull("ServiceContainer not null", container);
      ServiceController<?> controller = container.getRequiredService(SimpleService.SERVICE_NAME);
      SimpleService service = (SimpleService)controller.getValue();
      service.add(2, 3);
   }
}
