package com.mogobiz.mirakl.client.domain

import com.mogobiz.common.client.{Item, BulkAction, BulkItem}

/**
  *
  * Created by smanciot on 13/04/16.
  */
trait MiraklItem extends BulkItem {
  val label: String
  def append(buffer: StringBuffer, separator: String): StringBuffer
}

case class MiraklItems[T <: MiraklItem](header: String, items: List[T]) {

  def getBytes(charset: String, separator: String = ";"): Array[Byte] = {
    def buffer = new StringBuffer(String.format(s"$header%n"))
    for(item <- items) {item.append(buffer, separator)}
    val str = buffer.toString
    str.getBytes(charset)
  }

}

abstract case class AbstractMiraklItem[T <: Item](
                                                         id: String,
                                                         label: String,
                                                         action: BulkAction,
                                                         parent: Option[T])
  extends MiraklItem{
  setId(id)
  setAction(action)
  setParent(parent.orNull)
}

class MiraklCategory(
                           override val id: String,
                           override val label: String,
                           override val action: BulkAction,
                           override val parent: Option[MiraklCategory],
                           logisticClass: String = "")
  extends AbstractMiraklItem[MiraklCategory](id, label, action, parent) {

  def this(id: String, label: String, parent: Option[MiraklCategory]){
    this(id, label, BulkAction.UPDATE, parent)
  }

  def this(id: String, label: String){
    this(id, label, None)
  }

  @Override
  def append(buffer: StringBuffer, separator: String = ";"): StringBuffer = {
    buffer.append(
      String.format(
        s"$id$separator$label$separator$logisticClass$separator${action.toString.toLowerCase()}$separator${parent.map(_.id).getOrElse("")}%n"
      )
    )
  }
}

class MiraklHierarchy(
                            override val id: String,
                            override val label: String,
                            override val action: BulkAction,
                            override val parent: Option[MiraklCategory])
  extends AbstractMiraklItem[MiraklCategory](id, label, action, parent) {

  def this(id: String, label: String, parent: Option[MiraklCategory]){
    this(id, label, BulkAction.UPDATE, parent)
  }

  def this(id: String, label: String){
    this(id, label, None)
  }

  @Override
  def append(buffer: StringBuffer, separator: String = ";"): StringBuffer = {
    buffer.append(
      String.format(
        s"$id$separator$label$separator${parent.map(_.id).getOrElse("")}$separator${action.toString.toLowerCase()}$separator%n"
      )
    )
  }
}

class MiraklValue(
                        override val id: String,
                        override val label: String,
                        override val action: BulkAction,
                        override val parent: Option[MiraklValue])
  extends AbstractMiraklItem[MiraklValue](id, label, action, parent) {

  def this(id: String, label: String, parent: Option[MiraklValue]){
    this(id, label, BulkAction.UPDATE, parent)
  }

  def this(id: String, label: String){
    this(id, label, None)
  }

  @Override
  def append(buffer: StringBuffer, separator: String = ";"): StringBuffer = {
    buffer.append(
      String.format(
        s"${parent.map(_.id).getOrElse("")}$separator${parent.map(_.label).getOrElse("")}$separator$id$separator$label$separator${getAction.toString.toLowerCase()}%n"
      )
    )
  }
}

