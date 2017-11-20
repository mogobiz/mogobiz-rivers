package com.mogobiz.common.rivers

import java.util
import java.util.ServiceLoader

import scala.collection.JavaConversions._

import com.mogobiz.common.client.{BulkResponse, BulkItem}
import com.mogobiz.common.rivers.spi.{GenericRiver, River}

/**
  * Created by smanciot on 15/11/2017.
  */
trait Rivers[T <: River] extends GenericRivers[BulkItem, BulkResponse]{
  def river(): Class[T]
}

abstract class AbstractRivers[T <: River] extends AbstractGenericRivers[BulkItem, BulkResponse] with Rivers[T] {
  private val rivers = new util.HashMap[String, T]
  def loadRivers: util.List[GenericRiver[BulkItem, BulkResponse]] = {
    val riverLoader = ServiceLoader.load(river())
    rivers.clear()
    riverLoader.reload()
    for(river <- riverLoader.iterator()){
      rivers.put(river.getType, river)
    }
    val ret: List[GenericRiver[BulkItem, BulkResponse]] = rivers.values().toList
    ret
  }
  def loadRiver(`type`: String): Option[T] = {
    if (rivers.containsKey(`type`)) Some(rivers.get(`type`)) else None
  }

}