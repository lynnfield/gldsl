import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.Element
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.Node
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

  val connectors: MutableList<ConnectionPoint> = mutableListOf()

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

const val CONNECTION_POINT_RADIUS = 5

fun CanvasRenderingContext2D.draw(connectionPoint: ConnectionPoint, selected: ConnectionPoint?) {
  beginPath()
  arc(connectionPoint.x.toDouble(), connectionPoint.y.toDouble(),
      CONNECTION_POINT_RADIUS.toDouble(), 0.0, 2.0 * PI)
  if (connectionPoint === selected)
    fill()
  else
    stroke()
}

fun CanvasRenderingContext2D.draw(block: Block, selectedConnectionPoint: ConnectionPoint?) {
  draw(block.rectangle)
  block.connectors.forEach { draw(it, selectedConnectionPoint) }
}

fun CanvasRenderingContext2D.draw(blocks: List<Block>, selectedConnectionPoint: ConnectionPoint?) {
  clearRect(0.0, 0.0, canvas.width.toDouble(), canvas.height.toDouble())
  for (block in blocks) {
    draw(block, selectedConnectionPoint)
  }
}

fun CanvasRenderingContext2D.draw(blocksAndLinks: BlocksAndLinks) {
  save()
  translate(0.5, 0.5)
  draw(blocksAndLinks.blocks, blocksAndLinks.newLinkContext?.start)
  draw(blocksAndLinks.links)
  blocksAndLinks.newLinkContext?.let { draw(it) }
  draw(blocksAndLinks.contextMenu)
  restore()
}

fun CanvasRenderingContext2D.draw(newLinkContext: NewLinkContext) {
  beginPath()
  moveTo(newLinkContext.start.x.toDouble(), newLinkContext.start.y.toDouble())
  lineTo(newLinkContext.endX.toDouble(), newLinkContext.endY.toDouble())
  stroke()
}

fun CanvasRenderingContext2D.draw(connections: List<Link>) {
  connections.forEach { (start, end) ->
    beginPath()
    moveTo(start.x.toDouble(), start.y.toDouble())
    lineTo(end.x.toDouble(), end.y.toDouble())
    stroke()
  }
}

typealias Link = Pair<ConnectionPoint, ConnectionPoint>

class BlocksAndLinks(
  val blocks: MutableList<Block> = mutableListOf(),
  val links: MutableList<Link> = mutableListOf(),
  var contextMenu: ContextMenu? = null,
  val contextMenuItems: MutableList<ContextMenu.Item> = mutableListOf(),
  var newLinkContext: NewLinkContext? = null,
)

class NewLinkContext(
  val start: ConnectionPoint,
  var endX: Int,
  var endY: Int,
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
  val blocksAndLinks = buildBlocksAndLinks()
  val blocks = blocksAndLinks.blocks

  fun redraw() = ctx.draw(blocksAndLinks)

  addContextMenuFeature(canvas, blocksAndLinks, ::redraw)

  addCreateNewBlockFeature(blocksAndLinks, ::redraw)

  setCursorToDefaultOnMove(canvas)

  addDragFeature(canvas, blocks, ::redraw)

  addResizeFeature(canvas, blocks, ::redraw)

  addLinkFeature(canvas, blocksAndLinks, ::redraw)

  redraw()
}

fun addLinkFeature(canvas: HTMLCanvasElement, blocksAndLinks: BlocksAndLinks, redraw: () -> Unit) {
  // right-click -> check if border -> add context menu action "create connection"
  canvas.addEventListener("contextmenu", { e ->
    e as MouseEvent

    val x = e.x(canvas)
    val y = e.y(canvas)

    val border = findBlockBorder(blocksAndLinks.blocks, x, y)
    val position = border?.toCoordinateOnSide(x, y)

    if (position != null) {
      // "create connection" -> create connection point -> start "creating connection" procedure
      blocksAndLinks.contextMenuItems.add(object : ContextMenu.Item {
        override fun createElement(contextMenuDiv: HTMLDivElement): Node {
          val button = document.createElement("button") as HTMLButtonElement
          button.textContent = "Add connection point"
          button.addEventListener("click", { e ->
            e as MouseEvent
            val block = border.block
            val connectionPoint = ConnectionPoint(position, block)
            block.connectors.add(connectionPoint)
            blocksAndLinks.newLinkContext =
                NewLinkContext(connectionPoint, e.x(canvas), e.y(canvas))
            blocksAndLinks.contextMenuItems.remove(this)
          })
          return button
        }
      })
      redraw()
    }
  })

  // "create connection" procedure:
  // - draw a line between "selected connection point" and "mouse"
  canvas.addEventListener("mousemove", { e ->
    blocksAndLinks.newLinkContext?.apply {
      e as MouseEvent

      endX = e.x(canvas)
      endY = e.y(canvas)

      redraw()
    }
  })
  // - "click" -> check if border -> add "connection point" -> create link between "selected connection point" and "connection point"
  canvas.addEventListener("click", { e ->
    blocksAndLinks.newLinkContext?.also { newLinkContext ->
      e as MouseEvent

      val x = e.x(canvas)
      val y = e.y(canvas)

      val border = findBlockBorder(blocksAndLinks.blocks, x, y)
      if (border?.block != null && border.block !== newLinkContext.start.block) {
        val coordinateOnSide = border.toCoordinateOnSide(x, y)
        val connectionPoint = ConnectionPoint(coordinateOnSide, border.block)
        border.block.connectors.add(connectionPoint)
        blocksAndLinks.links.add(Link(newLinkContext.start, connectionPoint))
        blocksAndLinks.newLinkContext = null
        redraw()
      }
    }
  })
}

private fun ResizingRectangle.toCoordinateOnSide(x: Int, y: Int): CoordinateOnSide {
  val rectangle = block.rectangle
  return when (handle) {
    Rectangle.Handle.Left, Rectangle.Handle.TopLeft ->
      CoordinateOnSide(CoordinateOnSide.Side.Left, Percent(y - rectangle.y, rectangle.height))

    Rectangle.Handle.Top, Rectangle.Handle.TopRight ->
      CoordinateOnSide(CoordinateOnSide.Side.Top, Percent(x - rectangle.x, rectangle.width))

    Rectangle.Handle.Right, Rectangle.Handle.BottomRight ->
      CoordinateOnSide(CoordinateOnSide.Side.Right, Percent(y - rectangle.y, rectangle.height))

    Rectangle.Handle.Bottom, Rectangle.Handle.BottomLeft ->
      CoordinateOnSide(CoordinateOnSide.Side.Bottom, Percent(x - rectangle.x, rectangle.width))
  }
}

fun MouseEvent.x(element: Element): Int =
    (clientX - element.getBoundingClientRect().left).roundToInt()

fun MouseEvent.y(element: Element): Int =
    (clientY - element.getBoundingClientRect().top).roundToInt()

private fun buildBlocksAndLinks(): BlocksAndLinks {
  //region test data
  val block1 = Block(Rectangle(300, 300, 100, 100))
  block1.connectors.add(
      ConnectionPoint(CoordinateOnSide(CoordinateOnSide.Side.Top, Percent(3, 10)), block1))
  block1.connectors.add(
      ConnectionPoint(CoordinateOnSide(CoordinateOnSide.Side.Bottom, Percent(6, 10)), block1))
  block1.connectors.add(
      ConnectionPoint(CoordinateOnSide(CoordinateOnSide.Side.Left, Percent(12.5, 100)), block1))
  val connectionPoint1 = ConnectionPoint(
      CoordinateOnSide(CoordinateOnSide.Side.Right, Percent(87, 100)), block1)
  block1.connectors.add(connectionPoint1)

  val block2 = Block(Rectangle(600, 600, 100, 100))
  block1.connectors.add(
      ConnectionPoint(CoordinateOnSide(CoordinateOnSide.Side.Top, Percent(3, 10)), block2))
  block1.connectors.add(
      ConnectionPoint(CoordinateOnSide(CoordinateOnSide.Side.Bottom, Percent(6, 10)), block2))
  val connectionPoint2 = ConnectionPoint(
      CoordinateOnSide(CoordinateOnSide.Side.Left, Percent(12.5, 100)), block2)
  block1.connectors.add(connectionPoint2)
  block1.connectors.add(
      ConnectionPoint(CoordinateOnSide(CoordinateOnSide.Side.Right, Percent(87, 100)), block2))
  //endregion
  return BlocksAndLinks(
      blocks = mutableListOf(block1, block2),
      links = mutableListOf(Link(connectionPoint1, connectionPoint2)),
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
