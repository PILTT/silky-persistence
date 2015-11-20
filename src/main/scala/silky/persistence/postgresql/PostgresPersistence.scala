package silky.persistence.postgresql

import com.github.tminglei.slickpg.JsonString
import silky.persistence.postgresql.PostgresDriver.api._
import silky.persistence.{Entry, Persistence}
import slick.profile.SqlProfile.ColumnOption.NotNull

import scala.concurrent.ExecutionContext

class PostgresPersistence(db: Database)(implicit ctx: ExecutionContext) extends Persistence {
  private type RowType = (String, String, JsonString)

  private class Entries(tag: Tag) extends Table[RowType](tag, "entries") {
    def context  = column[String]("context", O.SqlType("VARCHAR(20)"), NotNull)
    def ref      = column[String]("ref",     O.SqlType("VARCHAR(20)"), NotNull)
    def contents = column[JsonString]("contents")

    def *  = (context, ref, contents)
    def pk = primaryKey("entries_pk", (context, ref))
    def uniqueRef = index("ref_idx", ref, unique = true)
  }

  private val entries = TableQuery[Entries]

  private[postgresql] def createSchema() = db.run(DBIO.seq(
    entries.schema.create,
    sqlu"CREATE INDEX entry_idx ON entries USING GIN (contents jsonb_path_ops)"
  ))

  private[postgresql] def optimise()   = db.run(sqlu"VACUUM ANALYZE entries")
  private[postgresql] def deleteAll()  = db.run(entries.delete)
  private[postgresql] def dropSchema() = db.run(entries.schema.drop)

  def lastRefAcross(prefix: Char, contexts: String*) =
    db.run(entries.filter(_.context inSetBind contexts).map(_.ref).max
      .fold("00000000".asColumnOf[String])(_.replace(String.valueOf(prefix), ""))
      .result)

  def save(entry: Entry) =
    db.run(entries += (entry.context, entry.ref, JsonString(entry.contents))).map(_ ⇒ entry)

  def find(context: String, ref: String) =
    db.run(findQuery(context, ref).result.headOption.map(option ⇒ option map toEntry))

  def load(context: String, predicate: String ⇒ Boolean) =
    db.run(loadQuery(context).result.map(rows ⇒ rows filter { r ⇒ predicate(r._2) } map toEntry))

  def move(ref: String, source: String, target: String) =
    db.run(selectContextQuery(ref, source).update(target)
      .andThen(findQuery(target, ref).result.head.map(toEntry)))

  private val findQuery = Compiled((context: Rep[String], ref: Rep[String]) ⇒ entries
    .filter(_.context === context)
    .filter(_.ref === ref))

  private val loadQuery = Compiled((context: Rep[String]) ⇒ entries
    .filter(_.context === context)
    .filterNot(_.contents @> JsonString("""{ "metadata": { "status": "Deleted" } }""")))

  private val selectContextQuery = Compiled((ref: Rep[String], source: Rep[String]) ⇒ entries
    .filter(_.context === source)
    .filter(_.ref === ref)
    .map(_.context))

  private def toEntry = (r: RowType) ⇒ Entry(r._1, r._2, r._3.value)
}
