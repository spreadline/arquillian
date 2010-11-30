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

import java.io.ByteArrayOutputStream;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.jboss.arquillian.osgi.ArchiveProvider;
import org.jboss.arquillian.protocol.jmx.JMXTestRunner;
import org.jboss.arquillian.protocol.jmx.JMXTestRunnerMBean;
import org.jboss.arquillian.protocol.jmx.RequestedCommand;
import org.jboss.arquillian.protocol.jmx.Utils;
import org.jboss.arquillian.spi.Logger;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * @author <a href="david@redhat.com">David Bosschaert</a>
 */
public class MBeanNotificationArchiveProvider implements InternalArchiveProvider, NotificationListener
{
   private static Logger log = Logger.getLogger(MBeanNotificationArchiveProvider.class);

   private final ArchiveProvider archiveProvider;
   private final MBeanServerConnection mBeanServer;
   private final JMXTestRunnerMBean testMBean;
   private final ObjectName testRunnerName;

   public MBeanNotificationArchiveProvider(MBeanServerConnection mBeanServer, ObjectName testRunnerName, ArchiveProvider archiveProvider)
   {
      try
      {
         mBeanServer.addNotificationListener(testRunnerName, this, null, null);
      }
      catch (Exception e)
      {
         // Not much point continuing, the test will fail to work properly
         throw new RuntimeException(e);
      }

      this.mBeanServer = mBeanServer;
      this.testMBean = MBeanServerInvocationHandler.newProxyInstance(mBeanServer, testRunnerName, JMXTestRunnerMBean.class, false);
      this.testRunnerName = testRunnerName;
      this.archiveProvider = archiveProvider;
   }

   @Override
   public void destroy()
   {
      try
      {
         mBeanServer.removeNotificationListener(testRunnerName, this);
      }
      catch (Exception e)
      {
         log.warning("Problem unregistering MBean Listener", e);
      }
   }

   @Override
   public void handleNotification(Notification notification, Object handback)
   {
      log.fine("Received JMX notification " + notification);

      if (JMXTestRunner.REQUEST_COMMAND.equals(notification.getType()) &&
            notification.getUserData() instanceof byte[])
      {
         try
         {
            RequestedCommand command = Utils.deserialize((byte[])notification.getUserData(), RequestedCommand.class);
            handleRequestedCommand(command);
         }
         catch (Exception e)
         {
            throw new IllegalStateException("Cannot un-marshap the JMX notification", e);
         }
      }
   }

   private void handleRequestedCommand(RequestedCommand requestedCommand)
   {
      switch (requestedCommand.getCommand())
      {
         case RESOURCE:
            handleResourceCommand(requestedCommand);
            break;
      }
   }

   private void handleResourceCommand(RequestedCommand requestedCommand)
   {
      if (requestedCommand.getArguments().length >= 1)
      {
         ClassLoader prevCl = Thread.currentThread().getContextClassLoader();
         try
         {
            Thread.currentThread().setContextClassLoader(JavaArchive.class.getClassLoader());
            JavaArchive archive = archiveProvider.getTestArchive(requestedCommand.getArguments()[0]);

            ZipExporter ze = archive.as(ZipExporter.class);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ze.exportZip(baos);
            testMBean.commandResult(requestedCommand.getId(), baos.toByteArray());
         }
         finally
         {
            Thread.currentThread().setContextClassLoader(prevCl);
         }
      }
   }
}
