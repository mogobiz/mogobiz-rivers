package com.mogobiz.mirakl.client.domain

import com.mogobiz.common.client.BulkAction
import com.mogobiz.mirakl.client.domain.transformation.Transformation
import com.mogobiz.mirakl.client.domain.validation.Validation

import scala.beans.BeanProperty

/**
  *
  * Created by smanciot on 13/04/16.
  */
trait MiraklItem { // TODO extends BulkItem
  val code: String
  val label: String
  var action: BulkAction
//  val id: String = code
  def append(buffer: StringBuffer, separator: String, header: String): StringBuffer = {
    def fields(properties: List[String], values: List[String]): List[String] = {
      properties match {
        case Nil => values
        case x :: xs => fields(xs, values :+ property2Value(x))
      }
    }
    buffer.append(String.format(fields(header.split(separator).map(_.trim.toLowerCase).toList, List.empty).mkString(separator)+"%n"))
  }
  val property2Value: String => String = {
    case _ => ""
  }
}

case class MiraklItems[T <: MiraklItem](header: String, items: List[T]) {

  def getBytes(charset: String, separator: String = ";"): Array[Byte] = {
    def append(items: List[T], buffer: StringBuffer): StringBuffer = {
      items match {
        case Nil => buffer
        case x :: xs => append(xs, x.append(buffer, separator, header))
      }
    }
    val str = append(items, new StringBuffer(String.format("\""+header.split(separator).map(_.trim).toList.mkString("\""+separator+"\"")+"\"%n"))).toString
    str.getBytes(charset)
  }

}

abstract class AbstractMiraklItem[T <: MiraklItem](
                                                    @BeanProperty val code: String,
                                                    @BeanProperty val label: String,
                                                    @BeanProperty var action: BulkAction,
                                                    @BeanProperty var parent: Option[T])
  extends MiraklItem

class MiraklCategory(
                           override val code: String,
                           override val label: String,
                           action: BulkAction,
                           parent: Option[MiraklCategory],
                           logisticClass: String = "")
  extends AbstractMiraklItem[MiraklCategory](code, label, action, parent) {

  def this(code: String, label: String, parent: Option[MiraklCategory]){
    this(code, label, BulkAction.UPDATE, parent)
  }

  def this(code: String, label: String){
    this(code, label, None)
  }

  //"category-code;category-label;logistic-class;update-delete;parent-code"
  override val property2Value: String => String = {
    case "category-code" => code
    case "category-label" => label
    case "logistic-class" => logisticClass
    case "update-delete" => action.toString.toLowerCase()
    case "parent-code" => parent.map(_.code).getOrElse("")
    case _ => ""
  }
}

class MiraklHierarchy(
                            override val code: String,
                            override val label: String,
                            action: BulkAction,
                            parent: Option[MiraklCategory])
  extends AbstractMiraklItem[MiraklCategory](code, label, action, parent) {

  def this(code: String, label: String, parent: Option[MiraklCategory]){
    this(code, label, BulkAction.UPDATE, parent)
  }

  def this(code: String, label: String){
    this(code, label, None)
  }

  // hierarchy-code;hierarchy-label;hierarchy-parent-code;update-delete
  override val property2Value: String => String = {
    case "hierarchy-code" => code
    case "hierarchy-label" => label
    case "hierarchy-parent-code" => parent.map(_.code).getOrElse("")
    case "update-delete" => action.toString.toLowerCase()
    case _ => ""
  }

}

class MiraklValue(
                        override val code: String,
                        override val label: String,
                        action: BulkAction,
                        parent: Option[MiraklValue])
  extends AbstractMiraklItem[MiraklValue](code, label, action, parent) {

  def this(code: String, label: String, parent: Option[MiraklValue]){
    this(code, label, BulkAction.UPDATE, parent)
  }

  def this(code: String, label: String){
    this(code, label, None)
  }

  // list-code;list-label;value-code";value-label";update-delete
  override val property2Value: String => String = {
    case "list-code" => parent.map(_.code).getOrElse("")
    case "list-label" => parent.map(_.label).getOrElse("")
    case "value-code" => code
    case "value-label" => label
    case "update-delete" => action.toString.toLowerCase()
    case _ => ""
  }
}

class MiraklAttribute(var action: BulkAction, val transformations: List[Transformation], val validations: List[Validation]) extends Attribute with MiraklItem{
  lazy val code = getCode
  lazy val label = getLabel
  //code;label;hierarchy-code;description;example;required;values-list;type;type-parameter;variant;default-value;transformations;validations;action
  override val property2Value: String => String = {
    case "code" => code
    case "label" => label
    case "hierarchy-code" => getHierarchyCode
    case "description" => getDescription
    case "example" => getExample
    case "required" => getRequired.toString
    case "values-list" => getValuesList
    case "type" => getType.toString
    case "type-parameter" => getTypeParameter
    case "variant" => getVariant.toString
    case "default-value" => getDefaultValue
    case "transformations" => transformations.mkString(",")
    case "validations" => validations.mkString(",")
    case "update-delete" => action.toString.toLowerCase
    case _ => ""
  }
}
