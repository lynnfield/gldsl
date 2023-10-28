import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.events.MouseEvent

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

class Block(val rectangle: Rectangle) {

  fun moveTo(x: Int, y: Int) = rectangle.moveTo(x, y)
  fun resizeTo(handle: Rectangle.Handle, x: Int, y: Int) = rectangle.resizeTo(handle, x, y)
}

fun CanvasRenderingContext2D.draw(rectangle: Rectangle) {
  strokeRect(
      rectangle.x.toDouble(),
      rectangle.y.toDouble(),
      rectangle.width.toDouble(),
      rectangle.height.toDouble(),
  )
}

fun CanvasRenderingContext2D.draw(block: Block) {
  draw(block.rectangle)
}

fun addContextMenu(canvas: HTMLCanvasElement, onCreateNewBlock: (Int, Int) -> Unit) {
  val contextMenu = checkNotNull(document.getElementById("context-menu")) {
    "document should contain a div with id 'context-menu'"
  } as HTMLDivElement

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

fun CanvasRenderingContext2D.draw(blocks: List<Block>) {
  clearRect(0.0, 0.0, canvas.width.toDouble(), canvas.height.toDouble())
  for (block in blocks) {
    draw(block)
  }
}

fun init() {
  val canvasElement = checkNotNull(document.getElementById("canvas")) {
    "element with id 'canvas' is expected"
  }
  val canvas = checkNotNull(canvasElement as? HTMLCanvasElement) {
    "element with id 'canvas' should be of type HtmlCanvasElement"
  }
  canvas.width = window.innerWidth
  canvas.height = window.innerHeight

  val renderingContext = checkNotNull(canvas.getContext("2d")) {
    "failed to obtain context '2d' from the HtmlCanvasElement with id 'canvas'"
  }
  val ctx = checkNotNull(renderingContext as? CanvasRenderingContext2D) {
    "failed to convert RenderingContext to CanvasRenderingContext2D"
  }
  val blocks = mutableListOf<Block>()

  fun redraw() {
    ctx.draw(blocks)
  }

  addContextMenu(
      canvas = canvas,
      onCreateNewBlock = { x: Int, y: Int ->
        blocks.add(Block(Rectangle(x, y, 50, 50)))
        console.log(blocks)
        redraw()
      },
  )

  setCursorToDefaultOnMove(canvas)

  addDragFeature(canvas, blocks, ::redraw)

  addResizeFeature(canvas, blocks, ::redraw)

  redraw()
}

private fun setCursorToDefaultOnMove(canvas: HTMLCanvasElement) {
  canvas.addEventListener("mousemove", {
    canvas.style.cursor = "default"
  })
}

fun main() {
  window.onload = { init() }
}
