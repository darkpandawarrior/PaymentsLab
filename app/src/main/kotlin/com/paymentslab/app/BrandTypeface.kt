package com.paymentslab.app

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * Space Grotesk — PaymentsLab's display typeface (OFL-licensed, bundled), as a plain Android font
 * resource rather than a Compose Multiplatform resource.
 *
 * It lives in the Android app rather than in `core:designsystem` for two reasons, and the first is
 * the one that matters: a design system that ships a brand typeface is not brand-neutral, and this
 * module's own documentation says it carries no palette, no logo and no app typography. The font was
 * always in the wrong place.
 *
 * The second is practical. Held inside a KMP library targeting android + wasmJs, the generated
 * compose-resources `Res` class stops landing on the androidMain compilation source path from
 * Compose Multiplatform beta02 onwards — confirmed still broken in beta03. Keeping it there meant
 * pinning the entire six-repo family to beta01 to protect one font. As an Android resource it needs
 * no compose-resources machinery at all, so that constraint disappears.
 *
 * iOS and web currently pass null and render stock Material typography. That is a visible brand gap
 * on those two shells, and a deliberate one — see the PR discussion.
 */
val SpaceGrotesk: FontFamily =
    FontFamily(
        Font(R.font.space_grotesk_regular, FontWeight.Normal),
        Font(R.font.space_grotesk_medium, FontWeight.Medium),
        Font(R.font.space_grotesk_semibold, FontWeight.SemiBold),
        Font(R.font.space_grotesk_bold, FontWeight.Bold),
    )
