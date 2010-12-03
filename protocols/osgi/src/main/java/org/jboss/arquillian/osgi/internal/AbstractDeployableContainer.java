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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.arquillian.osgi.ArchiveProvider;
import org.jboss.arquillian.osgi.RepositoryArchiveLocator;
import org.jboss.arquillian.spi.Configuration;
import org.jboss.arquillian.spi.ContainerMethodExecutor;
import org.jboss.arquillian.spi.Context;
import org.jboss.arquillian.spi.DeployableContainer;
import org.jboss.arquillian.spi.DeploymentException;
import org.jboss.arquillian.spi.LifecycleException;
import org.jboss.arquillian.spi.TestClass;
import org.jboss.arquillian.spi.TestDeployment;
import org.jboss.shrinkwrap.api.Archive;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * An abstract OSGi {@link DeployableContainer}
 *
 * @author thomas.diesler@jboss.com
 * @author <a href="david@redhat.com">David Bosschaert</a>
 * @version $Revision: $
 */
public abstract class AbstractDeployableContainer implements DeployableContainer
{
   // Provide logging
   private static final Logger log = Logger.getLogger(AbstractDeployableContainer.class.getName());

   private BundleList supportBundles = new BundleList();

   public void setup(Context context, Configuration configuration)
   {
      log.fine("Setup OSGi Container");
   }

   public void start(Context context) throws LifecycleException
   {
      log.fine("Start OSGi Container");
   }

   public void stop(Context context) throws LifecycleException
   {
      log.fine("Stop OSGi Container");
   }

   protected void installSupportBundles()
   {
      // Install the arquillian bundle on demand
      try
      {
         if (isBundleInstalled("arquillian-osgi-bundle") == false)
         {
            BundleHandle handle = installSupportBundle("arquillian-osgi-bundle", true);
            supportBundles.add(handle);
         }
      }
      catch (RuntimeException rte)
      {
         throw rte;
      }
      catch (Exception ex)
      {
         throw new IllegalStateException("Cannot install support bundles", ex);
      }
   }

   protected void uninstallSupportBundles()
   {
      uninstallBundleList(supportBundles);
   }

   public ContainerMethodExecutor deploy(Context context, final Archive<?> archive) throws DeploymentException
   {
      TestDeployment deployment = context.get(TestDeployment.class);

      BundleList bundleList = new BundleList();
      context.add(BundleList.class, bundleList);

      // Install the application archive
      BundleHandle appHandle = installInternal(context, archive);
      bundleList.add(appHandle);

      // Install the auxiliary archives
      for (Archive<?> auxArchive : deployment.getAuxiliaryArchives())
      {
         BundleHandle auxHandle = installInternal(context, auxArchive);
         bundleList.add(auxHandle);
      }

      // Resolve the application archive
      resolveInternal(context, appHandle);

      InternalArchiveProvider archiveProvider = processArchiveProvider(context.get(TestClass.class));
      if (archiveProvider != null)
         context.add(InternalArchiveProvider.class, archiveProvider);

      return getContainerMethodExecutor();
   }

   public void undeploy(Context context, Archive<?> archive) throws DeploymentException
   {
      // Unregister ArchiveProvider
      InternalArchiveProvider archiveProvider = context.get(InternalArchiveProvider.class);
      if (archiveProvider != null)
         archiveProvider.destroy();

      BundleList bundleList = context.get(BundleList.class);
      uninstallBundleList(bundleList);
   }

   private void uninstallBundleList(BundleList bundleList)
   {
      Collections.reverse(bundleList);
      for (BundleHandle handle : bundleList)
      {
         if (getBundleState(handle) != Bundle.UNINSTALLED)
            uninstallInternal(handle);
      }
      bundleList.clear();
   }

   private BundleHandle installInternal(Context context, final Archive<?> archive) throws DeploymentException
   {
      try
      {
         log.fine("Installing bundle: " + archive.getName());
         return installBundle(archive);
      }
      catch (RuntimeException rte)
      {
         throw rte;
      }
      catch (Exception ex)
      {
         throw new DeploymentException("Cannot install bundle: " + archive.getName(), ex);
      }
   }

   private void resolveInternal(Context context, BundleHandle handle) throws DeploymentException
   {
      try
      {
         log.fine("Resolve bundle: " + handle.getSymbolicName());
         resolveBundle(handle);
      }
      catch (RuntimeException rte)
      {
         throw rte;
      }
      catch (Exception ex)
      {
         throw new DeploymentException("Cannot resolve bundle: " + handle, ex);
      }
   }

   private void uninstallInternal(BundleHandle handle)
   {
      try
      {
         log.fine("Uninstalling bundle: " + handle.getSymbolicName());
         uninstallBundle(handle);
      }
      catch (RuntimeException rte)
      {
         throw rte;
      }
      catch (Exception ex)
      {
         log.log(Level.SEVERE, "Cannot uninstall bundle: " + handle, ex);
      }
   }

   public abstract InternalArchiveProvider createInternalArchiveProvider(TestClass testClass, ArchiveProvider archiveProvider);

   public abstract ContainerMethodExecutor getContainerMethodExecutor();

   public abstract BundleHandle installBundle(Archive<?> archive) throws BundleException, IOException;

   public abstract BundleHandle installBundle(URL bundleURL) throws BundleException, IOException;

   public abstract void resolveBundle(BundleHandle handle) throws BundleException, IOException;

   public abstract void uninstallBundle(BundleHandle handle) throws BundleException, IOException;

   public abstract int getBundleState(BundleHandle handle);

   public abstract void startBundle(BundleHandle handle) throws BundleException;

   public abstract void stopBundle(BundleHandle handle) throws BundleException;

   public abstract boolean isBundleInstalled(String symbolicName);

   private BundleHandle installSupportBundle(String artifactId, boolean startBundle) throws BundleException, IOException
   {
      URL artifactURL = RepositoryArchiveLocator.getArtifactURL(null, artifactId, null);
      if (artifactURL != null)
      {
         BundleHandle handle = installSupportFile(artifactURL, startBundle);
         return handle;
      }
      return null;
   }

   private BundleHandle installSupportFile(URL bundleURL, boolean startBundle) throws BundleException, IOException
   {
      BundleHandle handle = installBundle(bundleURL);
      if (startBundle == true)
         startBundle(handle);
      return handle;
   }

   private InternalArchiveProvider processArchiveProvider(TestClass testClass)
   {
      for (Class<?> innerClass : testClass.getJavaClass().getClasses())
      {
         if (ArchiveProvider.class.isAssignableFrom(innerClass))
         {
            try
            {
               ArchiveProvider archiveProvider = (ArchiveProvider)innerClass.newInstance();
               return createInternalArchiveProvider(testClass, archiveProvider);
            }
            catch (Exception ex)
            {
               log.severe("Cannot register: " + innerClass.getName());
            }
         }
      }
      return null;
   }
   
   public static class BundleHandle
   {
      private long bundleId;
      private String symbolicName;

      public BundleHandle(long bundleId, String symbolicName)
      {
         this.bundleId = bundleId;
         this.symbolicName = symbolicName;
      }

      public long getBundleId()
      {
         return bundleId;
      }

      public String getSymbolicName()
      {
         return symbolicName;
      }

      @Override
      public String toString()
      {
         return "[" + bundleId + "]" + symbolicName;
      }
   }

   @SuppressWarnings("serial")
   static class BundleList extends ArrayList<BundleHandle>
   {
   }
}
