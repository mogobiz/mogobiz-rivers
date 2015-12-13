/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */
package com.mogobiz.elasticsearch.client

import com.mogobiz.elasticsearch._

/**
 *
 */
trait EsTokenizer extends EsProperty

case class StandardTokenizer(override val id:String, maxTokenLength:Int) extends EsTokenizer{
  override lazy val build = Map("type" -> "standard", "max_token_length" -> maxTokenLength)
}

case class PatternTokenizer(override val id:String, pattern:String, group:Int = -1) extends EsTokenizer{
  override lazy val build = Map("pattern" -> pattern, "group" -> group)
}


