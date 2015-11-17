package silky.persistence.elasticsearch

import com.sksamuel.elastic4s.{KeywordAnalyzer, ElasticClient}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings.FieldType.{NestedType, StringType}
import org.elasticsearch.search.sort.SortOrder.DESC
import silky.persistence.{Entry, Persistence}

import scala.concurrent.{ExecutionContext, Future}

class ElasticsearchPersistence(_index: String, client: ElasticClient)(implicit ctx: ExecutionContext) extends Persistence {

  private[elasticsearch] def createDefaultMappings(): Unit =
    client.execute {
      // TODO: work out how to make the DSL produce the below raw mappings
//      create index _index mappings {
//        "_default_" as (
//          id typed StringType index NotAnalyzed store true,
//          "metadata" typed NestedType
//          )
//      }
      create index _index source
        """{
          |  "mappings": {
          |    "_default_": {
          |      "_id": { "index": "not_analyzed" },
          |      "properties": { "metadata": { "type": "nested" } }
          |    }
          |  }
          |}""".stripMargin
    }.await

  def lastRefAcross(prefix: Char, contexts: String*) = client.execute {
      search in _index types (contexts: _*) fetchSource false sort (field sort "_id" order DESC) postFilter {
        prefixFilter("_id", prefix)
      } limit 1
    }.map { _.getHits.hits().headOption.fold("00000000")(_.id().replace(String.valueOf(prefix), "")) }

  def save(entry: Entry) = client
    .execute { index into s"${_index}/${entry.context}" id entry.ref source entry.contents }
    .map { response ⇒ if (response.isCreated) entry else entry }  // TODO: perhaps return Either[String, Entry] instead

  def find(context: String, ref: String) = client
    .execute { get id ref from s"${_index}/$context" }
    .map { response ⇒ if (response.isExists) Some(Entry(context, ref, response.getSourceAsString)) else None }

  def load(context: String, predicate: String ⇒ Boolean) = client
    .execute {
      search in s"${_index}/$context" query {
        nestedQuery("metadata") query bool { not(matchQuery("metadata.status", "Deleted")) }
      }
    }.map { response ⇒ response.getHits.hits()
      .filter(hit ⇒ predicate(hit.id()))
      .map(hit ⇒ Entry(context, hit.id(), hit.sourceAsString()))
    }

  def move(ref: String, source: String, target: String) = Future {
    // retrieve from source
    // save to target
    // delete from source
    Entry(target, ref, "")
  }
}
