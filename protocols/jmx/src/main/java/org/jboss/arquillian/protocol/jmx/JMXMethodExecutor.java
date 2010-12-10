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
package org.jboss.arquillian.protocol.jmx;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.jboss.arquillian.api.ArchiveProvider;
import org.jboss.arquillian.spi.ContainerMethodExecutor;
import org.jboss.arquillian.spi.TestMethodExecutor;
import org.jboss.arquillian.spi.TestResult;
import org.jboss.arquillian.spi.TestResult.Status;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * JMXMethodExecutor
 *
 * @author thomas.diesler@jboss.com
 * @author <a href="david@redhat.com">David Bosschaert</a>
 * @since 06-Sep-2010
 */
public class JMXMethodExecutor implements ContainerMethodExecutor
{
   // Provide logging
   private static final Logger log = Logger.getLogger(JMXMethodExecutor.class.getName());

   private final MBeanServerConnection mbeanServer;
   private final ExecutionType executionType;
   private final Map<String, String> props;

   public enum ExecutionType
   {
      EMBEDDED, REMOTE
   }

   public JMXMethodExecutor(MBeanServerConnection connection, ExecutionType executionType)
   {
      this.mbeanServer = connection;
      this.executionType = executionType;
      this.props = new HashMap<String, String>();
      props.put(ExecutionType.class.getName(), executionType.toString());
   }

   public TestResult invoke(TestMethodExecutor testMethodExecutor)
   {
      if (testMethodExecutor == null)
         throw new IllegalArgumentException("TestMethodExecutor null");

      Object testInstance = testMethodExecutor.getInstance();
      String testClass = testInstance.getClass().getName();
      String testMethod = testMethodExecutor.getMethod().getName();

      TestResult result = null;
      NotificationListener listener = null;
      try
      {
         JMXTestRunnerMBean testRunner = getMBeanProxy(JMXTestRunnerMBean.OBJECT_NAME, JMXTestRunnerMBean.class);
         listener = registerNotificationListener(JMXTestRunnerMBean.OBJECT_NAME, testRunner, testInstance);

         if (executionType == ExecutionType.EMBEDDED)
         {
            InputStream resultStream = testRunner.runTestMethodEmbedded(testClass, testMethod, props);
            result = Utils.deserialize(resultStream, TestResult.class);
         }
         else if (executionType == ExecutionType.REMOTE)
         {
            result = testRunner.runTestMethod(testClass, testMethod, props);
         }
      }
      catch (final Throwable e)
      {
         result = new TestResult(Status.FAILED);
         result.setThrowable(e);
      }
      finally
      {
         result.setEnd(System.currentTimeMillis());

         unregisterNotificationListener(JMXTestRunnerMBean.OBJECT_NAME, listener);
      }
      return result;
   }

   private NotificationListener registerNotificationListener(ObjectName name, final JMXTestRunnerMBean testRunner, Object testInstance)
   {
      final Method apMethod = findAnnotatedMethod(testInstance.getClass(), ArchiveProvider.class);         
      if (apMethod == null)
         return null;

      log.fine("Found ArchiveProvider method: " + apMethod);
      if (!Modifier.isStatic(apMethod.getModifiers()))
         throw new IllegalStateException("Non-static ArchiveProvider on " + apMethod);
      if (!Archive.class.isAssignableFrom(apMethod.getReturnType()))
         throw new IllegalStateException("ArchiveProvider annotated method should return an instance of " + Archive.class + " :" + apMethod);
      if (!Arrays.equals(new Class[] { String.class }, apMethod.getParameterTypes()))
         throw new IllegalStateException("ArchiveProvider annotated method should take String parameter: " + apMethod);

      NotificationListener nl = new NotificationListener()
      {         
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
                  handleRequestedCommand(testRunner, apMethod, command);
               }
               catch (Exception e)
               {
                  throw new IllegalStateException("Cannot un-marshal the JMX RequestedCommand notification", e);
               }
            }
            else
               log.warning("Ignored unrecognized notification: " + notification);
         }
      };
      try
      {
         mbeanServer.addNotificationListener(name, nl, null, null);
         log.fine("Registered JMX Notification Listener for " + name);
         return nl;
      }
      catch (Exception e)
      {
         throw new IllegalStateException("Unable to register JMX notification listener for: " + name);
      }
   }

   private void unregisterNotificationListener(ObjectName name, NotificationListener listener)
   {
      try
      {
         mbeanServer.removeNotificationListener(name, listener);
      }
      catch (Exception e)
      {
         log.warning("Problem removing notification listener from MBean " + name);
      }
   }

   private void handleRequestedCommand(JMXTestRunnerMBean testRunner, Method apMethod, RequestedCommand requestedCommand)
   {
      switch (requestedCommand.getCommand())
      {
         case RESOURCE:
            handleResourceCommand(testRunner, requestedCommand, apMethod);
            break;
      }
   }

   private void handleResourceCommand(JMXTestRunnerMBean testRunner, RequestedCommand requestedCommand, Method apMethod)
   {
      if (requestedCommand.getArguments().length >= 1)
      {
         ClassLoader prevCl = Thread.currentThread().getContextClassLoader();
         try
         {
            Thread.currentThread().setContextClassLoader(JavaArchive.class.getClassLoader());
            Archive<?> archive = (Archive<?>)apMethod.invoke(null, requestedCommand.getArguments()[0]);

            ZipExporter ze = archive.as(ZipExporter.class);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ze.exportZip(baos);
            testRunner.commandResult(requestedCommand.getId(), baos.toByteArray());
         }
         catch (Exception e)
         {
            throw new RuntimeException(e);
         }
         finally
         {
            Thread.currentThread().setContextClassLoader(prevCl);
         }
      }
   }

   private <T> T getMBeanProxy(ObjectName name, Class<T> interf)
   {
      return (T)MBeanServerInvocationHandler.newProxyInstance(mbeanServer, name, interf, false);
   }

   private static Method findAnnotatedMethod(Class<?> cls, Class<? extends Annotation> annotation)
   {
      Method[] methods = cls.getMethods();
      for (Method method : methods)
      {
         if (method.isAnnotationPresent(annotation))
         {
            return method;
         }
      }
      return null;
   }
}