package silky.persistence.file

import java.io.FileNotFoundException
import java.nio.file.Paths

import silky.persistence.{Entry, Persistence}

import scala.reflect.io.Directory

class FilePersistence(baseDir: String, fileExtension: String = "json") extends Persistence {

  def initialise(contexts: String*): Unit =
    contexts map { context ⇒ Directory(s"$baseDir/$context") } foreach { dir ⇒ if (!dir.exists) dir.createDirectory() }

  def lastRefAcross(prefix: Char, contexts: String*): String = {
    val files = contexts flatMap filesIn filter (f ⇒ f.name.head == prefix && f.extension == fileExtension) sortBy (_.name)
    if (files.isEmpty) "00000000" else files.last.stripExtension.tail
  }

  def save(entry: Entry) = { Filepath.save(entry.contents, pathFor(entry.context, entry.ref)); entry }

  def find(context: String, ref: String) = filesIn(context)
    .find(_.name == s"$ref.$fileExtension")
    .map(f ⇒ Entry(context, ref, f.slurp()))

  def load(context: String, predicate: String ⇒ Boolean) = filesIn(context)
    .filter(f ⇒ predicate(f.name.replace(s".$fileExtension", "")))
    .map(f ⇒ Entry(context, f.name.replace(s".$fileExtension", ""), f.slurp()))
    .toSeq

  def move(ref: String, source: String, target: String) = {
    val sourcePath = pathFor(source, ref)
    if (!sourcePath.toFile.exists()) throw new FileNotFoundException(s"$sourcePath does not exist")
    Filepath.move(sourcePath, pathFor(target, ref))
  }

  private def filesIn(context: String) = Directory(s"$baseDir/$context").files
  private def pathFor(context: String, ref: String) = Paths.get(s"$baseDir/$context/$ref.$fileExtension")
}
