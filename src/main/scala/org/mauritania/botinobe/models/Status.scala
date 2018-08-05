package org.mauritania.botinobe.models

object Status {

  // TODO improve this mess
  // - possible statuses mixed with virtual statuses (merged)
  // - cannot filter "any"
  val Created: Status = "C"
  val Consumed: Status = "X"
  val Merged: Status = "M"

}
