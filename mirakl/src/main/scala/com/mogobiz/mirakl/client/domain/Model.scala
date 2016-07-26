/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */

package com.mogobiz.mirakl.client.domain

import java.text.SimpleDateFormat
import java.util.Date

import com.mogobiz.common.client.{BulkItem, BulkAction}
import com.mogobiz.mirakl.client.domain.transformation.Transformation
import com.mogobiz.mirakl.client.domain.validation.Validation
import com.mogobiz.tools.CsvLine

import scala.beans.BeanProperty

/**
 *
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

import scala.collection.JavaConverters._

class MiraklReportItem(val lineNumber: Long, val keys: List[String], val fields: Map[String, String], val errorMessage: Option[String] = None) extends CsvLine with MiraklItem {
  def this(line: CsvLine, errorMessage: Option[String]){
    this(
      line.getNumber.toLong,
      line.getKeys.toList ++ List("error-line-number", "error-message"),
      line.getFields.asScala.toMap,
      errorMessage
    )
  }
  def this(line: CsvLine){
    this(line, None)
  }
  override val code: String = ""
  override var action: BulkAction = BulkAction.UPDATE
  override val property2Value: String => String = {
    case "error-line-number" if errorMessage.isDefined => "\""+s"$lineNumber"+"\""
    case "error-message" => "\""+errorMessage.getOrElse("")+"\""
    case key if fields.contains(key) => "\""+Option(fields(key)).getOrElse("")+"\""
    case _ => "\"\""
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
                           logisticClass: String = "",
                           val uuid: String = ""
                    )
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

class MiraklAttributeValue(val attribute: String, val value: Option[String] = None)

class MiraklProduct(val code: String, val label: String, val description: Option[String], val category: String, val active: Option[Boolean] = None, val references: Seq[ProductReference] = Seq.empty, val shopSkus: Seq[String] = Seq.empty, val brand: Option[String], val url: Option[String] = None, val media: Option[String] = None, val authorizedShops: Seq[String] = Seq.empty, val variantGroupCode: Option[String] = None, val logisticClass: Option[String] = None, var action: BulkAction = BulkAction.UPDATE, val attributes: Seq[MiraklAttributeValue] = Seq.empty) extends BulkItem with MiraklItem{
  lazy val values: Map[String, Option[String]] = attributes.map(a => a.attribute -> a.value).toMap
  override val property2Value: String => String = {
    case "product-sku" | MiraklApi.identifier => code // mogobiz ticketType uuid
    case "product-description" | MiraklApi.description => description.getOrElse("")
    case "product-title" | MiraklApi.title => label
    case "category-code" | MiraklApi.category => category
    case "active" => active.getOrElse(true).toString
    case "product-references"  | MiraklApi.productReferences => references.map{reference => s"${reference.getReferenceType}|${reference.getReference}"}.mkString(",")
    case "shop-skus" => shopSkus.mkString(",")
    case "brand" | MiraklApi.brand => brand.getOrElse("")
    case "update-delete" => action.toString.toLowerCase
    case "product-url" => url.getOrElse("")
    case "media-url" | MiraklApi.media => media.getOrElse("")
    case "authorized-shop-ids" => authorizedShops.mkString(",")
    case "variant-group-code" | MiraklApi.variantIdentifier => variantGroupCode.getOrElse("") //mogobiz product uuid
    case "logistic-class" => logisticClass.getOrElse("")
    case x if values contains x => values(x).getOrElse("")
    case _ => ""
  }
}

class MiraklOffer(
                   val sku: String,
                   val productId: String,
                   val productIdType: ProductIdType = ProductIdType.SKU,
                   val description: Option[String],
                   val price: Long,
                   val quantity: Option[Long] = None,
                   val state: String,
                   val internalDescription: Option[String] = None,
                   val priceAdditionalInfo: Option[String] = None,
                   val minQuantityAlert: Option[Long] = None,
                   val availableStartDate: Option[Date] = None,
                   val availableEndDate: Option[Date] = None,
                   val discountPrice: Option[Long] = None,
                   val discountStartDate: Option[Date] = None,
                   val discountEndDate: Option[Date] = None,
                   val discountRanges: Option[String] = None,
                   val leadtimeToShip: Option[Int] = None,
                   var action: BulkAction = BulkAction.UPDATE,
                   val product: Option[MiraklProduct] = None
                 ) extends BulkItem with MiraklItem{
  lazy val code = sku
  def format(d: Date) = new SimpleDateFormat("yyyy-MM-dd").format(d)
  def format(l: Long) = (l.toDouble / 100.0).toString
  override val property2Value: String => String = {
    case "sku" => code
    case "product-id" => productId
    case "product-id-type" => productIdType.toString
    case "description" => description.getOrElse("")
    case "internal-description" => internalDescription.getOrElse("")
    case "price" => format(price)
    case "price-additional-info" => priceAdditionalInfo.getOrElse("")
    case "quantity" => s"${quantity.getOrElse("")}"
    case "min-quantity-alert" => s"${minQuantityAlert.getOrElse("")}"
    case "state" => state
    case "available-start-date" => availableStartDate.map(format).getOrElse("")
    case "available-end-date" => availableEndDate.map(format).getOrElse("")
    case "discount-price" => discountPrice.map(format).getOrElse("")
    case "discount-start-date" => discountStartDate.map(format).getOrElse("")
    case "discount-end-date" => discountEndDate.map(format).getOrElse("")
    case "discount-ranges" => discountRanges.getOrElse("")
    case "leadtime-to-ship" => s"${leadtimeToShip.getOrElse("")}"
    case "update-delete" => action.toString.toLowerCase
    case y if product.isDefined => product.get.property2Value(y)
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

  val productsHeader = "product-sku;product-description;product-title;category-code;active;product-references;shop-skus;brand;update-delete;product-url;media-url;authorized-shop-ids;variant-group-code;logistic-class"

  val productsApi = "/api/products/synchros"

  val offersHeader = "sku;product-id;product-id-type;description;internal-description;price;price-additional-info;quantity;min-quantity-alert;state;available-start-date;available-end-date;discount-price;discount-start-date;discount-end-date;discount-ranges;leadtime-to-ship;update-delete"

  val offersApi = "/api/offers/imports"

  val importProductsApi = "/api/products/imports"

  val identifier = "mogobiz-identifier"
  val title = "mogobiz-title"
  val category = "mogobiz-category"
  val description = "mogobiz-description"
  val variantIdentifier = "mogobiz-variant-identifier"
  val productReferences = "mogobiz-product-references"
  val media = "mogobiz-media"
  val brand = "mogobiz-brand"
}
