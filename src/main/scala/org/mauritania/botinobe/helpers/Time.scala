package org.mauritania.botinobe.helpers

import org.h2.util.DateTimeUtils

object Time {

	type Timestamp = Long

	def now: Timestamp = DateTimeUtils.getCalendar.getTimeInMillis


}
