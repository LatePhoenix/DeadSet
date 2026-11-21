package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.setlist.TransitionGraph
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Circular transition graph (step 3.6): songs around a circle, a curved chord for each connected
 * transition (thicker = more often). Tap a song to highlight its links; the list below the graph
 * names them, and tapping a row filters the show list to that transition.
 */
@Composable
fun TransitionGraphCard(
    graph: TransitionGraph,
    onFilterBySegue: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selected by rememberSaveable(graph.nodes) { mutableStateOf<String?>(null) }
    val textMeasurer = rememberTextMeasurer()
    val maxCount = graph.edges.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = StealieCardDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = modifier
            .fillMaxWidth()
            .testTag("transition_graph_card")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .semantics {
                        contentDescription = "Transition graph of ${graph.nodes.size} songs and " +
                            "${graph.edges.size} transitions. Tap a song to list its transitions."
                    }
                    .pointerInput(graph) {
                        detectTapGestures { tap ->
                            val positions = nodePositions(graph.nodes.size, size.width.toFloat(), size.height.toFloat())
                            val nearest = positions.withIndex().minByOrNull { (_, p) -> hypot(p.x - tap.x, p.y - tap.y) }
                            selected = nearest
                                ?.takeIf { (_, p) -> hypot(p.x - tap.x, p.y - tap.y) < 36.dp.toPx() }
                                ?.let { graph.nodes[it.index] }
                                ?.takeIf { it != selected }
                        }
                    }
            ) {
                val positions = nodePositions(graph.nodes.size, size.width, size.height)
                val index = graph.nodes.withIndex().associate { it.value to it.index }
                val center = Offset(size.width / 2, size.height / 2)

                for (edge in graph.edges) {
                    val from = positions[index.getValue(edge.from)]
                    val to = positions[index.getValue(edge.to)]
                    val active = selected == null || edge.from == selected || edge.to == selected
                    val path = Path().apply {
                        moveTo(from.x, from.y)
                        // Bend each chord toward the center so overlapping links stay readable.
                        val ctrl = Offset((from.x + to.x) / 2 * 0.4f + center.x * 0.6f, (from.y + to.y) / 2 * 0.4f + center.y * 0.6f)
                        quadraticTo(ctrl.x, ctrl.y, to.x, to.y)
                    }
                    val weight = edge.count.toFloat() / maxCount
                    drawPath(
                        path = path,
                        color = if (active) StealieCrimsonLight else BorderSubtle,
                        alpha = if (active) 0.35f + 0.6f * weight else 0.25f,
                        style = Stroke(width = (1.5f + 5f * weight).dp.toPx())
                    )
                }

                graph.nodes.forEachIndexed { i, name ->
                    val p = positions[i]
                    val isSel = name == selected
                    drawCircle(color = if (isSel) StealieGold else StealieCyan, radius = (if (isSel) 7 else 5).dp.toPx(), center = p)
                    val layout = textMeasurer.measure(
                        text = name,
                        style = TextStyle(
                            color = if (isSel) StealieGold else TextSecondaryDark,
                            fontSize = 9.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                        ),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        constraints = androidx.compose.ui.unit.Constraints(maxWidth = 92.dp.roundToPx())
                    )
                    // Labels sit outside the circle: beside side nodes, above/below top and bottom nodes.
                    val nearVerticalAxis = kotlin.math.abs(p.x - center.x) < 24.dp.toPx()
                    val topLeft = if (nearVerticalAxis) {
                        val dy = if (p.y < center.y) -layout.size.height - 8.dp.toPx() else 8.dp.toPx()
                        Offset(p.x - layout.size.width / 2, p.y + dy)
                    } else {
                        val dx = if (p.x >= center.x) 8.dp.toPx() else -layout.size.width - 8.dp.toPx()
                        Offset(p.x + dx, p.y - layout.size.height / 2)
                    }
                    drawText(layout, topLeft = topLeft)
                }
            }

            val sel = selected
            if (sel == null) {
                Text(
                    text = "Tap a song to list its transitions.",
                    color = TextTertiaryDark,
                    fontSize = 12.sp
                )
            } else {
                Text(text = sel, color = StealieGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                for (edge in graph.edgesOf(sel)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onFilterBySegue(edge.label) }
                            .padding(vertical = 6.dp)
                    ) {
                        Text(text = edge.label, color = TextPrimaryDark, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Text(text = "${edge.count}x", color = StealieGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/** Evenly spaced points on a circle inset enough to leave room for labels. */
private fun nodePositions(n: Int, width: Float, height: Float): List<Offset> {
    if (n == 0) return emptyList()
    val radius = minOf(width, height) / 2 * 0.58f
    val cx = width / 2
    val cy = height / 2
    return List(n) { i ->
        val angle = -Math.PI / 2 + 2 * Math.PI * i / n
        Offset(cx + radius * cos(angle).toFloat(), cy + radius * sin(angle).toFloat())
    }
}
