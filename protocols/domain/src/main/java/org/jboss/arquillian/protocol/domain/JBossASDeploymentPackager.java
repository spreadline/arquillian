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
package org.jboss.arquillian.protocol.domain;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.jboss.arquillian.spi.Context;
import org.jboss.arquillian.spi.DeploymentPackager;
import org.jboss.arquillian.spi.TestClass;
import org.jboss.arquillian.spi.TestDeployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * JBossASDeploymentPackager
 *
 * @author Thomas.Diesler@jboss.com
 * @since 17-Nov-2010
 */
public class JBossASDeploymentPackager implements DeploymentPackager
{
   public Archive<?> generateDeployment(Context context, TestDeployment testDeployment)
   {
      Archive<?> appArchive = testDeployment.getApplicationArchive();
      enhanceApplicationArchive(context, JavaArchive.class.cast(appArchive));
      return appArchive;
   }

   /*
    * Add or modify the manifest such that it contains the test class and
    * defines dependencies on the arquillian modules
    */
   private void enhanceApplicationArchive(Context context, Archive<?> archive)
   {
      if (JavaArchive.class.isAssignableFrom(archive.getClass()) == false)
         throw new IllegalArgumentException("JavaArchive expected: " + archive);

      JavaArchive appArchive = JavaArchive.class.cast(archive);
      TestClass testClass = context.get(TestClass.class);
      Class<?> javaClass = testClass.getJavaClass();

      // Check if the application archive already contains the test class
      String path = javaClass.getName().replace('.', '/') + ".class";
      if (appArchive.contains(path) == false)
         appArchive.addClass(javaClass);

      final Manifest manifest = getOrCreateManifest(appArchive);
      Attributes attributes = manifest.getMainAttributes();
      String value = attributes.getValue("Dependencies");
      StringBuffer moduleDeps = new StringBuffer(value != null ? value + "," : "");
      moduleDeps.append("org.jboss.arquillian.api");
      moduleDeps.append(",org.jboss.arquillian.junit");
      moduleDeps.append(",org.jboss.shrinkwrap.api");
      moduleDeps.append(",junit.junit");
      attributes.putValue("Dependencies", moduleDeps.toString());

      // Add the manifest to the archive
      appArchive.setManifest(new Asset()
      {
         public InputStream openStream()
         {
            try
            {
               manifest.write(System.out);

               ByteArrayOutputStream baos = new ByteArrayOutputStream();
               manifest.write(baos);
               return new ByteArrayInputStream(baos.toByteArray());
            }
            catch (IOException ex)
            {
               throw new IllegalStateException("Cannot write manifest", ex);
            }
         }
      });
   }

   private Manifest getOrCreateManifest(Archive<?> archive)
   {
      Manifest manifest;
      try
      {
         Node node = archive.get("META-INF/MANIFEST.MF");
         if (node == null)
         {
            manifest = new Manifest();
            Attributes attributes = manifest.getMainAttributes();
            attributes.putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
         }
         else
         {
            manifest = new Manifest(node.getAsset().openStream());
         }
         return manifest;
      }
      catch (Exception ex)
      {
         throw new IllegalStateException("Cannot obtain manifest", ex);
      }
   }
}
