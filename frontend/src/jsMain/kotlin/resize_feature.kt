import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.MouseEvent
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

fun findBlockBorder(blocks: List<Block>, x: Int, y: Int): ResizingRectangle? {
  return blocks.firstNotNullOfOrNull { block ->
    findHandle(block.rectangle, y, x)?.let { ResizingRectangle(block, it) }
  }
}

private fun findHandle(block: Rectangle, y: Int, x: Int): Rectangle.Handle? {
  val nearTop = (block.top - y).absoluteValue <= Rectangle.RESIZE_HANDLE_HALF_WIDTH && block.left - Rectangle.RESIZE_HANDLE_HALF_WIDTH < x && x < block.right + Rectangle.RESIZE_HANDLE_HALF_WIDTH
  val nearBottom = (block.bottom - y).absoluteValue <= Rectangle.RESIZE_HANDLE_HALF_WIDTH && block.left - Rectangle.RESIZE_HANDLE_HALF_WIDTH < x && x < block.right + Rectangle.RESIZE_HANDLE_HALF_WIDTH
  val nearLeft = (block.left - x).absoluteValue <= Rectangle.RESIZE_HANDLE_HALF_WIDTH && block.top - Rectangle.RESIZE_HANDLE_HALF_WIDTH < y && y < block.bottom + Rectangle.RESIZE_HANDLE_HALF_WIDTH
  val nearRight = (block.right - x).absoluteValue <= Rectangle.RESIZE_HANDLE_HALF_WIDTH && block.top - Rectangle.RESIZE_HANDLE_HALF_WIDTH < y && y < block.bottom + Rectangle.RESIZE_HANDLE_HALF_WIDTH
  val handle = when {
    nearTop && nearLeft -> Rectangle.Handle.TopLeft
    nearTop && nearRight -> Rectangle.Handle.TopRight
    nearBottom && nearLeft -> Rectangle.Handle.BottomLeft
    nearBottom && nearRight -> Rectangle.Handle.BottomRight
    nearTop && !nearLeft && !nearRight -> Rectangle.Handle.Top
    nearRight && !nearTop && !nearBottom -> Rectangle.Handle.Right
    nearBottom && !nearLeft && !nearRight -> Rectangle.Handle.Bottom
    nearLeft && !nearTop && !nearBottom -> Rectangle.Handle.Left
    else -> null
  }
  return handle
}

class ResizingRectangle(
  private val block: Block,
  val handle: Rectangle.Handle,
) {

  fun resizeTo(x: Int, y: Int) {
    block.resizeTo(handle, x, y)
  }
}

fun addResizeFeature(canvas: HTMLCanvasElement, blocks: List<Block>, redraw: () -> Unit) {
  var resizingBlock: ResizingRectangle? = null

  canvas.addEventListener("mousedown", { e ->
    check(e is MouseEvent) { "should be MouseEvent, but $e" }

    val mouseX = (e.clientX - canvas.getBoundingClientRect().left).roundToInt()
    val mouseY = (e.clientY - canvas.getBoundingClientRect().top).roundToInt()

    val foundBlockBorder = findBlockBorder(blocks, mouseX, mouseY)

    if (foundBlockBorder != null) {
      resizingBlock = foundBlockBorder
      console.log("resize block", foundBlockBorder)
    } else {
      console.log("block border not found at ($mouseX, $mouseY)")
    }
  })

  canvas.addEventListener("mouseup", {
    resizingBlock?.also {
      console.log("release block", it)
      resizingBlock = null
    }
  })

  canvas.addEventListener("mousemove", { e ->
    check(e is MouseEvent) { "should be MouseEvent, but $e" }

    val mouseX = (e.clientX - canvas.getBoundingClientRect().left).roundToInt()
    val mouseY = (e.clientY - canvas.getBoundingClientRect().top).roundToInt()

    resizingBlock?.also {
      it.resizeTo(mouseX, mouseY)

      redraw()

      canvas.style.cursor = it.handle.asCursorStyle()
    } ?: findBlockBorder(blocks, mouseX, mouseY)?.also {
      canvas.style.cursor = it.handle.asCursorStyle()
    }
  })
}

private fun Rectangle.Handle.asCursorStyle(): String = when (this) {
  Rectangle.Handle.Left -> "ew-resize"
  Rectangle.Handle.TopLeft -> "nwse-resize"
  Rectangle.Handle.Top -> "ns-resize"
  Rectangle.Handle.TopRight -> "nesw-resize"
  Rectangle.Handle.Right -> "ew-resize"
  Rectangle.Handle.BottomRight -> "nwse-resize"
  Rectangle.Handle.Bottom -> "ns-resize"
  Rectangle.Handle.BottomLeft -> "nesw-resize"
}
