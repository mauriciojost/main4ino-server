package org.mauritania.main4ino.devicelogs

import eu.timepit.refined.types.numeric.NonNegInt

class Partitioner(drops: NonNegInt) {
  import Partitioner._
  def partition(r: Record): Partition = r / Math.pow(10, drops.value).toInt
}

object Partitioner {
  type Record = Long
  type Partition = Long
}
