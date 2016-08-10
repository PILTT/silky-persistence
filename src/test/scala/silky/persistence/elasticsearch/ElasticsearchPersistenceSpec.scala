package silky.persistence.elasticsearch

import com.sksamuel.elastic4s.testkit.ElasticSugar
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.{BeforeAndAfterAll, WordSpec}
import silky.persistence.Entry
import silky.persistence.FutureMatchers._

class ElasticsearchPersistenceSpec extends WordSpec with BeforeAndAfterAll with ElasticSugar {
  private val indexName = "silky"

  import java.nio.file.Paths, java.util.UUID.randomUUID
  override lazy val testNodeHomePath = Paths.get(s"target/tests/${getClass.getPackage.getName}.$suiteName/$randomUUID")

  private lazy val persistence = new ElasticsearchPersistence(indexName, createLocalClient)(concurrent.ExecutionContext.global)

  private val message1 :: ticket1 :: ticket2 :: ticket3 :: ticket4 :: xs = Seq(
    Entry("messages", "M00000123", """{ "metadata": { "status": "Pending" }, "message": "Hello World!" }"""),
    Entry("tickets",  "T00000001", """{ "metadata": { "status": "Deleted" }, "quantity":  100 }"""),
    Entry("tickets",  "T00000002", """{ "metadata": { "status": "Active"  }, "quantity":  200 }"""),
    Entry("tickets",  "T00000003", """{ "metadata": { "status": "Active"  }, "quantity":  300 }"""),
    Entry("incoming", "T00000004", """{ "metadata": { "status": "Pending" }, "quantity":  400 }"""),
    Entry("tickets",  "T00000005", """{ "metadata": { "status": "Deleted" }, "quantity":  500 }"""),
    Entry("tickets",  "T00000006", """{ "metadata": { "status": "Active"  }, "quantity":  600 }"""),
    Entry("incoming", "T00000007", """{ "metadata": { "status": "Pending" }, "quantity":  700 }"""),
    Entry("tickets",  "T00000008", """{ "metadata": { "status": "Active"  }, "quantity":  800 }"""),
    Entry("tickets",  "T00000009", """{ "metadata": { "status": "Deleted" }, "quantity":  900 }"""),
    Entry("tickets",  "T00000010", """{ "metadata": { "status": "Active"  }, "quantity": 1000 }"""),
    Entry("tickets",  "T00000011", """{ "metadata": { "status": "Active"  }, "quantity": 1100 }"""),
    Entry("tickets",  "T00000012", """{ "metadata": { "status": "Active"  }, "quantity": 1200 }"""),
    Entry("incoming", "T00000013", """{ "metadata": { "status": "Pending" }, "quantity": 1300 }"""),
    Entry("incoming", "T00000014", """{ "metadata": { "status": "Pending" }, "quantity": 1400 }"""),
    Entry("incoming", "T00000015", """{ "metadata": { "status": "Pending" }, "quantity": 1500 }""")
  )

  override protected def beforeAll() = {
    System.setSecurityManager(null)
    persistence.createDefaultMappings()

    val entries = message1 :: ticket1 :: ticket2 :: ticket3 :: ticket4 :: xs
    entries foreach persistence.save

    refresh(indexName)
    blockUntilCount(entries.size, indexName)
  }

  "lastRefAcross returns the last reference across multiple contexts" in {
    persistence.lastRefAcross(prefix = 'T', "incoming", "tickets") willReturn "00000015"
  }

  "lastRefAcross returns '00000000' when no matching entries are found in a given set of contexts" in {
    persistence.lastRefAcross(prefix = 'M', "incoming", "tickets") willReturn "00000000"
  }

  "find returns an entry existing in a given context" in {
    persistence.find("messages", "M00000123") willReturn message1
    persistence.find("tickets",  "T00000001") willReturn ticket1
    persistence.find("incoming", "T00000004") willReturn ticket4
  }

  "find returns none for a non-existent entry in a given context" in {
    persistence.find("tickets", "T00000123") willReturn None
  }

  "load returns entries whose reference matches a given predicate in a given context" in {
    persistence.load("tickets", predicate = _.matches("T0000000[1-4]")) will contain only (ticket2, ticket3)
  }

  "move copies the given entry from one context to another then deletes the original entry" in {
    val target = "tickets"
    persistence.move(ticket4.ref, ticket4.context, target) willReturn ticket4.copy(context = target)
    persistence.find(ticket4.context, ticket4.ref) willReturn None
  }

  "move fails for a non-existent entry" in {
    import org.scalatest.MustMatchers._
    import org.scalatest.exceptions.TestFailedException

    val exception = the [TestFailedException] thrownBy persistence.move("foo", "thin-air", "tickets").futureValue
    exception.getCause must have message s"requirement failed: Entry 'foo' not found in 'thin-air'"
  }
}
