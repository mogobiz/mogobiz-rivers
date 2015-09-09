/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */

package com.mogobiz.rivers.cfp

import akka.actor.ActorSystem

/**
 * Core is type containing the ``system: ActorSystem`` member. This enables us to use it in our
 * apps as well as in our tests.
 */
trait CfpSystem {
  implicit def system: ActorSystem
}

/**
 * This trait implements ``System`` by starting the required ``ActorSystem`` and registering the
 * termination handler to stop the system when the JVM exits.
 */
trait BootedCfpSystem extends CfpSystem {
  /**
   * Construct the ActorSystem we will use in our application
   */
  implicit lazy val system = ActorSystem("cfp")

  /**
   * Ensure that the constructed ActorSystem is shut down when the JVM shuts down
   */
  sys.addShutdownHook(system.shutdown())
}
