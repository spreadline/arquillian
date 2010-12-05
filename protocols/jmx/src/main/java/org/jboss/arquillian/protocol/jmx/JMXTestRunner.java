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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.management.JMException;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.StandardEmitterMBean;

import org.jboss.arquillian.protocol.jmx.RequestedCommand.Command;
import org.jboss.arquillian.spi.TestClass;
import org.jboss.arquillian.spi.TestResult;
import org.jboss.arquillian.spi.TestResult.Status;
import org.jboss.arquillian.spi.TestRunner;
import org.jboss.arquillian.spi.util.TestRunners;

/**
 * An MBean to run test methods in container.
 *
 * @author thomas.diesler@jboss.com
 * @author <a href="david@redhat.com">David Bosschaert</a>
 * @since 06-Sep-2010
 */
public class JMXTestRunner implements JMXTestRunnerMBean, ResourceCallbackHandler
{
   // Provide logging
   private static Logger log = Logger.getLogger(JMXTestRunner.class.getName());

   public static final String REQUEST_COMMAND = "org.jboss.arquillian.protocol.jmx.request_command";

   private StandardEmitterMBean mbean;
   private final Map<Long, BlockingQueue<byte[]>> results = new ConcurrentHashMap<Long, BlockingQueue<byte[]>>();

   public interface TestClassLoader
   {
      Class<?> loadTestClass(String className) throws ClassNotFoundException;

      ClassLoader getServiceClassLoader();
   }

   public void registerMBean(MBeanServer mbeanServer) throws JMException
   {
      String[] types = { REQUEST_COMMAND };
      MBeanNotificationInfo info = new MBeanNotificationInfo(types,
            Notification.class.getName(),
            "A command request event has been emitted by this mbean");
      NotificationBroadcasterSupport emitter = new NotificationBroadcasterSupport(info);

      mbean = new StandardEmitterMBean(this, JMXTestRunnerMBean.class, emitter);
      mbeanServer.registerMBean(mbean, OBJECT_NAME);

      log.fine("JMXTestRunner registered: " + OBJECT_NAME);
   }

   public void unregisterMBean(MBeanServer mbeanServer) throws JMException
   {
      if (mbeanServer.isRegistered(OBJECT_NAME))
      {
         mbeanServer.unregisterMBean(OBJECT_NAME);
         log.fine("JMXTestRunner unregistered: " + OBJECT_NAME);
      }
   }

   protected TestClassLoader getTestClassLoader()
   {
      return new TestClassLoader()
      {
         @Override
         public Class<?> loadTestClass(String className) throws ClassNotFoundException
         {
            return getClass().getClassLoader().loadClass(className);
         }

         @Override
         public ClassLoader getServiceClassLoader()
         {
            return getClass().getClassLoader();
         }
      };
   }

   public TestResult runTestMethod(String className, String methodName, Map<String, String> props)
   {
      return runTestMethodInternal(className, methodName, props);
   }

   public InputStream runTestMethodEmbedded(String className, String methodName, Map<String, String> props)
   {
      TestResult result = runTestMethodInternal(className, methodName, props);

      // Marshall the TestResult
      try
      {
         return new ByteArrayInputStream(Utils.serialize(result));
      }
      catch (IOException ex)
      {
         throw new IllegalStateException("Cannot marshall response", ex);
      }
   }

   private TestResult runTestMethodInternal(String className, String methodName, Map<String, String> props)
   {
      try
      {
         // Associate the ExecutionType with the thread.
         // [TODO] Remove this hack when it becomes possible to pass data to the enrichers
         ResourceCallbackHandlerAssociation.setCallbackHandler(this);

         // Get the TestRunner
         ClassLoader serviceClassLoader = getTestClassLoader().getServiceClassLoader();
         TestRunner runner = TestRunners.getTestRunner(serviceClassLoader);

         ClassLoader tccl = SecurityActions.getThreadContextClassLoader();
         try
         {
            SecurityActions.setThreadContextClassLoader(serviceClassLoader);
            Class<?> testClass = getTestClassLoader().loadTestClass(className);
            TestResult testResult = runner.execute(testClass, methodName);
            return testResult;
         }
         finally
         {
            SecurityActions.setThreadContextClassLoader(tccl);
         }
      }
      catch (Throwable th)
      {
         return new TestResult(Status.FAILED, th);
      }
   }

   @Override
   public byte[] requestResource(TestClass testClass, String resourceName) throws Exception
   {
      // Create a command that describes the request.
      RequestedCommand command = new RequestedCommand(testClass.getName(), Command.RESOURCE, resourceName);

      try
      {
         // The command is asynchronously executed by the test client as it receives the notification,
         // the result will be stored in a blocking queue once its available.
         BlockingQueue<byte[]> result = new ArrayBlockingQueue<byte[]>(1);
         results.put(command.getId(), result);

         // Inform the test client of the request by sending a JMX notification
         Notification n = new Notification(REQUEST_COMMAND, mbean, command.getId(), "A command request for " + resourceName);
         n.setUserData(Utils.serialize(command));
         mbean.sendNotification(n);

         log.fine("Sent JMX notification for command request: " + command);

         // Wait for the result to appear in the result queue.
         return result.poll(1, TimeUnit.MINUTES);
      }
      finally
      {
         // Remove the command from the results map
         results.remove(command.getId());
      }
   }

   @Override
   public void commandResult(long commandId, byte[] result)
   {
      log.fine("Received result for command request with ID: " + commandId);

      // Find the associated blocking queue to put the result on.
      BlockingQueue<byte[]> resultBQ = results.get(commandId);
      if (resultBQ != null)
         resultBQ.offer(result);
   }
}
