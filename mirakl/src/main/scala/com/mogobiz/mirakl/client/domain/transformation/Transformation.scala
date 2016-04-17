package com.mogobiz.mirakl.client.domain.transformation

import java.util.regex.Pattern

import com.mogobiz.mirakl.client.domain.{RoundingMode, TransformationType}

/**
  *
  * Created by smanciot on 17/04/16.
  */
trait Transformation{
  val `type`: TransformationType
  def transformation: String
}

abstract class AbstractTransformation(val `type`: TransformationType) extends Transformation{
  override def transformation = s"$`type`"
}

class UpperCase extends AbstractTransformation(TransformationType.UPPER_CASE)
class LowerCase extends AbstractTransformation(TransformationType.LOWER_CASE)
class CamelCase extends AbstractTransformation(TransformationType.CAMEL_CASE)
class Capitalize extends AbstractTransformation(TransformationType.CAPITALIZE)
class RemoveHtml extends AbstractTransformation(TransformationType.REMOVE_HTML)

case class NumericTransformation(override val `type`: TransformationType, val number: Number) extends AbstractTransformation(`type`){
  override def transformation = s"$`type`|$number"
}
case class Addition(override val number: Number) extends NumericTransformation(TransformationType.ADDITION, number)
case class Division(override val number: Number) extends NumericTransformation(TransformationType.DIVISION, number)
case class Multiplication(override val number: Number) extends NumericTransformation(TransformationType.MULTIPLICATION, number)
case class Subtraction(override val number: Number) extends NumericTransformation(TransformationType.SUBTRACTION, number)

case class Constant(text: String, empty: Boolean = true) extends AbstractTransformation(TransformationType.CONSTANT){
  override def transformation = s"$`type`|$text|$empty"
}

case class Date(inputFormat: String, outputFormat: String) extends AbstractTransformation(TransformationType.DATE){
  override def transformation = s"$`type`|$inputFormat|$outputFormat"
}

case class Regexp(pattern: Pattern, output: String) extends AbstractTransformation(TransformationType.REGEXP){
  override def transformation = s"$`type`|${pattern.pattern()}|$output"
}

case class RoundDecimal(mode: RoundingMode, precision: Int) extends AbstractTransformation(TransformationType.ROUND_DECIMAL){
  override def transformation = s"$`type`|${mode.toString}|$precision"
}
