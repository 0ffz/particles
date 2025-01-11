import extensions.FPSDisplay
import math.calculateNetForce
import math.newPosition
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.Random
import org.openrndr.math.Vector2
import org.openrndr.math.clamp
import org.openrndr.math.mod
import kotlin.math.pow

fun main() = application {
    configure {
        width = 1000
        height = 1000
        windowResizable = true
    }
    program {
        val area = drawer.bounds.offsetEdges(0.0)
        var positions = Array(targetCount) {
            Random.point(area)
        }

        val width = area.width.toInt()
        val gridSize = (2.5 * sigma).toInt()
        val height = area.height.toInt()

        var prevPositions = positions.copyOf()
        val gridWidth = (width / gridSize)// + 1
        val gridHeight = (height / gridSize)// + 1
        val gridCells = Array((gridWidth + 1) * (gridHeight + 1)) {
            mutableListOf<Vector2>()
        }
        val randomVel = 100.0
        for (i in 0 until targetCount) {
            prevPositions[i] += Random.vector2(-randomVel * deltaT, randomVel * deltaT)
        }

        extend {
            drawer.clear(ColorRGBa.GRAY)
            drawer.fill = ColorRGBa.WHITE
            drawer.strokeWeight = 0.5

            // Clear grid
            for (i in gridCells.indices) gridCells[i].clear()

            // Fill grid
            for (i in 0 until targetCount) {
                val xGrid = (positions[i].x / gridSize).toInt()
                val yGrid = (positions[i].y / gridSize).toInt()
                gridCells[xGrid * gridWidth + yGrid] += positions[i]
            }
            val forces = Array(targetCount) {
                Vector2.ZERO
            }
            positions.forEachIndexed { i, position ->
                val gridX = (position.x / gridSize).toInt()
                val gridY = (position.y / gridSize).toInt()
                val gridCellId = gridX * gridWidth + gridY
//                val nearbyPositions =
//                val nearbyPositions = positions.toList()
                val netForce = calculateNetForce(
                    position,
                    gridCells[gridCellId],
                    gridCells.getOrNull(gridCellId - 1),
                    gridCells.getOrNull(gridCellId + 1),
                    gridCells.getOrNull(gridCellId - gridWidth),
                    gridCells.getOrNull(gridCellId + gridWidth),
                    gridCells.getOrNull(gridCellId - gridWidth - 1),
                    gridCells.getOrNull(gridCellId - gridWidth + 1),
                    gridCells.getOrNull(gridCellId + gridWidth - 1),
                    gridCells.getOrNull(gridCellId + gridWidth + 1),
                )
                val newPosition = newPosition(
                    position = position,
                    prev = prevPositions[i],
                    netForce = netForce,
                    deltaT = deltaT
                )
//                drawer.lineSegment(position, position + (netForce * 10.0))
//                drawer.text("Distance: " + positions[0].distanceTo(positions[1]).toString(), 10.0, 10.0)
//                drawer.text("Net force: ${netForce.length}", 10.0, 20.0)
//                positions[i] = newPosition
                forces[i] = netForce * deltaT.pow(2)
                prevPositions[i] = newPosition.mod(area.dimensions)
            }

            val new = prevPositions
            prevPositions = positions
            positions = new

            drawer.circles {
                for (i in 0 until targetCount) {
                    circle(positions[i], sigma / 2)
                }
            }
            // draw forces as lines in one batch
            if(showForceLines) {
                val visualForceClamp = 100.0
                drawer.lineSegments(forces.flatMapIndexed { i, force ->
                    listOf(
                        positions[i],
                        positions[i] + (force * 10.0).clamp(
                            Vector2(-visualForceClamp, -visualForceClamp),
                            Vector2(visualForceClamp, visualForceClamp)
                        )
                    )
                })
            }
        }
    }
}
