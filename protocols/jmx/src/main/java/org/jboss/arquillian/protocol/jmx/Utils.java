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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * @author <a href="david@redhat.com">David Bosschaert</a>
 */
public class Utils
{
   private Utils()
   {
      // Cannot be instatiated
   }

   /**
    * Serialize an object into an array of bytes using Java Serialization.
    * @param obj the object to be serialized.
    * @return the bytes representing the object.
    * @throws IOException when an IO Exception occurs.
    */
   public static byte[] serialize(Serializable obj) throws IOException
   {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(obj);
      oos.close();

      return baos.toByteArray();
   }

   /**
    * Deserialize an array of bytes back into an object using Java Serialization.
    * @param bytes the bytes to be serialized.
    * @param cls the class of the desired object.
    * @return the reconstituted object.
    * @throws IOException when an IO Exception occurs.
    * @throws ClassNotFoundException if a class cannot be loaded during deserialization.
    */
   public static <T> T deserialize(byte[] bytes, Class<T> cls) throws IOException, ClassNotFoundException
   {
      ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
      return deserialize(bais, cls);
   }

   /**
    * Deserialize an object from an {@link InputStream} using Java Serialization.
    * @param is the input stream to read the object from.
    * @param cls the class of the desired object.
    * @return the reconstituted object.
    * @throws IOException when an IO Exception occurs.
    * @throws ClassNotFoundException if a class cannot be loaded during deserialization.
    */
   public static <T> T deserialize(InputStream is, Class<T> cls) throws IOException, ClassNotFoundException
   {
      ObjectInputStream ois = new ObjectInputStream(is);
      Object obj = ois.readObject();
      return cls.cast(obj);
   }
}
