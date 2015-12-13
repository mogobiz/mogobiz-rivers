/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */

package com.mogobiz.rivers.cfp

import org.json4s.{DefaultFormats, Formats}
import spray.httpx.Json4sSupport

/**
 *
 */
object CfpJsonProtocol extends Json4sSupport{

  override implicit def json4sFormats : Formats = DefaultFormats

}
