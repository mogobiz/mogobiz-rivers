package com.mogobiz.mirakl.client.domain

import java.util.Date

import com.mogobiz.common.client.{BulkItem, BulkAction}
import com.mogobiz.mirakl.client.domain.transformation.Transformation
import com.mogobiz.mirakl.client.domain.validation.Validation

import scala.beans.BeanProperty

/**
  *
  * Created by smanciot on 13/04/16.
  */
trait MiraklItem {
  val code: String
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

case class MiraklItems[T <: MiraklItem](header: String, items: List[T], separator: String = ";") {

  def getBytes(charset: String): Array[Byte] = {
    toString.getBytes(charset)
  }

  override def toString: String = {
    def append(items: List[T], buffer: StringBuffer): StringBuffer = {
      items match {
        case Nil => buffer
        case x :: xs => append(xs, x.append(buffer, separator, header))
      }
    }
    append(items, new StringBuffer(String.format("\""+header.split(separator).map(_.trim).toList.mkString("\""+separator+"\"")+"\"%n"))).toString
  }
}

abstract class AbstractMiraklItem[T <: MiraklItem](
                                                    @BeanProperty val code: String,
                                                    @BeanProperty val label: String,
                                                    @BeanProperty var action: BulkAction,
                                                    @BeanProperty var parent: Option[T] = None)
  extends MiraklItem

class MiraklCategory(
                           override val code: String,
                           override val label: String,
                           action: BulkAction,
                           parent: Option[MiraklCategory] = None,
                           logisticClass: String = "")
  extends AbstractMiraklItem[MiraklCategory](code, label, action, parent) {

  def this(code: String, label: String, parent: Option[MiraklCategory]){
    this(code, label, BulkAction.UPDATE, parent)
  }

  def this(code: String, label: String){
    this(code, label, None)
  }

  override val property2Value: String => String = {
    case "category-code" => code
    case "category-label" => label
    case "logistic-class" => Option(logisticClass).getOrElse("")
    case "update-delete" => action.toString.toLowerCase()
    case "parent-code" => parent.map(_.code).getOrElse("")
    case _ => ""
  }
}

class MiraklHierarchy(
                            override val code: String,
                            override val label: String,
                            action: BulkAction,
                            parent: Option[MiraklCategory] = None)
  extends AbstractMiraklItem[MiraklCategory](code, label, action, parent) {

  def this(code: String, label: String, parent: Option[MiraklCategory]){
    this(code, label, BulkAction.UPDATE, parent)
  }

  def this(code: String, label: String){
    this(code, label, None)
  }

  def this(hierarchie: Hierarchie){
    this(hierarchie.getCode, hierarchie.getLabel, BulkAction.UPDATE, Option(hierarchie.getParentCode) match {
      case Some(s) if s.trim.length > 0 => Some(new MiraklCategory(s, ""))
      case _ => None
    })
  }

  def this(category: MiraklCategory){
    this(category.code, category.label, BulkAction.UPDATE, category.parent)
  }

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
                        parent: Option[MiraklValue] = None)
  extends AbstractMiraklItem[MiraklValue](code, label, action, parent) {

  def this(code: String, label: String, parent: Option[MiraklValue]){
    this(code, label, BulkAction.UPDATE, parent)
  }

  def this(code: String, label: String){
    this(code, label, None)
  }

  override val property2Value: String => String = {
    case "list-code" => parent.map(_.code).getOrElse("")
    case "list-label" => parent.map(_.label).getOrElse("")
    case "value-code" => code
    case "value-label" => label
    case "update-delete" => action.toString.toLowerCase()
    case _ => ""
  }
}

class MiraklAttribute(var action: BulkAction = BulkAction.UPDATE, val transformations: List[Transformation], val validations: List[Validation]) extends Attribute with MiraklItem{
  def this(transformations: List[Transformation], validations: List[Validation]){
    this(BulkAction.UPDATE, transformations, validations)
  }
  def this(attribute: Attribute){
    this(List.empty, List.empty) // TODO transformationsAsString -> transformations, validationsAsString -> validations
    setCode(attribute.getCode)
    setLabel(attribute.getLabel)
    setHierarchyCode(attribute.getHierarchyCode)
    setDescription(attribute.getDescription)
    setExample(attribute.getExample)
    setRequired(attribute.getRequired)
    setValuesList(attribute.getValuesList)
    setType(attribute.getType)
    setTypeParameter(attribute.getTypeParameter)
    setVariant(attribute.getVariant)
    setDefaultValue(attribute.getDefaultValue)
  }
  lazy val code = getCode
  lazy val label = getLabel
  override val property2Value: String => String = {
    case "code" => code
    case "label" => label
    case "hierarchy-code" => getHierarchyCode
    case "description" => Option(getDescription).getOrElse("")
    case "example" => Option(getExample).getOrElse("")
    case "required" => Option(getRequired).getOrElse(false).toString
    case "values-list" => Option(getValuesList).getOrElse("")
    case "type" => Option(getType).getOrElse(AttributeType.TEXT).toString
    case "type-parameter" => Option(getTypeParameter).getOrElse("")
    case "variant" => Option(getVariant).getOrElse(false).toString
    case "default-value" => Option(getDefaultValue).getOrElse("")
    case "transformations" => transformations.mkString(",")
    case "validations" => validations.mkString(",")
    case "update-delete" => action.toString.toLowerCase
    case _ => ""
  }
}

class MiraklProduct(val code: String, val label: String, val description:String, val category: String, val active: Option[Boolean] = None, val references:Seq[ProductReference] = Seq.empty, val skus: Seq[String] = Seq.empty, val brand: String, val url: Option[String] = None, val media: Option[String] = None, val shops: Seq[String] = Seq.empty, val group: Option[String] = None, var action: BulkAction = BulkAction.UPDATE) extends BulkItem with MiraklItem{
  override val property2Value: String => String = {
    case "product-sku" => code
    case "product-description" => description
    case "product-title" => label
    case "category-code" => category
    case "active" => active.getOrElse(true).toString
    case "product-references" => references.map{reference => s"${reference.getReferenceType}|${reference.getReference}"}.mkString(",")
    case "shop-skus" => skus.mkString(",")
    case "brand" => Option(brand).getOrElse("")
    case "update-delete" => action.toString.toLowerCase
    case "product-url" => url.getOrElse("")
    case "media-url" => media.getOrElse("")
    case "authorized-shop-ids" => shops.mkString(",")
    case "variant-group-code" => group.getOrElse("")
    case _ => ""
  }
}

class MiraklAttributeValue(val attribute: MiraklAttribute, val value: Option[String] = None)

class MiraklOffer(
                   val sku: String,
                   val productId: String,
                   val productIdType: ProductIdType = ProductIdType.SKU,
                   val description: String,
                   val price: Double,
                   val quantity: Int = 0,
                   val state: String,
                   val internalDescription: Option[String] = None,
                   val priceAdditionalInfo: Option[String] = None,
                   val minQuantityAlert: Option[Int] = None,
                   val availableStartDate: Option[Date] = None,
                   val availableEndDate: Option[Date] = None,
                   val discountPrice: Option[Double] = None,
                   val discountStartDate: Option[Date] = None,
                   val discountEndDate: Option[Date] = None,
                   val discountRanges: Option[String] = None,
                   val leadtimeToShip: Option[Int] = None,
                   var action: BulkAction = BulkAction.UPDATE,
                   val attributes: Seq[MiraklAttributeValue] = Seq.empty,
                   val product: Option[MiraklProduct] = None
                 ) extends BulkItem with MiraklItem{
  lazy val code = sku
  lazy val values: Map[String, Option[String]] = attributes.map(a => a.attribute.code -> a.value).toMap
  override val property2Value: String => String = {
    case "sku" => code
    case x if values contains x => values(x).getOrElse("")
    case _ => ""
  }
}

object MiraklApi {

  val categoriesHeader = "category-code;category-label;logistic-class;update-delete;parent-code"

  val categoriesApi = "/api/categories/synchros"

  val hierarchiesHeader = "hierarchy-code;hierarchy-label;hierarchy-parent-code;update-delete"

  val hierarchiesApi = "/api/hierarchies/imports"

  val valuesHeader = "list-code;list-label;value-code;value-label;update-delete"

  val valuesApi = "/api/values_lists/imports"

  val attributesHeader = "code;label;hierarchy-code;description;example;required;values-list;type;type-parameter;variant;default-value;transformations;validations;update-delete"

  val attributesApi = "/api/products/attributes/imports"

  val productsHeader = "product-sku;product-description;product-title;category-code;active;product-references;shop-skus;brand;update-delete;product-url;media-url;authorized-shop-ids;variant-group-code"

  val productsApi = "/api/products/synchros"

  val offersHeader = "sku;product-id;product-id-type;description;internal-description;price;price-additional-info;quantity;min-quantity-alert;state;available-start-date;available-end-date;discount-price;discount-start-date;discount-end-date;discount-ranges;leadtime-to-ship;update-delete"

  val offersApi = "/api/offers/imports"

}
