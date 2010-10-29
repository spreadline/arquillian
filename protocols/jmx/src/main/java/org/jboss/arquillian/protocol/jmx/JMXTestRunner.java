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
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Properties;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jboss.arquillian.protocol.jmx.JMXMethodExecutor.ExecutionType;
import org.jboss.arquillian.spi.TestResult;
import org.jboss.arquillian.spi.TestResult.Status;
import org.jboss.arquillian.spi.TestRunner;
import org.jboss.arquillian.spi.util.TestRunners;
import org.jboss.logging.Logger;

/**
 * An MBean to run test methods in container.
 *
 * @author thomas.diesler@jboss.com
 * @version $Revision: $
 */
public class JMXTestRunner implements JMXTestRunnerMBean
{
   // Provide logging
   private static Logger log = Logger.getLogger(JMXTestRunner.class);

   private static ThreadLocal<ExecutionType> executionTypeAssociation = new ThreadLocal<ExecutionType>();
   private static ThreadLocal<MBeanServer> mbeanServerAssociation = new ThreadLocal<MBeanServer>();
   private final MBeanServer mbeanServer;
   private TestClassLoader testClassLoader;
   
   public interface TestClassLoader
   {
      Class<?> loadTestClass(String className) throws ClassNotFoundException;
   }

   public JMXTestRunner(MBeanServer mbeanServer, TestClassLoader classLoader)
   {
      this.mbeanServer = mbeanServer;
      this.testClassLoader = classLoader;
      
      // Initialize the default TestClassLoader
      if (testClassLoader == null)
      {
         testClassLoader = new TestClassLoader()
         {
            public Class<?> loadTestClass(String className) throws ClassNotFoundException
            {
               ClassLoader classLoader = JMXTestRunner.class.getClassLoader();
               return classLoader.loadClass(className);
            }
         };
      }
   }

   public static ExecutionType getExecutionType()
   {
      return executionTypeAssociation.get();
   }

   public static MBeanServer getThreadContextMBeanServer()
   {
      return mbeanServerAssociation.get();
   }

   public ObjectName registerMBean() throws JMException
   {
      ObjectName oname = new ObjectName(JMXTestRunnerMBean.OBJECT_NAME);
      mbeanServer.registerMBean(this, oname);
      log.debug("JMXTestRunner registered: " + oname);
      return oname;
   }

   public void unregisterMBean() throws JMException
   {
      ObjectName oname = new ObjectName(JMXTestRunnerMBean.OBJECT_NAME);
      if (mbeanServer.isRegistered(oname))
      {
         mbeanServer.unregisterMBean(oname);
         log.debug("JMXTestRunner unregistered: " + oname);
      }
   }

   public TestResult runTestMethod(String className, String methodName, Properties props)
   {
      return runTestMethodInternal(className, methodName, props);
   }

   public byte[] runTestMethodSerialized(String className, String methodName, Properties props)
   {
      TestResult result = runTestMethodInternal(className, methodName, props);

      // Marshall the TestResult
      try
      {
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         ObjectOutputStream oos = new ObjectOutputStream(baos);
         oos.writeObject(result);
         oos.close();

         return baos.toByteArray();
      }
      catch (IOException ex)
      {
         throw new IllegalStateException("Cannot marshall response", ex);
      }
   }

   private TestResult runTestMethodInternal(String className, String methodName, Properties props)
   {
      if (props != null)
      {
         // This hack associates the ExecutionType with the current thread
         // such that the OSGitestEnricher can pick it up and know whether we run embedded or remote
         ExecutionType executionType = (ExecutionType)props.get(ExecutionType.class);
         executionTypeAssociation.set(executionType);
      }
      
      try
      {
         // This hack associates the MBeanServer that this runner is registered with
         // with the current Thread so that other components (e.g. OSGiTestEnricher)
         // are gurantied to use the same MBeanServer. This issue showed up in AS7.
         mbeanServerAssociation.set(mbeanServer);
         
         TestRunner runner = TestRunners.getTestRunner(JMXTestRunner.class.getClassLoader());
         Class<?> testClass = testClassLoader.loadTestClass(className);
         
         TestResult testResult = runner.execute(testClass, methodName);
         return testResult;
      }
      catch (Throwable th)
      {
         return new TestResult(Status.FAILED, th);
      }
      finally
      {
         mbeanServerAssociation.remove();
      }
   }
}
