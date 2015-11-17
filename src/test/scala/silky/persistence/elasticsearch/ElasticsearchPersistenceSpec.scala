package silky.persistence.elasticsearch

import com.sksamuel.elastic4s.testkit.ElasticSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpec}
import silky.elasticsearch.EmbeddedElasticsearch
import silky.persistence.Entry

class ElasticsearchPersistenceSpec extends WordSpec with MustMatchers with BeforeAndAfterAll with ElasticSugar with ScalaFutures {

  private val message1 :: ticket1 :: ticket2 :: ticket3 :: ticket4 :: xs = Seq(
    Entry("messages", "M00000123", "{ \"metadata\": { \"status\": \"Pending\" }, \"message\": \"Hello World!\" }"),
    Entry("tickets",  "T00000001", "{ \"metadata\": { \"status\": \"Deleted\" }, \"quantity\":  100 }"),
    Entry("tickets",  "T00000002", "{ \"metadata\": { \"status\": \"Active\"  }, \"quantity\":  200 }"),
    Entry("tickets",  "T00000003", "{ \"metadata\": { \"status\": \"Active\"  }, \"quantity\":  300 }"),
    Entry("incoming", "T00000004", "{ \"metadata\": { \"status\": \"Pending\" }, \"quantity\":  400 }"),
    Entry("tickets",  "T00000005", "{ \"metadata\": { \"status\": \"Deleted\" }, \"quantity\":  500 }"),
    Entry("tickets",  "T00000006", "{ \"metadata\": { \"status\": \"Active\"  }, \"quantity\":  600 }"),
    Entry("incoming", "T00000007", "{ \"metadata\": { \"status\": \"Pending\" }, \"quantity\":  700 }"),
    Entry("tickets",  "T00000008", "{ \"metadata\": { \"status\": \"Active\"  }, \"quantity\":  800 }"),
    Entry("tickets",  "T00000009", "{ \"metadata\": { \"status\": \"Deleted\" }, \"quantity\":  900 }"),
    Entry("tickets",  "T00000010", "{ \"metadata\": { \"status\": \"Active\"  }, \"quantity\": 1000 }"),
    Entry("tickets",  "T00000011", "{ \"metadata\": { \"status\": \"Active\"  }, \"quantity\": 1100 }"),
    Entry("tickets",  "T00000012", "{ \"metadata\": { \"status\": \"Active\"  }, \"quantity\": 1200 }"),
    Entry("incoming", "T00000013", "{ \"metadata\": { \"status\": \"Pending\" }, \"quantity\": 1300 }"),
    Entry("incoming", "T00000014", "{ \"metadata\": { \"status\": \"Pending\" }, \"quantity\": 1400 }"),
    Entry("incoming", "T00000015", "{ \"metadata\": { \"status\": \"Pending\" }, \"quantity\": 1500 }")
  )

  private val elasticsearch = new EmbeddedElasticsearch(
    dataDir  = s"target/tests/${getClass.getPackage.getName}.$suiteName/${java.util.UUID.randomUUID}",
    nodeName = "local_node",
    httpEnabled = true
  )

  private val indexName   = "silky"
  private val persistence = new ElasticsearchPersistence(indexName, elasticsearch.client)(concurrent.ExecutionContext.global)

  override def client = elasticsearch.client

  override protected def beforeAll() = {
    elasticsearch.start()
    persistence.createDefaultMappings()

    val entries = message1 :: ticket1 :: ticket2 :: ticket3 :: ticket4 :: xs
    entries foreach persistence.save

    refresh(indexName)
    blockUntilCount(entries.size, indexName)
  }

  override protected def afterAll()  = elasticsearch.stop()

  "lastRefAcross returns the last reference across multiple contexts" in {
    persistence.lastRefAcross(prefix = 'T', "incoming", "tickets").futureValue mustBe "00000015"
  }

  "lastRefAcross returns '00000000' when no matching entries are found in a given set of contexts" in {
    persistence.lastRefAcross(prefix = 'M', "incoming", "tickets").futureValue mustBe "00000000"
  }

  "find returns an entry existing in a given context" in {
    persistence.find("messages", "M00000123").futureValue mustBe Some(message1)
    persistence.find("tickets",  "T00000001").futureValue mustBe Some(ticket1)
    persistence.find("incoming", "T00000004").futureValue mustBe Some(ticket4)
  }

  "find returns none for a non-existent entry in a given context" in {
    persistence.find("tickets", "T00000123").futureValue mustBe None
  }

  "load returns entries whose reference matches a given predicate in a given context" in {
    persistence.load("tickets", predicate = _.matches("T0000000[1-4]")).futureValue must contain only (ticket2, ticket3)
  }
}
