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

import java.io.Serializable;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This command is sent to the test client via a JMX notification. The client
 * should react to it and call the {@link JMXTestRunnerMBean#commandResult(long, byte[])}
 * to provide the result.
 * 
 * @author <a href="david@redhat.com">David Bosschaert</a>
 */
public class RequestedCommand implements Serializable
{
   private static final long serialVersionUID = 1L;
   private static final AtomicLong idCounter = new AtomicLong(0L);

   // We could support various commands, currently there is only one
   public enum Command
   {
      RESOURCE
   };
   
   private final long id;
   private final String testClassName;
   private final Command command;
   private final String[] arguments;

   public RequestedCommand(String testClassName, Command command, String... arguments)
   {
      id = idCounter.incrementAndGet();
      this.testClassName = testClassName;
      this.command = command;
      this.arguments = arguments;
   }

   public long getId()
   {
      return id;
   }

   public Command getCommand()
   {
      return command;
   }

   public String[] getArguments()
   {
      return arguments;
   }

   public String getTestClassName()
   {
      return testClassName;
   }

   @Override
   public boolean equals(Object o)
   {
      if (o == this)
         return true;
      if (!(o instanceof RequestedCommand))
         return false;
      RequestedCommand rq = (RequestedCommand)o;

      return id == rq.id;
   }

   @Override
   public int hashCode()
   {
      return new Long(id).hashCode();
   }

   @Override
   public String toString()
   {
      return "(" + id + ") " + testClassName + ": " + command + " " + Arrays.toString(arguments);
   }

}
