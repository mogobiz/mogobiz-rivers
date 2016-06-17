/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */
package com.mogobiz.mirakl.client.domain.transformation

import java.util.regex.Pattern

import com.mogobiz.mirakl.client.domain.{RoundingMode, TransformationType}

/**
 *
 */
trait Transformation{
  val `type`: TransformationType
  val transformation: String
  override def toString = transformation
}

abstract class AbstractTransformation(val `type`: TransformationType) extends Transformation{
  override val transformation = s"${`type`}"
}

class UpperCase extends AbstractTransformation(TransformationType.UPPER_CASE)
class LowerCase extends AbstractTransformation(TransformationType.LOWER_CASE)
class CamelCase extends AbstractTransformation(TransformationType.CAMEL_CASE)
class Capitalize extends AbstractTransformation(TransformationType.CAPITALIZE)
class RemoveHtml extends AbstractTransformation(TransformationType.REMOVE_HTML)

class NumericTransformation(override val `type`: TransformationType, val number: Number) extends AbstractTransformation(`type`){
  override val transformation = s"${`type`}|$number"
}
case class Addition(override val number: Number) extends NumericTransformation(TransformationType.ADDITION, number)
case class Division(override val number: Number) extends NumericTransformation(TransformationType.DIVISION, number)
case class Multiplication(override val number: Number) extends NumericTransformation(TransformationType.MULTIPLICATION, number)
case class Subtraction(override val number: Number) extends NumericTransformation(TransformationType.SUBTRACTION, number)

case class Constant(text: String, empty: Boolean = true) extends AbstractTransformation(TransformationType.CONSTANT){
  override val transformation = s"${`type`}|$text|$empty"
}

case class Date(inputFormat: String, outputFormat: String) extends AbstractTransformation(TransformationType.DATE){
  override val transformation = s"${`type`}|$inputFormat|$outputFormat"
}

case class Regexp(pattern: Pattern, output: String) extends AbstractTransformation(TransformationType.REGEXP){
  override val transformation = s"${`type`}|${pattern.pattern()}|$output"
}

case class RoundDecimal(mode: RoundingMode, precision: Int) extends AbstractTransformation(TransformationType.ROUND_DECIMAL){
  override val transformation = s"${`type`}|${mode.toString}|$precision"
}
