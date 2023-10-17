import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.events.MouseEvent
import kotlin.math.roundToInt

class Rectangle(var x: Int, var y: Int, var width: Int, var height: Int) {

  var left = x
  var right = x + width
  var top = y
  var bottom = y + height

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
    left, topleft, top, topright, right, bottomright, bottom, bottomleft
  }

  fun resizeTo(handle: Handle, x: Int, y: Int) {
    when (handle) {
      Handle.left -> {
        this.x = x
        this.left = x
        this.width = this.right - this.left
        if (this.width < 5) {
          this.width = 5
          this.left = this.right - 5
          this.x = this.left
        }
      }

      Handle.topleft -> {
        resizeTo(Handle.top, x, y)
        resizeTo(Handle.left, x, y)
      }

      Handle.top -> {
        this.y = y
        this.top = y
        this.height = this.bottom - this.top
        if (this.height < 5) {
          this.height = 5
          this.top = this.bottom - 5
          this.y = this.top
        }
      }

      Handle.topright -> {
        resizeTo(Handle.top, x, y)
        resizeTo(Handle.right, x, y)
      }

      Handle.right -> {
        this.right = x
        this.width = this.right - this.left
        if (this.width < 5) {
          this.width = 5
          this.right = this.left + 5
        }
      }

      Handle.bottomright -> {
        resizeTo(Handle.bottom, x, y)
        resizeTo(Handle.right, x, y)
      }

      Handle.bottom -> {
        this.bottom = y
        this.height = this.bottom - this.top
        if (this.height < 5) {
          this.height = 5
          this.bottom = this.top + 5
        }
      }

      Handle.bottomleft -> {
        resizeTo(Handle.bottom, x, y)
        resizeTo(Handle.left, x, y)
      }
    }
  }
}

fun drawRectangle(ctx: CanvasRenderingContext2D, rectangle: Rectangle) {
  ctx.strokeRect(
      rectangle.x.toDouble(),
      rectangle.y.toDouble(),
      rectangle.width.toDouble(),
      rectangle.height.toDouble()
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

fun findBlock(blocks: MutableList<Rectangle>, x: Int, y: Int): Rectangle? {
  return blocks.find { it.contains(x, y) }
}

class DraggingRectangle(
  val rectangle: Rectangle,
  val offsetX: Int,
  val offsetY: Int
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

    val block = findBlock(blocks, mouseX, mouseY)

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
    if (block.top == y) {
      if (block.left == x) {
        return ResizingRectangle(block, Rectangle.Handle.topleft)
      } else if (block.right == x) {
        return ResizingRectangle(block, Rectangle.Handle.topright)
      } else {
        return ResizingRectangle(block, Rectangle.Handle.top)
      }
    } else if (block.right == x) {
      if (block.top == y) {
        return ResizingRectangle(block, Rectangle.Handle.topright)
      } else if (block.bottom == y) {
        return ResizingRectangle(block, Rectangle.Handle.bottomright)
      } else {
        return ResizingRectangle(block, Rectangle.Handle.right)
      }
    } else if (block.bottom == y) {
      if (block.left == x) {
        return ResizingRectangle(block, Rectangle.Handle.bottomleft)
      } else if (block.right == x) {
        return ResizingRectangle(block, Rectangle.Handle.bottomright)
      } else {
        return ResizingRectangle(block, Rectangle.Handle.bottom)
      }
    } else if (block.left == x) {
      if (block.top == y) {
        return ResizingRectangle(block, Rectangle.Handle.topleft)
      } else if (block.bottom == y) {
        return ResizingRectangle(block, Rectangle.Handle.bottomleft)
      } else {
        return ResizingRectangle(block, Rectangle.Handle.left)
      }
    } else {
      null
    }
  }
}

class ResizingRectangle(
  val rectangle: Rectangle,
  val handle: Rectangle.Handle,
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

  canvas.addEventListener("mousemove", {e ->
    resizingBlock?.also {
      check(e is MouseEvent) { "should be MouseEvent, but $e" }

      val mouseX = (e.clientX - canvas.getBoundingClientRect().left).roundToInt()
      val mouseY = (e.clientY - canvas.getBoundingClientRect().top).roundToInt()

      it.resizeTo(mouseX, mouseY)

      redraw()
    }
  })
}

fun draw(ctx: CanvasRenderingContext2D, blocks: List<Rectangle>) {
  ctx.clearRect(0.0, 0.0, ctx.canvas.width.toDouble(), ctx.canvas.height.toDouble())
  for (block in blocks) {
    drawRectangle(ctx, block)
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
    draw(ctx, blocks)
  }

  addContextMenu(
      canvas = canvas,
      onCreateNewBlock = { x: Int, y: Int ->
        blocks.add(Rectangle(x, y, 50, 50))
        console.log(blocks)
        redraw()
      }
  )

  addDragFeature(canvas, blocks, ::redraw)

  addResizeFeature(canvas, blocks, ::redraw)

  redraw()
}

fun main() {
  window.onload = { init() }
}
