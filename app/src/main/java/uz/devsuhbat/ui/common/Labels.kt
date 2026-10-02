package uz.devsuhbat.ui.common

import androidx.annotation.StringRes
import uz.devsuhbat.R
import uz.devsuhbat.content.Level

@get:StringRes
val Level.titleRes: Int
    get() = when (this) {
        Level.JUNIOR -> R.string.level_junior
        Level.MIDDLE -> R.string.level_middle
        Level.STRONG_MIDDLE -> R.string.level_strong_middle
        Level.SENIOR -> R.string.level_senior
    }

@get:StringRes
val Level.descriptionRes: Int
    get() = when (this) {
        Level.JUNIOR -> R.string.level_junior_desc
        Level.MIDDLE -> R.string.level_middle_desc
        Level.STRONG_MIDDLE -> R.string.level_strong_middle_desc
        Level.SENIOR -> R.string.level_senior_desc
    }

/** Title of a field group id from the catalog; unknown groups fall under "Boshqa". */
@StringRes
fun groupTitleRes(group: String): Int = when (group) {
    "mobile" -> R.string.group_mobile
    "frontend" -> R.string.group_frontend
    "backend" -> R.string.group_backend
    else -> R.string.group_other
}
