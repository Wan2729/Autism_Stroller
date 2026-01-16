package com.example.autismstroller.reusables

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.autismstroller.utilities.AppColors
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun LightingSettingDialog(
    initialColorString: String,
    onColorSelected: (String) -> Unit,
    onAnimationSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    // Use HSV (Hue, Saturation, Value) for easier manipulation
    val initialColor = Color(
      android.graphics.Color.HSVToColor(parseHsvString(initialColorString))
    )
    val initialHsv = FloatArray(3).apply {
        android.graphics.Color.colorToHSV(initialColor.toArgb(), this)
    }

    var hue by remember { mutableStateOf(initialHsv[0]) }
    var saturation by remember { mutableStateOf(initialHsv[1]) }
    var value by remember { mutableStateOf(initialHsv[2]) }

    val currentColor = remember {
        derivedStateOf {
            // Recompute color whenever Hue, Saturation, or Value changes
            Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))
        }
    }

    var selectedOption by remember { mutableStateOf(1) }
    var expanded by remember { mutableStateOf(false) }
    val options = mapOf<Int, String>(
        1 to "Color",
        2 to "Fade",
        3 to "Travel",
        4 to "Travel no Fill"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large,
            color = AppColors.White
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Customize Light Color",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // -- Dropdown Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Select Light: $selectedOption")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        options.forEach { (animationIndex, animationName) ->
                            DropdownMenuItem(
                                text = { Text(animationName) },
                                onClick = {
                                    selectedOption = animationIndex
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                // -- HUE SLIDER
                SliderControl(
                    label = "Hue (Angle: ${hue.toInt()}°)",
                    modifier = Modifier.fillMaxWidth(),
                    color = currentColor.value,
                    value = hue,
                    onValueChange = { hue = it },
                    // Set range to 0 to 360 degrees for precise control
                    valueRange = 0f..360f,
                    // Gradient shows the full spectrum at max S and V
                    gradient = Brush.horizontalGradient(
                        (0..360).map { i ->
                            Color(android.graphics.Color.HSVToColor(floatArrayOf(i.toFloat(), 1f, 1f)))
                        }
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // -- BRIGHTNESS SLIDER (Value/Lightness)
                SliderControl(
                    label = "Brightness (Value: ${"%.0f".format(value * 100)}%)",
                    modifier = Modifier.fillMaxWidth(),
                    color = currentColor.value,
                    value = value,
                    onValueChange = { value = it },
                    valueRange = 0f..1f,
                    // Gradient shows the range from black (V=0) to full brightness (V=1)
                    gradient = Brush.horizontalGradient(
                        listOf(Color.Black, Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, 1f))))
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Spacer(modifier = Modifier.height(24.dp))

                // Action Button (Apply)
                Button(
                    onClick = {
                        // 1. Take the raw slider values directly
                        // hue is 0.0 - 360.0
                        // saturation is 0.0 - 1.0
                        // value is 0.0 - 1.0

                        // 2. Format them to "360_100_100" string
                        val rawColorString = formatHsv(hue, saturation, value)

                        // 3. Send to BLE Handler
                        onColorSelected(rawColorString)
                        onAnimationSelected(selectedOption)
                        onDismiss()

                        Log.d("On Color Selected", "Raw HSV Sent: $rawColorString")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply Color", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

// -- COLOR WHEEL CANVAS COMPONENT (CORRECTED)
@Composable
fun ColorWheel(
    modifier: Modifier,
    hue: Float,
    saturation: Float,
    onHueChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit
) {
    // We use LocalDensity to correctly translate DP to Pixels inside the Box scope
    val density = LocalDensity.current
    val size: Dp = 250.dp

    val sweepGradient = remember {
        Brush.sweepGradient(
            (0..360).map { i ->
                Color(android.graphics.Color.HSVToColor(floatArrayOf(i.toFloat(), 1f, 1f)))
            }
        )
    }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier
            .fillMaxSize()
            .pointerInput(density) { // Use density as a key for correct context
                val sizePx = with(density) { size.toPx() }

                detectDragGestures { change, _ ->
                    val x = change.position.x - sizePx / 2
                    val y = change.position.y - sizePx / 2
                    val radius = sqrt(x * x + y * y)
                    val angle = atan2(y, x).toDegrees()

                    var newHue = (angle + 360) % 360
                    if (newHue < 0) newHue += 360
                    onHueChange(newHue)

                    val maxRadius = sizePx / 2
                    val newSaturation = (radius / maxRadius).coerceIn(0f, 1f)
                    onSaturationChange(newSaturation)
                }
            }
        ) {
            val sizePx = size.toPx() // Use toPx() inside the Canvas draw scope
            val center = Offset(sizePx / 2, sizePx / 2)
            val outerRadius = sizePx / 2

            // Draw the main color wheel (Hue)
            drawCircle(
                brush = sweepGradient,
                radius = outerRadius,
                center = center
            )

            // Draw saturation gradient (White center overlay - S=0 to S=1)
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to Color.White.copy(alpha = 1f), // white center
                    1.0f to Color.White.copy(alpha = 0f)  // transparent edge
                ),
                radius = outerRadius,
                center = center
            )

            // --- Selection Indicator Drawing ---
            val indicatorRadius = outerRadius * saturation
            val angleInRadians = Math.toRadians(hue.toDouble())
            val indicatorX = center.x + indicatorRadius * cos(angleInRadians).toFloat()
            val indicatorY = center.y + indicatorRadius * sin(angleInRadians).toFloat()

            // Draw the indicator circle
            drawCircle(
                color = Color.Black,
                radius = 12.dp.toPx(),
                center = Offset(indicatorX, indicatorY),
                style = Stroke(width = 3.dp.toPx()) // Black outline
            )
            drawCircle(
                color = Color.White,
                radius = 9.dp.toPx(),
                center = Offset(indicatorX, indicatorY)
            )
        }
    }
}

// -- Slider Component
private fun Float.toDegrees() = this * 180 / Math.PI.toFloat()
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SliderControl(
    label: String,
    modifier: Modifier,
    color: Color,
    value: Float,
    onValueChange: (Float) -> Unit,
    gradient: Brush,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f
) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Color preview (small circle)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(color, shape = CircleShape)
            )

            Spacer(Modifier.width(8.dp))

            // Slider
            Slider(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                valueRange = valueRange,
                colors = SliderDefaults.colors(
                    inactiveTrackColor = Color.Transparent,
                    activeTrackColor = Color.Transparent
                ),
                track = {
                    // Custom track to display the gradient
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .fillMaxWidth()
                            .background(gradient, shape = CircleShape)
                    )
                }
            )
        }
    }
}

//////////////////////
// FUNCTIONALITIES //
/////////////////////
private fun formatHsv(h: Float, s: Float, v: Float): String {
    val hueInt = h.toInt()
    val satInt = (s * 100).toInt()
    val valInt = (v * 100).toInt()
    return "${hueInt}_${satInt}_${valInt}"
}

private fun parseHsvString(hsvString: String): FloatArray {
    val (h, s, v) = hsvString.split("_").map { it.toInt() }

    return floatArrayOf(
        h.toFloat(),        // hue stays 0–360 float
        s / 100f,           // sat: 0–100 → 0f..1f float
        v / 100f            // val: 0–100 → 0f..1f float
    )
}