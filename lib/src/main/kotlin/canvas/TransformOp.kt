package cnedclub.sad.canvas

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.min

class TransformOp(
    private val target: CanvasOp,
    private val width: Dimension,
    private val height: Dimension,
    private val transform: Transform,
    private val outOfBoundsFill: Color
) : CanvasOp {
    override suspend fun run(ctx: CanvasOp.Context, parentWidth: Dimension, parentHeight: Dimension): Result<Canvas> {
        val inverse: Transform = transform.inverse() ?: return fail("Invalid transform matrix $transform")
        return target.run(ctx, parentWidth, parentHeight).mapResult { canvas ->
            if (transform.isIdentity) return Result.success(canvas)

            val width: UInt
            val height: UInt

            if (this.width.hasValue && this.height.hasValue) {
                width = this.width.orThrow()
                height = this.height.orThrow()
            } else {
                autoSize(canvas.width, canvas.height) { w, h ->
                    width = w
                    height = h
                }
            }

            Result.success(MutableCanvas.fill(width, height) { x, y ->
                inverse.transform(
                    x.toFloat() - width.toFloat() / 2f + .5f,
                    y.toFloat() - height.toFloat() / 2f + .5f
                ) { x2, y2 ->
                    val canvasX = x2 + canvas.width.toFloat() / 2f
                    val canvasY = y2 + canvas.height.toFloat() / 2f
                    if (canvasX < 0 || canvasY < 0) outOfBoundsFill else {
                        val px = canvasX.toUInt()
                        val py = canvasY.toUInt()
                        if (canvas.isInBoundary(px, py)) canvas[px, py] else outOfBoundsFill
                    }
                }
            })
        }
    }

    @OptIn(ExperimentalContracts::class)
    private inline fun autoSize(imageWidth: UInt, imageHeight: UInt, action: (width: UInt, height: UInt) -> Unit) {
        contract {
            callsInPlace(action, InvocationKind.EXACTLY_ONCE)
        }
        var xMin = 0f
        var xMax = 0f
        var yMin = 0f
        var yMax = 0f

        for (i in 0..3) {
            this.transform.transform(
                if (i % 2 == 0) imageWidth.toFloat() else -imageWidth.toFloat(),
                if (i / 2 == 0) imageHeight.toFloat() else -imageHeight.toFloat()
            ) { x, y ->
                if (x < xMin) xMin = x
                else if (x > xMax) xMax = x
                if (y < yMin) yMin = y
                else if (y > yMax) yMax = y
            }
        }

        action(((xMax - xMin) / 2f).toUInt(), ((yMax - yMin) / 2f).toUInt())
    }

    override fun toString() = "TransformOp(target=$target, width=$width, height=$height, transform=$transform)"
}