package appteam.onboardingkit.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The progress bar's spacing in the onboarding bar.
 *
 * The progress bar fills all the room between the back button and Skip, or
 * reaches the bar's margins where they are hidden.
 *
 * Kept separate from the bar's layout so it can be unit-tested without UI.
 */
internal object ProgressBarLayout {
    /** The gap between the progress bar and a side button. */
    val itemSpacing: Dp = 32.dp

    /** The gap between the progress bar and the bar's edge, on a side with no button. */
    val edgeMargin: Dp = 16.dp

    /**
     * The progress bar's padding on one side.
     *
     * @param hasNeighbor Whether a button (back or Skip) is shown on that side.
     */
    fun padding(hasNeighbor: Boolean): Dp = if (hasNeighbor) itemSpacing else edgeMargin
}
