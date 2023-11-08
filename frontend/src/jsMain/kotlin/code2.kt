import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.MouseEvent

class ObjectsLayer(
  val graphicalLayer: GraphicalLayer
) {

  class Block
  class ConnectionPoint
  class Link
}

class GraphicalLayer(
  val canvas: HTMLCanvasElement
) {

  private val drawingContext = canvas.getContext("2d") as CanvasRenderingContext2D
  private val primitives = mutableListOf<Primitive>()
  private val tagToPrimitive = mutableMapOf<Tag, Primitive>()

  interface Tag {

    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
  }

  sealed interface Primitive

  class Rectangle(val x: Int, val y: Int, val width: Int, val height: Int) : Primitive
  class Circle
  class Line

  inner class PrimitiveBuilder(val x: Int, val y: Int) {

    fun addRectangle(tag: Tag, width: Int = 100, height: Int = 100) {
      Rectangle(x, y, width, height).also {
        // todo can I use just the map?
        primitives.add(it)
        tagToPrimitive[tag] = it
      }
      redraw()
    }
  }

  fun addContextMenuListener(onContextMenu: PrimitiveBuilder.() -> Unit) {
    canvas.addEventListener("contextmenu", { event ->
      event as MouseEvent

      PrimitiveBuilder(event.clientX, event.clientY).onContextMenu()

      event.preventDefault()
    })
  }

  fun redraw() {
    window.requestAnimationFrame {
      with(drawingContext) {
        save()
        translate(0.5, 0.5)
        draw(primitives)
        restore()
      }
    }
  }

  fun CanvasRenderingContext2D.draw(primitives: List<Primitive>) {
    for (primitive in primitives) {
      draw(primitive)
    }
  }

  fun CanvasRenderingContext2D.draw(primitive: Primitive) {
    when (primitive) {
      is Rectangle -> draw(primitive)
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
}

fun newInit() {
  val canvasElement = checkNotNull(document.getElementById("canvas")) {
    "element with id 'canvas' is expected"
  }
  val canvas = checkNotNull(canvasElement as? HTMLCanvasElement) {
    "element with id 'canvas' should be of type HtmlCanvasElement"
  }
  canvas.width = window.innerWidth
  canvas.height = window.innerHeight

  val graphicalLayer = GraphicalLayer(canvas)

  graphicalLayer.addContextMenuListener {
    addRectangle(Tag)
  }
}

data object Tag : GraphicalLayer.Tag
