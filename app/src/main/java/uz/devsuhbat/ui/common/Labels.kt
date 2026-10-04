package uz.devsuhbat.ui.common

import androidx.annotation.StringRes
import uz.devsuhbat.R
import uz.devsuhbat.content.Level
import uz.devsuhbat.content.QuestionKind

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

@get:StringRes
val QuestionKind.titleRes: Int
    get() = when (this) {
        QuestionKind.CONCEPT -> R.string.kind_concept
        QuestionKind.TRUE_FALSE -> R.string.kind_true_false
        QuestionKind.CODE_OUTPUT -> R.string.kind_code_output
        QuestionKind.CODE_REVIEW -> R.string.kind_code_review
    }

/** Title of a field group id from the catalog; unknown groups fall under "Boshqa". */
@StringRes
fun groupTitleRes(group: String): Int = when (group) {
    "mobile" -> R.string.group_mobile
    "frontend" -> R.string.group_frontend
    "backend" -> R.string.group_backend
    else -> R.string.group_other
}
