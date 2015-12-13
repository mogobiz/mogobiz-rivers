/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */
package com.mogobiz

import scala.collection.JavaConversions._

/**
  *
  */
package object elasticsearch {

  trait EsBuilder{
    def build:Map[String, Any]
    def apply():Map[String, Any] = build
    def toJavaMap: java.util.Map[String, Object] = {
      val ret = new java.util.HashMap[String, Object]()
      build.foreach(t=>ret.put(t._1, toJava(t._2)))
      ret
    }
    def toJava(a: Any) : Object = a match {
      case s: String => s
      case i: Integer => i
      case l: Seq[Any] => asJavaCollection(l)
      case m: Map[String, Any] =>
        val ret = new java.util.HashMap[String, Object]()
        m.foreach(t=>ret.put(t._1, toJava(t._2)))
        ret
      case _ => ???
    }
  }

  trait EsProperty extends EsBuilder{
    def id:String
    override def apply():Map[String, Any] = Map(id -> build)
  }

}
