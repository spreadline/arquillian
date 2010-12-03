/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.arquillian.protocol.jmx;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServer;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.management.remote.rmi.RMIJRMPServerImpl;
import javax.management.remote.rmi.RMIServerImpl;

/**
 * A RMI/JRMP connector service.
 * 
 * @author thomas.diesler@jboss.com
 * @since 24-Apr-2009
 */
public class JMXConnectorServerExt
{
   // Provide logging
   private static final Logger log = Logger.getLogger(JMXConnectorServerExt.class.getName());

   private JMXServiceURL serviceURL;
   private RMIConnectorServer connectorServer;
   private boolean shutdownRegistry;
   private Registry rmiRegistry;

   public JMXConnectorServerExt(JMXServiceURL serviceURL, int regPort) throws IOException
   {
      this.serviceURL = serviceURL;

      String host = serviceURL.getHost();
      
      // Check to see if registry already created
      rmiRegistry = LocateRegistry.getRegistry(host, regPort);
      try
      {
         rmiRegistry.list();
         log.fine("RMI registry running at host=" + host + ",port=" + regPort);
      }
      catch (Exception ex)
      {
         log.fine("No RMI registry running at host=" + host + ",port=" + regPort + ".  Will create one.");
         rmiRegistry = LocateRegistry.createRegistry(regPort, null, new DefaultSocketFactory(InetAddress.getByName(host)));
         shutdownRegistry = true;
      }
   }

   public void start(MBeanServer mbeanServer) throws IOException
   {
      String rmiHost = serviceURL.getHost();
      int rmiPort = serviceURL.getPort();
      
      // create new connector server and start it
      RMIServerSocketFactory serverSocketFactory = new DefaultSocketFactory(InetAddress.getByName(rmiHost));
      RMIServerImpl rmiServer = new RMIJRMPServerImpl(rmiPort, null, serverSocketFactory, null);
      connectorServer = new RMIConnectorServer(serviceURL, null, rmiServer, mbeanServer);
      log.fine("JMXConnectorServer created: " + serviceURL);

      connectorServer.start();
      rmiRegistry.rebind("jmxrmi", rmiServer.toStub());
      log.fine("JMXConnectorServer started: " + serviceURL);
   }

   public void stop()
   {
      try
      {
         if (connectorServer != null)
         {
            connectorServer.stop();
            rmiRegistry.unbind("jmxrmi");
         }

         // Shutdown the registry if this service created it
         if (shutdownRegistry == true)
         {
            log.fine("Shutdown RMI registry");
            UnicastRemoteObject.unexportObject(rmiRegistry, true);
         }

         log.fine("JMXConnectorServer stopped");
      }
      catch (Exception ex)
      {
         log.log(Level.WARNING, "Cannot stop JMXConnectorServer", ex);
      }
   }
}