package org.mauritania.main4ino

import java.nio.file.Files
import java.nio.file.Path

import scala.reflect.io.Directory

trait TmpDirCtx {
  def withTmpDir[T](test: Path => T): T = {

    val tmp = Files.createTempDirectory("scala-tmp-dir")

    val result = try {
      test(tmp)
    } finally {
      val directory = new Directory(tmp.toFile)
      val success = directory.deleteRecursively()
      if (!success) {
        throw new IllegalStateException("Could not remove test directory: " + tmp)
      }
    }
    result
  }

}
