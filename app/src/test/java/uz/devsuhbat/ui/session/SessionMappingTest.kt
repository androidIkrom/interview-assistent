package uz.devsuhbat.ui.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import uz.devsuhbat.engine.Progress
import uz.devsuhbat.ui.design.OptionState

class SessionMappingTest {
    @Test
    fun singleFlowStates() {
        assertEquals(OptionState.IDLE, optionState("a", emptySet(), emptySet(), solved = false))
        assertEquals(OptionState.SELECTED, optionState("a", setOf("a"), emptySet(), solved = false))
        assertEquals(OptionState.WRONG, optionState("b", emptySet(), setOf("b"), solved = false))
        assertEquals(OptionState.CORRECT, optionState("a", setOf("a"), setOf("b"), solved = true))
        assertEquals(OptionState.DIMMED, optionState("c", setOf("a"), setOf("b"), solved = true))
    }

    @Test
    fun multiSolvedStates() {
        val selected = setOf("a", "b")
        val disabled = setOf("c")
        assertEquals(OptionState.CORRECT, optionState("a", selected, disabled, solved = true))
        assertEquals(OptionState.CORRECT, optionState("b", selected, disabled, solved = true))
        assertEquals(OptionState.WRONG, optionState("c", selected, disabled, solved = true))
        assertEquals(OptionState.DIMMED, optionState("d", selected, disabled, solved = true))
    }

    @Test
    fun grownReadinessShowsBothValues() {
        assertEquals(ReadinessLine.Grew(50, 60), readinessLine(Progress(5, 10), Progress(6, 10)))
    }

    @Test
    fun droppedReadinessShowsOnlyTheCurrentPercent() {
        assertEquals(ReadinessLine.Current(50), readinessLine(Progress(6, 10), Progress(5, 10)))
        assertEquals(ReadinessLine.Current(60), readinessLine(Progress(6, 10), Progress(6, 10)))
    }

    @Test
    fun pendingWhileSaving() {
        assertEquals(ReadinessLine.Pending(50), readinessLine(Progress(5, 10), null))
        assertNull(readinessLine(null, null))
    }
}
