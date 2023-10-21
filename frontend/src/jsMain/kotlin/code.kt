import Rectangle.Companion.RESIZE_HANDLE_HALF_WIDTH
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.events.MouseEvent
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class Rectangle(var x: Int, var y: Int, width: Int, height: Int) {

  companion object {

    const val MIN_SIZE = 5
    const val RESIZE_HANDLE_HALF_WIDTH = 2
  }

  var width: Int
  var height: Int
  var left = x
  var right: Int
  var top = y
  var bottom: Int

  init {
    this.width = width.coerceAtLeast(MIN_SIZE)
    this.height = height.coerceAtLeast(MIN_SIZE)
    right = x + width
    bottom = y + height
  }

  fun contains(x: Int, y: Int): Boolean {
    return left < x && x < right && top < y && y < bottom
  }

  fun moveTo(x: Int, y: Int) {
    this.x = x
    this.y = y

    this.top = y
    this.bottom = y + this.height
    this.left = x
    this.right = x + this.width
  }

  enum class Handle {
    Left, TopLeft, Top, TopRight, Right, BottomRight, Bottom, BottomLeft
  }

  fun resizeTo(handle: Handle, x: Int, y: Int) {
    when (handle) {
      Handle.Left -> {
        this.width = (this.right - x).coerceAtLeast(MIN_SIZE)
        this.left = this.right - this.width
        this.x = this.left
      }

      Handle.TopLeft -> {
        resizeTo(Handle.Top, x, y)
        resizeTo(Handle.Left, x, y)
      }

      Handle.Top -> {
        this.height = (this.bottom - y).coerceAtLeast(MIN_SIZE)
        this.top = this.bottom - this.height
        this.y = this.top
      }

      Handle.TopRight -> {
        resizeTo(Handle.Top, x, y)
        resizeTo(Handle.Right, x, y)
      }

      Handle.Right -> {
        this.width = (x - this.left).coerceAtLeast(MIN_SIZE)
        this.right = this.left + this.width
      }

      Handle.BottomRight -> {
        resizeTo(Handle.Bottom, x, y)
        resizeTo(Handle.Right, x, y)
      }

      Handle.Bottom -> {
        this.height = (y - this.top).coerceAtLeast(MIN_SIZE)
        this.bottom = this.top + this.height
      }

      Handle.BottomLeft -> {
        resizeTo(Handle.Bottom, x, y)
        resizeTo(Handle.Left, x, y)
      }
    }
  }
}

fun CanvasRenderingContext2D.drawRectangle(rectangle: Rectangle) {
  strokeRect(
      rectangle.x.toDouble(),
      rectangle.y.toDouble(),
      rectangle.width.toDouble(),
      rectangle.height.toDouble(),
  )
}

fun addContextMenu(canvas: HTMLCanvasElement, onCreateNewBlock: (Int, Int) -> Unit) {
  val contextMenu = checkNotNull(document.getElementById(
      "context-menu")) { "document should contain a div with id 'context-menu'" } as HTMLDivElement

  fun closeContextMenu() {
    contextMenu.style.display = "none"
    while (contextMenu.firstChild != null) {
      contextMenu.lastChild?.also { contextMenu.removeChild(it) }
    }
  }
  canvas.addEventListener("contextmenu", { e ->
    console.log(e)
    check(e is MouseEvent) { "should be a MouseEvent, but $e" }

    closeContextMenu()

    contextMenu.style.top = "${e.clientY}px"
    contextMenu.style.left = "${e.clientX}px"
    contextMenu.style.display = "block"

    val createNewBlockButton = createButtonCreateNewBlock {
      onCreateNewBlock(e.clientX, e.clientY)
      closeContextMenu()
    }

    contextMenu.appendChild(createNewBlockButton)

    e.preventDefault()
  })
  canvas.addEventListener("click", { closeContextMenu() })
}

fun createButtonCreateNewBlock(onCreateNewBlock: () -> Unit): HTMLButtonElement {
  val button = document.createElement("button") as HTMLButtonElement
  button.textContent = "Create new block"
  button.addEventListener("click", { e ->
    onCreateNewBlock()
    e.preventDefault()
  })
  return button
}

fun findBlockForDragging(blocks: MutableList<Rectangle>, x: Int, y: Int): Rectangle? {
  return blocks.find {
    it.left + RESIZE_HANDLE_HALF_WIDTH < x && x < it.right - RESIZE_HANDLE_HALF_WIDTH && it.top + RESIZE_HANDLE_HALF_WIDTH < y && y < it.bottom - RESIZE_HANDLE_HALF_WIDTH
  }
}

class DraggingRectangle(
  private val rectangle: Rectangle,
  private val offsetX: Int,
  private val offsetY: Int,
) {

  fun moveTo(x: Int, y: Int) {
    rectangle.moveTo(x - offsetX, y - offsetY)
  }
}

fun addDragFeature(canvas: HTMLCanvasElement, blocks: MutableList<Rectangle>, redraw: () -> Unit) {
  var draggableBlock: DraggingRectangle? = null

  canvas.addEventListener("mousedown", { e ->
    check(e is MouseEvent) { "should be MouseEvent, but $e" }

    val mouseX = (e.clientX - canvas.getBoundingClientRect().left).roundToInt()
    val mouseY = (e.clientY - canvas.getBoundingClientRect().top).roundToInt()

    val block = findBlockForDragging(blocks, mouseX, mouseY)

    if (block != null) {
      draggableBlock = DraggingRectangle(
          block,
          mouseX - block.x,
          mouseY - block.y,
      )
      console.log("drag block", draggableBlock)
    } else {
      console.log("block not found at ($mouseX, $mouseY)")
    }
  })

  canvas.addEventListener("mouseup", {
    draggableBlock?.also {
      console.log("release block", it)
      draggableBlock = null
    }
  })

  canvas.addEventListener("mousemove", { e ->
    draggableBlock?.also {
      check(e is MouseEvent) { "should be MouseEvent, but $e" }

      val mouseX = (e.clientX - canvas.getBoundingClientRect().left).roundToInt()
      val mouseY = (e.clientY - canvas.getBoundingClientRect().top).roundToInt()

      it.moveTo(mouseX, mouseY)

      redraw()
    }
  })
}

fun findBlockBorder(blocks: List<Rectangle>, x: Int, y: Int): ResizingRectangle? {
  return blocks.firstNotNullOfOrNull { block ->
    val nearTop = (block.top - y).absoluteValue <= RESIZE_HANDLE_HALF_WIDTH && block.left - RESIZE_HANDLE_HALF_WIDTH < x && x < block.right + RESIZE_HANDLE_HALF_WIDTH
    val nearBottom = (block.bottom - y).absoluteValue <= RESIZE_HANDLE_HALF_WIDTH && block.left - RESIZE_HANDLE_HALF_WIDTH < x && x < block.right + RESIZE_HANDLE_HALF_WIDTH
    val nearLeft = (block.left - x).absoluteValue <= RESIZE_HANDLE_HALF_WIDTH && block.top - RESIZE_HANDLE_HALF_WIDTH < y && y < block.bottom + RESIZE_HANDLE_HALF_WIDTH
    val nearRight = (block.right - x).absoluteValue <= RESIZE_HANDLE_HALF_WIDTH && block.top - RESIZE_HANDLE_HALF_WIDTH < y && y < block.bottom + RESIZE_HANDLE_HALF_WIDTH
    when {
      nearTop && nearLeft -> ResizingRectangle(block, Rectangle.Handle.TopLeft)
      nearTop && nearRight -> ResizingRectangle(block, Rectangle.Handle.TopRight)
      nearBottom && nearLeft -> ResizingRectangle(block, Rectangle.Handle.BottomLeft)
      nearBottom && nearRight -> ResizingRectangle(block, Rectangle.Handle.BottomRight)
      nearTop && !nearLeft && !nearRight -> ResizingRectangle(block, Rectangle.Handle.Top)
      nearRight && !nearTop && !nearBottom -> ResizingRectangle(block, Rectangle.Handle.Right)
      nearBottom && !nearLeft && !nearRight -> ResizingRectangle(block, Rectangle.Handle.Bottom)
      nearLeft && !nearTop && !nearBottom -> ResizingRectangle(block, Rectangle.Handle.Left)
      else -> null
    }
  }
}

class ResizingRectangle(
  private val rectangle: Rectangle,
  private val handle: Rectangle.Handle,
) {

  fun resizeTo(x: Int, y: Int) {
    rectangle.resizeTo(handle, x, y)
  }
}

fun addResizeFeature(canvas: HTMLCanvasElement, blocks: List<Rectangle>, redraw: () -> Unit) {
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
    resizingBlock?.also {
      check(e is MouseEvent) { "should be MouseEvent, but $e" }

      val mouseX = (e.clientX - canvas.getBoundingClientRect().left).roundToInt()
      val mouseY = (e.clientY - canvas.getBoundingClientRect().top).roundToInt()

      it.resizeTo(mouseX, mouseY)

      redraw()
    }
  })
}

fun CanvasRenderingContext2D.draw(blocks: List<Rectangle>) {
  clearRect(0.0, 0.0, canvas.width.toDouble(), canvas.height.toDouble())
  for (block in blocks) {
    drawRectangle(block)
  }
}

fun init() {
  val canvasElement = checkNotNull(
      document.getElementById("canvas")) { "element with id 'canvas' is expected" }
  val canvas = checkNotNull(
      canvasElement as? HTMLCanvasElement) { "element with id 'canvas' should be of type HtmlCanvasElement" }
  canvas.width = window.innerWidth
  canvas.height = window.innerHeight

  val renderignContext = checkNotNull(canvas.getContext(
      "2d")) { "failed to obtain context '2d' from the HtmlCanvasElement with id 'canvas'" }
  val ctx = checkNotNull(
      renderignContext as? CanvasRenderingContext2D) { "failed to convert RenderingContext to CanvasRenderingContext2D" }
  val blocks = mutableListOf<Rectangle>()

  fun redraw() {
    ctx.draw(blocks)
  }

  addContextMenu(
      canvas = canvas,
      onCreateNewBlock = { x: Int, y: Int ->
        blocks.add(Rectangle(x, y, 50, 50))
        console.log(blocks)
        redraw()
      },
  )

  addDragFeature(canvas, blocks, ::redraw)

  addResizeFeature(canvas, blocks, ::redraw)

  redraw()
}

fun main() {
  window.onload = { init() }
}
