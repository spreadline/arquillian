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

import java.util.logging.Level;


/**
 * A logger that uses {@link java.util.logging.Logger}.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 20-Nov-2010
 */
public final class JDKLogger extends Logger
{
   final static Logger INSTANCE = new JDKLogger();

   private java.util.logging.Logger delegate;

   // Hide ctor
   private JDKLogger()
   {
   }

   // Hide ctor
   private JDKLogger(String name)
   {
      delegate = java.util.logging.Logger.getLogger(name);
   }

   @Override
   Logger newLogger(String name)
   {
      return new JDKLogger(name);
   }

   @Override
   public void severe(String msg)
   {
      delegate.log(Level.SEVERE, msg);
   }

   @Override
   public void severe(String msg, Throwable th)
   {
      delegate.log(Level.SEVERE, msg, th);
   }

   @Override
   public void warning(String msg)
   {
      delegate.log(Level.WARNING, msg);
   }

   @Override
   public void warning(String msg, Throwable th)
   {
      delegate.log(Level.WARNING, msg, th);
   }

   @Override
   public void info(String msg)
   {
      delegate.log(Level.INFO, msg);
   }

   @Override
   public void info(String msg, Throwable th)
   {
      delegate.log(Level.INFO, msg, th);
   }

   @Override
   public void fine(String msg)
   {
      delegate.log(Level.FINE, msg);
   }

   @Override
   public void fine(String msg, Throwable th)
   {
      delegate.log(Level.FINE, msg, th);
   }

   @Override
   public void finer(String msg)
   {
      delegate.log(Level.FINER, msg);
   }

   @Override
   public void finer(String msg, Throwable th)
   {
      delegate.log(Level.FINER, msg, th);
   }

   @Override
   public void finest(String msg)
   {
      delegate.log(Level.FINEST, msg);
   }

   @Override
   public void finest(String msg, Throwable th)
   {
      delegate.log(Level.FINEST, msg, th);
   }
}
