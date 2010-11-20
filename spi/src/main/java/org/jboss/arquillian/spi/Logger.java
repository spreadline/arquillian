/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.jboss.arquillian.spi;


/**
 * A logging abstraction for Arquillian.
 *
 * Some containers (i.e. JBossAS7) may want to initialize the logging
 * system at boot time.
 *
 * Arquillian logging can be configured with
 *
 * <code>
 * -Darquillian.logging=false|off
 * -Darquillian.logging=system
 * -Darquillian.logging=jdk
 * </code>
 *
 * by default logging is redirected to java.util.logging
 *
 * @author Thomas.Diesler@jboss.com
 * @since 20-Nov-2010
 */
public abstract class Logger
{
   public final static String ARQUILLIAN_LOGGING = "arquillian.logging";

   private final static Logger delegate;
   static
   {
      String value = System.getProperty(ARQUILLIAN_LOGGING);
      if ("false".equals(value) || "off".equals(value))
         delegate = NullLogger.INSTANCE;
      else if ("system".equals(value))
         delegate = SystemLogger.INSTANCE;
      else
         delegate = JDKLogger.INSTANCE;
   }

   public static Logger getLogger(String name)
   {
      return delegate.newLogger(name);
   }

   public static Logger getLogger(Class<?> clazz)
   {
      return delegate.newLogger(clazz.getName());
   }

   // package protected
   abstract Logger newLogger(String name);

   public abstract void severe(String msg);

   public abstract void severe(String msg, Throwable th);

   public abstract void warning(String msg);

   public abstract void warning(String msg, Throwable th);

   public abstract void info(String msg);

   public abstract void info(String msg, Throwable th);

   public abstract void fine(String msg);

   public abstract void fine(String msg, Throwable th);

   public abstract void finer(String msg);

   public abstract void finer(String msg, Throwable th);

   public abstract void finest(String msg);

   public abstract void finest(String msg, Throwable th);
}
