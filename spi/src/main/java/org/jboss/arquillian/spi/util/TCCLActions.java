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
package org.jboss.arquillian.spi.util;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Privileged actions for the thread context ClassLoader.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 29-Oct-2010
 */
public final class TCCLActions {

    // Hide ctor
    private TCCLActions() {
    }

    /**
     * Get the thread context class loader
     * @return The class loader associated with the current thread
     */
    public static ClassLoader getClassLoader() {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
                Thread currentThread = Thread.currentThread();
                return currentThread.getContextClassLoader();
            }
        });
    }

    /**
     * Set the thread context class loader
     * @return The class loader previously associated with the current thread
     */
    public static ClassLoader setClassLoader(final ClassLoader classLoader) {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
                Thread currentThread = Thread.currentThread();
                ClassLoader current = currentThread.getContextClassLoader();
                currentThread.setContextClassLoader(classLoader);
                return current;
            }
        });
    }
}