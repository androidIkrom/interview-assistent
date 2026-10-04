package uz.devsuhbat.engine

/** The result screen's headline, by the share of questions answered right on the first try. */
enum class ResultHeadline {
    GREAT, GOOD, KEEP_GOING;

    companion object {
        fun of(firstTryCorrect: Int, total: Int): ResultHeadline = when {
            total <= 0 -> KEEP_GOING
            firstTryCorrect * 100 >= 80 * total -> GREAT
            firstTryCorrect * 100 >= 50 * total -> GOOD
            else -> KEEP_GOING
        }
    }
}

/** Only a strong result gets confetti, so a weak one is not cheered. */
val ResultHeadline.celebrates: Boolean get() = this == ResultHeadline.GREAT
