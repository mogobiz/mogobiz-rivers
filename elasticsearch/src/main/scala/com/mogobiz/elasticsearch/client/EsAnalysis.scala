/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */
package com.mogobiz.elasticsearch.client

import com.mogobiz.elasticsearch._
import com.mogobiz.elasticsearch.client.EsAnalyzer._
import com.mogobiz.elasticsearch.client.EsFilter._

import scala.collection.JavaConversions

/**
 *
 * Created by smanciot on 10/08/14.
 */
case class EsAnalysis(filters:Seq[EsFilter], analyzers:Seq[EsAnalyzer], tokenizers:Seq[EsTokenizer] = Seq.empty) extends EsBuilder{
  override lazy val build = Map(
    "analysis" -> Map(
      "filter" -> filters.flatMap(_()).toMap,
      "analyzer" -> analyzers.flatMap(_()).toMap,
      "tokenizer" -> tokenizers.flatMap(_()).toMap
    )
  )
  lazy val index_analyzers:Seq[EsAnalyzer] = analyzers.filter(_.index)
  lazy val search_analyzers:Seq[EsAnalyzer] = analyzers.filterNot(_.index)
  def index_analyzer(lang: String) = index_analyzers.find(p => p.lang == lang).getOrElse(EsAnalyzer.default_index_analyzer)
  def search_analyzer(lang: String) = search_analyzers.find(p => p.lang == lang).getOrElse(EsAnalyzer.default_search_analyzer)
}

object EsAnalysis {
  def apply(languages:Set[String]):EsAnalysis = apply(languages, Seq.empty)
  def apply(languages:Set[String], tokenizers:Seq[EsTokenizer]):EsAnalysis = EsAnalysis(
    filters(languages).values.flatten.toSeq ++: Seq(nGram_filter),
    analyzers(languages).values.flatten.toSeq,
    tokenizers)
}
