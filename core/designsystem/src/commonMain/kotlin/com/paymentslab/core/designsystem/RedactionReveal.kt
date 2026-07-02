package com.paymentslab.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val SCRAMBLE_CHARS = "#@\$%&*01234567890abcdefABCDEF"
private const val SCRAMBLE_STEPS = 6
private const val SCRAMBLE_STEP_MS = 45L

/**
 * Animates onto an already-[Redactor]-masked value (e.g. `"9f••••3a"`) by scrambling the visible
 * characters through a few random glyphs before settling — makes the redaction happen visibly
 * instead of the masked string just appearing, which is the whole point of showing it at all.
 */
@Composable
fun RedactionReveal(
    value: String,
    modifier: Modifier = Modifier,
) {
    var display by remember(value) { mutableStateOf(value) }
    val reducedMotion = LocalReducedMotion.current

    LaunchedEffect(value) {
        if (reducedMotion || value.isEmpty()) {
            display = value
            return@LaunchedEffect
        }
        repeat(SCRAMBLE_STEPS) { step ->
            display =
                value
                    .mapIndexed { index, char ->
                        val shouldScramble = char == '•' || index % 2 == step % 2
                        if (shouldScramble) SCRAMBLE_CHARS[Random.nextInt(SCRAMBLE_CHARS.length)] else char
                    }.joinToString("")
            delay(SCRAMBLE_STEP_MS)
        }
        display = value
    }

    Text(
        text = display,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        modifier = modifier,
    )
}
