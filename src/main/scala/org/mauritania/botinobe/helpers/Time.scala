package org.mauritania.botinobe.helpers

object Time {

	type Timestamp = Long

	def now: Timestamp = System.currentTimeMillis()

}
