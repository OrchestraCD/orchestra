package io.chumps.orchestra

import java.io.PrintStream
import java.nio.file.{Files, Paths}
import java.time.{LocalDateTime, ZoneOffset}

object Utils {

  /** Sets the standard out and err across all thread.
    * This is not Thread safe!
    */
  def withOutErr[T](stream: PrintStream)(func: => T): T = {
    val stdOut = System.out
    val stdErr = System.err
    try {
      System.setOut(stream)
      System.setErr(stream)
      Console.withOut(stream)(Console.withErr(stream)(func))
    } finally {
      stream.flush()
      stream.close()
      System.setOut(stdOut)
      System.setErr(stdErr)
    }
  }

  def runInit(runInfo: RunInfo, tags: Seq[String]) = {
    val runDir = OrchestraConfig.jobRunDir(runInfo)
    val firstTimeInit = runDir.toFile.mkdirs()

    if (firstTimeInit) {
      OrchestraConfig.runDir(runInfo.runId).toFile.mkdirs()

      tags.foreach { tag =>
        val tagDir = OrchestraConfig.tagDir(runInfo.job.id, tag)
        tagDir.toFile.mkdirs()
        Files.createSymbolicLink(Paths.get(tagDir.toString, runInfo.runId.toString), runDir)
      }

      val now = LocalDateTime.now()
      val dateDir = Paths
        .get(OrchestraConfig.runsByDateDir(runInfo.job.id).toString,
             now.getYear.toString,
             now.getDayOfYear.toString,
             now.toEpochSecond(ZoneOffset.UTC).toString)
      dateDir.toFile.mkdirs()
      Files.createSymbolicLink(Paths.get(dateDir.toString, runInfo.runId.toString), runDir)
    }
  }

  def generateColour(s: String): String = {
    def hex(shift: Int) =
      Integer.toHexString((s.hashCode >> shift) & 0x5) // 0x5 instead of 0xF to keep the colour dark
    "#" + hex(20) + hex(16) + hex(12) + hex(8) + hex(4) + hex(0)
  }
}
