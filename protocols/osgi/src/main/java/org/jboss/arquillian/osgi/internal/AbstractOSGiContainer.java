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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.zip.ZipInputStream;

import javax.management.MBeanServerConnection;

import org.jboss.arquillian.osgi.OSGiContainer;
import org.jboss.arquillian.osgi.RepositoryArchiveLocator;
import org.jboss.arquillian.protocol.jmx.ResourceCallbackHandler;
import org.jboss.arquillian.spi.TestClass;
import org.jboss.arquillian.spi.util.TCCLActions;
import org.jboss.osgi.spi.util.BundleInfo;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;

/**
 * An abstract {@link OSGiContainer}
 *
 * @author thomas.diesler@jboss.com
 * @author <a href="david@redhat.com">David Bosschaert</a>
 * @since 07-Sep-2010
 */
public abstract class AbstractOSGiContainer implements OSGiContainer
{
   private BundleContext context;
   private TestClass testClass;
   private ResourceCallbackHandler callbackHandler;

   protected AbstractOSGiContainer(BundleContext context, TestClass testClass, ResourceCallbackHandler callbackHandler)
   {
      this.context = context;
      this.testClass = testClass;
      this.callbackHandler = callbackHandler;
   }

   protected BundleContext getBundleContext()
   {
      return context;
   }

   public Bundle installBundle(Archive<?> archive) throws BundleException
   {
      InputStream inputStream;

      ClassLoader ctxLoader = Thread.currentThread().getContextClassLoader();
      try
      {
         // Read the archive in the context of the arquillian-osgi-bundle
         Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
         ZipExporter exporter = archive.as(ZipExporter.class);
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         exporter.exportZip(baos);

         inputStream = new ByteArrayInputStream(baos.toByteArray());
      }
      finally
      {
         Thread.currentThread().setContextClassLoader(ctxLoader);
      }

      return context.installBundle(archive.getName(), inputStream);
   }

   public Bundle installBundle(String artifactId) throws BundleException
   {
      return installBundle(null, artifactId, null);
   }

   public Bundle installBundle(String groupId, String artifactId, String version) throws BundleException
   {
      URL artifactURL = RepositoryArchiveLocator.getArtifactURL(groupId, artifactId, version);
      if (artifactURL == null)
         return null;

      // Verify that the artifact is a bundle
      BundleInfo info = BundleInfo.createBundleInfo(artifactURL);
      Bundle bundle = getBundle(info.getSymbolicName(), info.getVersion());
      if (bundle != null)
         return bundle;

      bundle = context.installBundle(artifactURL.toExternalForm());
      return bundle;
   }

   public Bundle getBundle(String symbolicName, Version version) throws BundleException
   {
      if (context == null)
         throw new IllegalArgumentException("Null context");
      if (symbolicName == null)
         throw new IllegalArgumentException("Null symbolicName");

      for (Bundle bundle : context.getBundles())
      {
         boolean artefactMatch = symbolicName.equals(bundle.getSymbolicName());
         boolean versionMatch = version == null || version.equals(bundle.getVersion());
         if (artefactMatch && versionMatch)
            return bundle;
      }
      return null;
   }

   public Archive<?> getTestArchive(String name)
   {
      InputStream input = getTestArchiveStream(name);

      ClassLoader ctxLoader = TCCLActions.getClassLoader();
      try
      {
         // Create the archive in the context of the arquillian-osgi-bundle
         TCCLActions.setClassLoader(AbstractOSGiContainer.class.getClassLoader());
         JavaArchive archive = ShrinkWrap.create(JavaArchive.class, name);
         ZipImporter zipImporter = archive.as(ZipImporter.class);
         zipImporter.importZip(new ZipInputStream(input));
         return archive;
      }
      finally
      {
         TCCLActions.setClassLoader(ctxLoader);
      }
   }

   public InputStream getTestArchiveStream(String name)
   {
      try
      {
         byte[] bytes = callbackHandler.requestResource(testClass, name);
         ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
         return bais;
      }
      catch (Exception ex)
      {
         throw new IllegalStateException("Cannot obtain test archive: " + name, ex);
      }
   }

   public abstract MBeanServerConnection getMBeanServerConnection();
}
