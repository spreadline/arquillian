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
package org.jboss.arquillian.osgi.internal;

import static org.jboss.osgi.jmx.JMXConstantsExt.DEFAULT_REMOTE_RMI_HOST;
import static org.jboss.osgi.jmx.JMXConstantsExt.DEFAULT_REMOTE_RMI_PORT;
import static org.jboss.osgi.jmx.JMXConstantsExt.REMOTE_RMI_HOST;
import static org.jboss.osgi.jmx.JMXConstantsExt.REMOTE_RMI_PORT;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.Context;

import org.jboss.arquillian.osgi.OSGiContainer;
import org.jboss.arquillian.spi.TestClass;
import org.jboss.logging.Logger;
import org.osgi.framework.BundleContext;

/**
 * The embedded version of {@link OSGiContainer}
 *
 * @author thomas.diesler@jboss.com
 * @since 07-Sep-2010
 */
public class RemoteOSGiContainer extends AbstractOSGiContainer
{
   // Provide logging
   private static final Logger log = Logger.getLogger(RemoteOSGiContainer.class);

   private JMXConnector jmxConnector;

   public RemoteOSGiContainer(BundleContext context, TestClass testClass)
   {
      super(context, testClass);
   }

   @Override
   public MBeanServerConnection getMBeanServerConnection()
   {
      String rmiHost = getFrameworkProperty(REMOTE_RMI_HOST, DEFAULT_REMOTE_RMI_HOST);
      Integer rmiPort = Integer.parseInt(getFrameworkProperty(REMOTE_RMI_PORT, DEFAULT_REMOTE_RMI_PORT)) + 100;
      String urlString = System.getProperty("arq.callback.url", "service:jmx:rmi:///jndi/rmi://" + rmiHost + ":" + rmiPort + "/jmxrmi");
      log.debug("Connecting JMXConnector to: " + urlString);
      
      Map<String, Object> env = new HashMap<String, Object>();
      env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.rmi.registry.RegistryContextFactory");
      try
      {
         // Get the MBeanServerConnection through the JMXConnector
         JMXServiceURL serviceURL = new JMXServiceURL(urlString);
         jmxConnector = JMXConnectorFactory.connect(serviceURL, env);
         return jmxConnector.getMBeanServerConnection();
      }
      catch (IOException ex)
      {
         throw new IllegalStateException("Cannot obtain MBeanServerConnection", ex);
      }
   }

   private String getFrameworkProperty(String key, String defaultValue)
   {
      String value = getBundleContext().getProperty(key);
      if (value == null)
         value = defaultValue;

      return value;
   }
}
