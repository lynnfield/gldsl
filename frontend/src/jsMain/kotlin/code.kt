import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.events.MouseEvent
import kotlin.math.PI
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

class Block(
  val rectangle: Rectangle,
) {

  val connections: MutableList<ConnectionPoint> = mutableListOf()

  fun moveTo(x: Int, y: Int) = rectangle.moveTo(x, y)
  fun resizeTo(handle: Rectangle.Handle, x: Int, y: Int) = rectangle.resizeTo(handle, x, y)
}

value class Percent private constructor(private val percent: Double) {
  constructor(part: Number, whole: Number) : this(part.toDouble() / whole.toDouble())

  infix fun of(value: Int): Int = (value * percent).roundToInt()
}

class CoordinateOnSide(
  val side: Side,
  val fromStart: Percent,
) {

  enum class Side {
    Left, Top, Right, Bottom
  }
}

class ConnectionPoint(
  val position: CoordinateOnSide,
  val block: Block,
) {

  val x
    get() = when (position.side) {
      CoordinateOnSide.Side.Left -> block.rectangle.left
      CoordinateOnSide.Side.Right -> block.rectangle.right
      CoordinateOnSide.Side.Top, CoordinateOnSide.Side.Bottom -> block.rectangle.left + (position.fromStart of block.rectangle.width)
    }

  val y
    get() = when (position.side) {
      CoordinateOnSide.Side.Top -> block.rectangle.top
      CoordinateOnSide.Side.Bottom -> block.rectangle.bottom
      CoordinateOnSide.Side.Left, CoordinateOnSide.Side.Right -> block.rectangle.top + (position.fromStart of block.rectangle.height)
    }
}

fun CanvasRenderingContext2D.draw(rectangle: Rectangle) {
  strokeRect(
      rectangle.x.toDouble(),
      rectangle.y.toDouble(),
      rectangle.width.toDouble(),
      rectangle.height.toDouble(),
  )
}

fun CanvasRenderingContext2D.draw(connectionPoint: ConnectionPoint) {
  beginPath()
  arc(connectionPoint.x.toDouble(), connectionPoint.y.toDouble(), 5.0, 0.0, 2.0 * PI)
  stroke()
}

fun CanvasRenderingContext2D.draw(block: Block) {
  draw(block.rectangle)
  block.connections.forEach { draw(it) }
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

fun CanvasRenderingContext2D.draw(blocksAndConnections: BlocksAndConnections) {
  draw(blocksAndConnections.blocks)
  draw(blocksAndConnections.connections)
}

fun CanvasRenderingContext2D.draw(connections: List<Connector>) {
  connections.forEach { (start, end) ->
    beginPath()
    moveTo(start.x.toDouble(), start.y.toDouble())
    lineTo(end.x.toDouble(), end.y.toDouble())
    stroke()
  }
}

typealias Connector = Pair<ConnectionPoint, ConnectionPoint>

class BlocksAndConnections(
  val blocks: MutableList<Block> = mutableListOf(),
  val connections: MutableList<Connector> = mutableListOf(),
)

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
  val blocksAndConnections = buildBlocksAndConnections()
  val blocks = blocksAndConnections.blocks

  fun redraw() = ctx.draw(blocksAndConnections)

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

private fun buildBlocksAndConnections(): BlocksAndConnections {
  //region test data
  val block1 = Block(Rectangle(300, 300, 100, 100))
  block1.connections.add(
      ConnectionPoint(CoordinateOnSide(CoordinateOnSide.Side.Top, Percent(3, 10)), block1))
  block1.connections.add(
      ConnectionPoint(CoordinateOnSide(CoordinateOnSide.Side.Bottom, Percent(6, 10)), block1))
  block1.connections.add(
      ConnectionPoint(CoordinateOnSide(CoordinateOnSide.Side.Left, Percent(12.5, 100)), block1))
  val connectionPoint1 = ConnectionPoint(
      CoordinateOnSide(CoordinateOnSide.Side.Right, Percent(87, 100)), block1)
  block1.connections.add(connectionPoint1)

  val block2 = Block(Rectangle(600, 600, 100, 100))
  block1.connections.add(
      ConnectionPoint(CoordinateOnSide(CoordinateOnSide.Side.Top, Percent(3, 10)), block2))
  block1.connections.add(
      ConnectionPoint(CoordinateOnSide(CoordinateOnSide.Side.Bottom, Percent(6, 10)), block2))
  val connectionPoint2 = ConnectionPoint(
      CoordinateOnSide(CoordinateOnSide.Side.Left, Percent(12.5, 100)), block2)
  block1.connections.add(connectionPoint2)
  block1.connections.add(
      ConnectionPoint(CoordinateOnSide(CoordinateOnSide.Side.Right, Percent(87, 100)), block2))
  //endregion
  return BlocksAndConnections(
      blocks = mutableListOf(block1, block2),
      connections = mutableListOf(Connector(connectionPoint1, connectionPoint2))
  )
}

private fun setCursorToDefaultOnMove(canvas: HTMLCanvasElement) {
  canvas.addEventListener("mousemove", {
    canvas.style.cursor = "default"
  })
}

fun main() {
  window.onload = { init() }
}
