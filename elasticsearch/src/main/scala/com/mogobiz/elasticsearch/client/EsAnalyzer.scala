/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */
package com.mogobiz.elasticsearch.client

import com.mogobiz.elasticsearch._
import com.mogobiz.elasticsearch.client.EsFilter._

/**
 *
 */
case class EsAnalyzer(override val id:String, tokenizer:String, filter:Seq[String], charFilter:Seq[String], index:Boolean, lang:String = "*") extends EsProperty{
  override lazy val build = Map("type" -> "custom", "tokenizer" -> tokenizer, "filter" -> filter, "char_filter" -> charFilter)
}

object EsAnalyzer{
  /**
   * analyzers
   */
  lazy val analyzers: Set[String] => Map[String, Seq[EsAnalyzer]] = languages => {
    filters(languages).foldLeft[Seq[(String, Seq[EsAnalyzer])]](Seq.empty)((a, b) => a :+ (b._1, EsAnalyzer.apply(b._1, b._2))).toMap
  }

  type AnalyzerFilter = EsAnalyzer => Boolean

  val language_analyzer : String => AnalyzerFilter => Option[EsAnalyzer] = language => p => {
    analyzers(Set(language)).get(language) match {
      case Some(xs) => Some(xs.filter(p).head)
      case _ => None
    }
  }

  val index_analyzer : String => Option[EsAnalyzer] = language => language_analyzer(language)(_.index)

  val search_analyzer : String => Option[EsAnalyzer] = language => language_analyzer(language)(!_.index)

  lazy val default_index_analyzer = apply("*", Seq.empty, index=true)

  lazy val default_search_analyzer = apply("*", Seq.empty, index=false)

  def apply(language:String, filters:Seq[EsFilter]):Seq[EsAnalyzer] = {
    Seq(apply(language, filters, index=true), apply(language, filters, index=false))
  }

  def apply(language:String, filters:Seq[EsFilter], index:Boolean):EsAnalyzer = {
    var filter = Seq[String]("icu_folding", "icu_normalizer") ++: filters.map(_.id)
    var id = language match {
      case "*" => "default"
      case _ => language
    }
    if(index){
      id += "_index_analyzer"
      filter = filter ++: Seq(nGram_filter.id)
    }
    else{
      id += "_search_analyzer"
    }
    EsAnalyzer(
      id,
      "icu_tokenizer",
      filter,
      Seq("html_strip"),
      index,
      language)
  }
}

