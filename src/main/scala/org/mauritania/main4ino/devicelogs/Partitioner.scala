package org.mauritania.main4ino.devicelogs

import eu.timepit.refined.types.numeric.PosInt

class Partitioner(bucketSize: PosInt) {
  import Partitioner._
  def partition(r: Record): Partition = r / bucketSize.value
}

object Partitioner {
  type Record = Long
  type Partition = Long
}
