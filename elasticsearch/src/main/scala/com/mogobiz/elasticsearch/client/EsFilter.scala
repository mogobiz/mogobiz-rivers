/*
 * Copyright (C) 2015 Mogobiz SARL. All rights reserved.
 */
package com.mogobiz.elasticsearch.client

import com.mogobiz.elasticsearch._

/**
 *
 */
trait EsFilter extends EsProperty

case class StemFilter(override val id:String, name:String) extends EsFilter{
  override lazy val build = Map("type" -> "stemmer", "name" -> name)
}

case class StopFilter(override val id:String, stopWords:Seq[String]) extends EsFilter{
  override lazy val build = Map("type" -> "stop", "stopwords" -> stopWords)
}

case class NGramFilter(override val id:String, minGram:Int, maxGram:Int) extends EsFilter{
  override lazy val build = Map("type" -> "nGram", "min_gram"->minGram, "max_gram" -> maxGram)
}

object EsFilter {

  /**
   * filters
   * TODO add filters for la1,cy,ch,et,hr,ja,lv,pl,sk,sl,sr,zh
   */
  val nGram_filter = NGramFilter("nGram_filter", 2, 20)
  val ar_stop_filter = StopFilter("ar_stop_filter", Seq("_arabic_"))
  val bg_stop_filter = StopFilter("bg_stop_filter", Seq("_bulgarian_"))
  val ca_stop_filter = StopFilter("ca_stop_filter", Seq("_catalan_"))
  val cs_stop_filter = StopFilter("cs_stop_filter", Seq("_czech_"))
  val da_stop_filter = StopFilter("da_stop_filter", Seq("_danish_"))
  val de_stop_filter = StopFilter("de_stop_filter", Seq("_german_"))
  val de_stem_filter = StemFilter("de_stem_filter", "minimal_german")
  val el_stop_filter = StopFilter("el_stop_filter", Seq("_greek_"))
  val en_stop_filter = StopFilter("en_stop_filter", Seq("_english_"))
  val en_stem_filter = StemFilter("en_stem_filter", "minimal_english")
  val es_stop_filter = StopFilter("es_stop_filter", Seq("_spanish_"))
  val es_stem_filter = StemFilter("es_stem_filter", "light_spanish")
  val eu_stop_filter = StopFilter("eu_stop_filter", Seq("_basque_"))
  val fa_stop_filter = StopFilter("fa_stop_filter", Seq("_persian_"))
  val fi_stop_filter = StopFilter("fi_stop_filter", Seq("_finnish_"))
  val fi_stem_filter = StemFilter("fi_stem_filter", "light_finish")
  val fr_stop_filter = StopFilter("fr_stop_filter", Seq("_french_"))
  val fr_stem_filter = StemFilter("fr_stem_filter", "minimal_french")
  val hi_stop_filter = StopFilter("hi_stop_filter", Seq("_hindi_"))
  val hu_stop_filter = StopFilter("hu_stop_filter", Seq("_hungarian_"))
  val hu_stem_filter = StemFilter("hu_stem_filter", "light_hungarian")
  val hy_stop_filter = StopFilter("hy_stop_filter", Seq("_armenian_"))
  val id_stop_filter = StopFilter("id_stop_filter", Seq("_indonesian_"))
  val it_stop_filter = StopFilter("it_stop_filter", Seq("_italian_"))
  val it_stem_filter = StemFilter("it_stem_filter", "light_italian")
  val nl_stop_filter = StopFilter("nl_stop_filter", Seq("_dutch_"))
  val no_stop_filter = StopFilter("no_stop_filter", Seq("_norwegian_"))
  val pt_stop_filter = StopFilter("pt_stop_filter", Seq("_portuguese_"))
  val pt_stem_filter = StemFilter("pt_stem_filter", "minimal_portuguese")
  val ro_stop_filter = StopFilter("ro_stop_filter", Seq("_romanian_"))
  val ru_stop_filter = StopFilter("ru_stop_filter", Seq("_russian_"))
  val ru_stem_filter = StemFilter("ru_stem_filter", "light_russian")
  val sv_stop_filter = StopFilter("sv_stop_filter", Seq("_swedish_"))
  val sv_stem_filter = StemFilter("sv_stem_filter", "light_swedish")
  val tr_stop_filter = StopFilter("tr_stop_filter", Seq("_turkish_"))

  lazy val filters: Set[String] => Map[String, Seq[EsFilter]] = languages => {
    Map(
      "*"  -> Seq(),
      "ar" -> Seq(ar_stop_filter),
      "bg" -> Seq(bg_stop_filter),
      "ca" -> Seq(ca_stop_filter),
      "cs" -> Seq(cs_stop_filter),
      "da" -> Seq(da_stop_filter),
      "de" -> Seq(de_stop_filter, de_stem_filter),
      "el" -> Seq(el_stop_filter),
      "en" -> Seq(en_stop_filter, en_stem_filter),
      "es" -> Seq(es_stop_filter, es_stem_filter),
      "eu" -> Seq(eu_stop_filter),
      "fa" -> Seq(fa_stop_filter),
      "fi" -> Seq(fi_stop_filter, fi_stem_filter),
      "fr" -> Seq(fr_stop_filter, fr_stem_filter),
      "hi" -> Seq(hi_stop_filter),
      "hu" -> Seq(hu_stop_filter, hu_stem_filter),
      "hy" -> Seq(hy_stop_filter),
      "id" -> Seq(id_stop_filter),
      "it" -> Seq(it_stop_filter, it_stem_filter),
      "nl" -> Seq(nl_stop_filter),
      "no" -> Seq(no_stop_filter),
      "pt" -> Seq(pt_stop_filter, pt_stem_filter),
      "ro" -> Seq(ro_stop_filter),
      "ru" -> Seq(ru_stop_filter, ru_stem_filter),
      "sv" -> Seq(sv_stop_filter, sv_stem_filter),
      "tr" -> Seq(tr_stop_filter)
    ).filter((a:(String,Seq[EsFilter])) => ("*" == a._1) || languages.contains(a._1))
  }

}
