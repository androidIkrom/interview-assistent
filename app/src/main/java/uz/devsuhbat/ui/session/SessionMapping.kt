package uz.devsuhbat.ui.session

import uz.devsuhbat.engine.Progress
import uz.devsuhbat.ui.design.OptionState

/**
 * How one option looks. An option eliminated by a wrong pick stays crossed out even after the question is solved;
 * once solved, the selected options are the correct set and the untouched rest is dimmed.
 */
fun optionState(optionId: String, selected: Set<String>, disabled: Set<String>, solved: Boolean): OptionState = when {
    optionId in disabled -> OptionState.WRONG
    solved && optionId in selected -> OptionState.CORRECT
    solved -> OptionState.DIMMED
    optionId in selected -> OptionState.SELECTED
    else -> OptionState.IDLE
}

/** What the result screen says about readiness. */
sealed interface ReadinessLine {
    /** The session is still being saved; [percent] is the value before it, if known. */
    data class Pending(val percent: Int?) : ReadinessLine

    data class Grew(val from: Int, val to: Int) : ReadinessLine

    /** No growth: the plain value, so a drop never shows as a misleading arrow. */
    data class Current(val percent: Int) : ReadinessLine
}

fun readinessLine(before: Progress?, after: Progress?): ReadinessLine? = when {
    before == null && after == null -> null
    after == null -> ReadinessLine.Pending(before?.percent)
    before != null && after.percent > before.percent -> ReadinessLine.Grew(before.percent, after.percent)
    else -> ReadinessLine.Current(after.percent)
}
