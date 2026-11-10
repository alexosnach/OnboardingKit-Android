package appteam.onboardingkit.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressBarLayoutTest {

    @Test
    fun spacingIsThirtyTwoDp() {
        assertEquals(32.dp, ProgressBarLayout.itemSpacing)
    }

    /** Next to a button the progress bar keeps the spacing. */
    @Test
    fun paddingNextToAButtonIsTheSpacing() {
        assertEquals(ProgressBarLayout.itemSpacing, ProgressBarLayout.padding(hasNeighbor = true))
    }

    /** No button on a side: the progress bar keeps to the bar's margin. */
    @Test
    fun paddingWithoutAButtonIsTheEdgeMargin() {
        assertEquals(ProgressBarLayout.edgeMargin, ProgressBarLayout.padding(hasNeighbor = false))
    }

    /** The progress bar gets wider where a button is hidden, never narrower. */
    @Test
    fun edgeMarginIsSmallerThanTheSpacing() {
        assertTrue(ProgressBarLayout.edgeMargin < ProgressBarLayout.itemSpacing)
    }
}
