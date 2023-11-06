import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.MouseEvent

fun findBlockForDragging(blocks: List<Block>, x: Int, y: Int): Block? =
    blocks.find { it.rectangle.isInsideDragArea(x, y) }

private fun Rectangle.isInsideDragArea(x: Int, y: Int) =
    left + Rectangle.RESIZE_HANDLE_HALF_WIDTH < x && x < right - Rectangle.RESIZE_HANDLE_HALF_WIDTH && top + Rectangle.RESIZE_HANDLE_HALF_WIDTH < y && y < bottom - Rectangle.RESIZE_HANDLE_HALF_WIDTH

class DraggingBlock(
  private val block: Block,
  draggingPointX: Int,
  draggingPointY: Int,
) {

  private val offsetX: Int = draggingPointX - block.rectangle.x
  private val offsetY: Int = draggingPointY - block.rectangle.y

  fun moveTo(x: Int, y: Int) {
    block.moveTo(x - offsetX, y - offsetY)
  }
}

fun addDragFeature(canvas: HTMLCanvasElement, blocks: List<Block>, redraw: () -> Unit) {
  var draggableBlock: DraggingBlock? = null

  canvas.addEventListener("mousedown", { e ->
    check(e is MouseEvent) { "should be MouseEvent, but $e" }

    if (e.button == 0.toShort()) {
      val mouseX = e.x(canvas)
      val mouseY = e.y(canvas)

      val block = findBlockForDragging(blocks, mouseX, mouseY)

      if (block != null) {
        draggableBlock = DraggingBlock(block, mouseX, mouseY)
        console.log("drag block", draggableBlock)
      } else {
        console.log("block not found at ($mouseX, $mouseY)")
      }
    }
  })

  canvas.addEventListener("mouseup", {
    draggableBlock?.also {
      console.log("release block", it)
      draggableBlock = null
    }
  })

  canvas.addEventListener("mousemove", { e ->
    check(e is MouseEvent) { "should be MouseEvent, but $e" }

    val mouseX = e.x(canvas)
    val mouseY = e.y(canvas)

    draggableBlock?.also {
      it.moveTo(mouseX, mouseY)

      redraw()

      canvas.style.cursor = "move"
    } ?: findBlockForDragging(blocks, mouseX, mouseY)?.also {
      canvas.style.cursor = "move"
    }
  })
}
