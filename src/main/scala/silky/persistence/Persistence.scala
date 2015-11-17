package silky.persistence

import scala.concurrent.Future

trait Persistence {
  def lastRefAcross(prefix: Char, contexts: String*): Future[String]
  def save(entry: Entry): Future[Entry]
  def find(context: String, ref: String): Future[Option[Entry]]
  def load(context: String, predicate: String ⇒ Boolean): Future[Seq[Entry]]
  def move(ref: String, source: String, target: String): Unit
}

case class Entry(context: String, ref: String, contents: String)
