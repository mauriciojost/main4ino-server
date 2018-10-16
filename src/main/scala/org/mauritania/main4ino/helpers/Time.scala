package org.mauritania.main4ino.helpers

import java.util.Date

import org.mauritania.main4ino.models.Timestamp

object Time {

	def nowTimestamp: Timestamp = System.currentTimeMillis()

	def toString(t: Timestamp): String = new Date(t).toString

}
