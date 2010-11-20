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
 * A logger that does nothing.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 20-Nov-2010
 */
public final class NullLogger extends Logger
{
   final static Logger INSTANCE = new NullLogger();

   // Hide ctor
   private NullLogger()
   {
   }

   @Override
   Logger newLogger(String name)
   {
      return INSTANCE;
   }

   @Override
   public void severe(String msg)
   {
   }

   @Override
   public void severe(String msg, Throwable th)
   {
   }

   @Override
   public void warning(String msg)
   {
   }

   @Override
   public void warning(String msg, Throwable th)
   {
   }

   @Override
   public void info(String msg)
   {
   }

   @Override
   public void info(String msg, Throwable th)
   {
   }

   @Override
   public void fine(String msg)
   {
   }

   @Override
   public void fine(String msg, Throwable th)
   {
   }

   @Override
   public void finer(String msg)
   {
   }

   @Override
   public void finer(String msg, Throwable th)
   {
   }

   @Override
   public void finest(String msg)
   {
   }

   @Override
   public void finest(String msg, Throwable th)
   {
   }
}
