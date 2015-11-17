package silky.persistence.file

import java.nio.file.Paths

import silky.persistence.{Entry, Persistence}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.io.File

class FilePersistence(baseDir: String, fileExtension: String = "json")(implicit ctx: ExecutionContext) extends Persistence {
  import reflect.io.Directory

  def lastRefAcross(prefix: Char, contexts: String*) = Future {
    val files = contexts flatMap filesIn filter (f ⇒ f.name.head == prefix && f.extension == fileExtension) sortBy (_.name)
    if (files.isEmpty) "00000000" else files.last.stripExtension.tail
  }

  def save(entry: Entry) = Future {
    createIfRequired(directoryFor(entry.context))
    Filepath.save(entry.contents, pathFor(entry.context, entry.ref))
    entry
  }

  def find(context: String, ref: String) = Future {
    filesIn(context)
      .find(_.name == s"$ref.$fileExtension")
      .map(f ⇒ Entry(context, ref, f.slurp()))
  }

  def load(context: String, predicate: String ⇒ Boolean) = Future {
    filesIn(context)
      .filter(f ⇒ predicate(f.name.replace(s".$fileExtension", "")))
      .map(f ⇒ Entry(context, f.name.replace(s".$fileExtension", ""), f.slurp()))
      .toSeq
  }

  def move(ref: String, source: String, target: String) = Future {
    val (sourcePath, targetPath) = (pathFor(source, ref), pathFor(target, ref))
    require(sourcePath.toFile.exists(), s"$sourcePath does not exist")
    createIfRequired(directoryFor(target))
    Filepath.move(sourcePath, targetPath)
    Entry(target, ref, new File(targetPath.toFile).slurp())
  }

  private def filesIn(context: String) = directoryFor(context).files
  private def directoryFor(context: String) = Directory(s"$baseDir/$context")
  private def createIfRequired(directory: Directory) = if (!directory.exists) directory.createDirectory()
  private def pathFor(context: String, ref: String) = Paths.get(s"$baseDir/$context/$ref.$fileExtension")
}
