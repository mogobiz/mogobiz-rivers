package com.mogobiz.rivers.cfp

import org.json4s.{DefaultFormats, Formats}
import spray.httpx.Json4sSupport

/**
 *
 * Created by smanciot on 27/08/14.
 */
object CfpJsonProtocol extends Json4sSupport{

  override implicit def json4sFormats : Formats = DefaultFormats

}
