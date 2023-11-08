import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.MouseEvent

class GraphicalLayer(
  val canvas: HTMLCanvasElement,
  val onClick: GraphicalLayer.(ClickEvent) -> Unit
) {

  sealed interface ClickEvent
  inner class EmptySpaceClicked(val x: Int, val y: Int) : ClickEvent

  private val drawingContext = canvas.getContext("2d") as CanvasRenderingContext2D
  private val primitives = mutableListOf<Primitive>()
  private val tagToPrimitive = mutableMapOf<String, Primitive>()

  init {
    drawingContext.fillStyle = "rgb(30 31 34)"
    drawingContext.strokeStyle = "rgb(160 160 160)"
    canvas.addEventListener("click", { event ->
      event as MouseEvent

      onClick(EmptySpaceClicked(event.clientX, event.clientY))

      event.preventDefault()
    })
  }

  interface Tag {

    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
  }

  sealed interface Primitive

  class Rectangle(val x: Int = 0, val y: Int = 0, val width: Int = 100, val height: Int = 100) :
    Primitive

  class Stack(val x: Int, val y: Int, val primitives: List<Primitive>) : Primitive
  class Circle
  class Line

  fun EmptySpaceClicked.addObjects(tag: String, item: Primitive, vararg items: Primitive) {
    Stack(x, y, listOf(item) + items).also {
      primitives.add(it)
      tagToPrimitive[tag] = it
    }
    redraw()
  }

  fun redraw() {
    window.requestAnimationFrame {
      with(drawingContext) {
        save()
        fillRect(0.0, 0.0, canvas.width.toDouble(), canvas.height.toDouble())
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
      is Stack -> draw(primitive)
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

  fun CanvasRenderingContext2D.draw(stack: Stack) {
    save()
    translate(stack.x.toDouble(), stack.y.toDouble())
    stack.primitives.forEach { draw(it) }
    restore()
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

  val graphicalLayer = GraphicalLayer(
      canvas = canvas,
      onClick = { e ->
        when (e) {
          is GraphicalLayer.EmptySpaceClicked -> e.addObjects(
              "tag",
              GraphicalLayer.Rectangle(height = 50),
              GraphicalLayer.Rectangle(y = 50, height = 50),
          )
        }
      },
  )

  graphicalLayer.redraw()
}

data object Tag : GraphicalLayer.Tag
