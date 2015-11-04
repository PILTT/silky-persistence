package silky.persistence.elasticsearch

import com.sksamuel.elastic4s.{KeywordAnalyzer, ElasticClient}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings.FieldType.{NestedType, StringType}
import org.elasticsearch.search.sort.SortOrder.DESC
import silky.persistence.{Entry, Persistence}

class ElasticsearchPersistence(_index: String, client: ElasticClient) extends Persistence {

  def initialise(contexts: String*) = {
/*
    // TODO: work out how to make the DSL produce the below raw mappings
    client.execute { create index _index mappings {
      "_default_" as (
                id typed StringType index NotAnalyzed store true,
        "metadata" typed NestedType
        )
    } }.await
*/
    val raw = """{ "mappings": { "_default_": { "_id": { "index": "not_analyzed" }, "properties": { "metadata": { "type": "nested" } } } } }"""
    client.execute { create index _index source raw }.await
  }

  def lastRefAcross(prefix: Char, contexts: String*) = {
    val query = search in _index types (contexts: _*) fetchSource false sort (field sort "_id" order DESC) postFilter prefixFilter("_id", prefix) limit 1
    val refs  = client.execute { query }.await.getHits.hits().map(_.id())
    if (refs.isEmpty) "00000000" else refs.head.replace(String.valueOf(prefix), "")
  }

  def save(entry: Entry) = {
    val response = client.execute { index into s"${_index}/${entry.context}" id entry.ref source entry.contents}.await
    if (response.isCreated) entry else entry  // TODO: perhaps return Either[String, Entry] instead
  }

  def find(context: String, ref: String) = {
    val response = client.execute { get id ref from s"${_index}/$context" }.await
    if (response.isExists) Some(Entry(context, ref, response.getSourceAsString)) else None
  }

  def load(context: String, predicate: String ⇒ Boolean) =
    client.execute {
      search in s"${_index}/$context" query {
        nestedQuery("metadata") query bool { not(matchQuery("metadata.status", "Deleted")) }
      }
    }.await.getHits.hits()
      .filter(hit ⇒ predicate(hit.id()))
      .map(hit ⇒ Entry(context, hit.id(), hit.sourceAsString()))

  def move(ref: String, source: String, target: String) = {
    // retrieve from source
    // save to target
    // delete from source
  }
}
