/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */
package com.mogobiz.mirakl.client.domain.validation

import java.util.regex.Pattern

import com.mogobiz.mirakl.client.domain.ValidationType

/**
 *
 */
trait Validation {
  val `type`: ValidationType
  val validation: String
  override def toString = validation
}

abstract class AbstractValidation[T](val `type`: ValidationType, value:T) extends Validation{
  override val validation = s"${`type`}|$value"
}

case class Length(number: Long) extends AbstractValidation(ValidationType.LENGTH, number)
case class Max(number: Long) extends AbstractValidation(ValidationType.MAX, number)
case class MaxLength(number: Long) extends AbstractValidation(ValidationType.MAX_LENGTH, number)
case class Min(number: Long) extends AbstractValidation(ValidationType.MIN, number)
case class MinLength(number: Long) extends AbstractValidation(ValidationType.MIN_LENGTH, number)

case class Regexp(pattern: Pattern) extends AbstractValidation(ValidationType.REGEXP, pattern){
  override val validation = s"${`type`}|${pattern.pattern()}"
}
