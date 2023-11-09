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
  data class EmptySpaceClicked(val x: Int, val y: Int) : ClickEvent
  data class PrimitiveClicked(val tag: String) : ClickEvent


  private val drawingContext = canvas.getContext("2d") as CanvasRenderingContext2D
  private val primitives = mutableListOf<Primitive>()
  private val tagToPrimitive = mutableMapOf<String, Primitive>()
  private val primitiveToTag = mutableMapOf<Primitive, String>()

  init {
    drawingContext.fillStyle = "rgb(30 31 34)"
    drawingContext.strokeStyle = "rgb(160 160 160)"
    canvas.addEventListener("click", { event ->
      event as MouseEvent

      val found = primitives
        .findLast { it.x <= event.clientX && event.clientX <= it.x + it.width && it.y <= event.clientY && event.clientY <= it.y + it.height }
        ?.let { primitiveToTag[it] }

      if (found != null) {
        onClick(PrimitiveClicked(found))
      } else {
        onClick(EmptySpaceClicked(event.clientX, event.clientY))
      }

      event.preventDefault()
    })
  }

  sealed interface Primitive

  data class Rectangle(val x: Int = 0, val y: Int = 0, val width: Int = 100, val height: Int = 100) :
    Primitive

  data class Stack(val x: Int, val y: Int, val primitives: List<Primitive>) : Primitive {

    val width: Int = primitives.maxOf { it.x + it.width } - primitives.minOf { it.x }
    val height: Int = primitives.maxOf { it.y + it.height } - primitives.minOf { it.y }
  }

  fun EmptySpaceClicked.addObjects(tag: String, item: Primitive, vararg items: Primitive) {
    Stack(x, y, listOf(item) + items).also {
      primitives.add(it)
      tagToPrimitive[tag] = it
      primitiveToTag[it] = tag
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

val GraphicalLayer.Primitive.x: Int
  get() = when (this) {
    is GraphicalLayer.Rectangle -> x
    is GraphicalLayer.Stack -> x
  }

private val GraphicalLayer.Primitive.width: Int
  get() = when (this) {
    is GraphicalLayer.Rectangle -> width
    is GraphicalLayer.Stack -> width
  }

private val GraphicalLayer.Primitive.height: Int
  get() = when (this) {
    is GraphicalLayer.Rectangle -> height
    is GraphicalLayer.Stack -> height
  }

private val GraphicalLayer.Primitive.y: Int
  get() = when (this) {
    is GraphicalLayer.Rectangle -> y
    is GraphicalLayer.Stack -> y
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
              GraphicalLayer.Rectangle(height = 30, width = 200),
              GraphicalLayer.Rectangle(y = 30, height = 100, width = 30),
              GraphicalLayer.Rectangle(x = 170, y = 30, height = 100, width = 30),
              GraphicalLayer.Rectangle(x = 30, y = 30, height = 100, width = 140),
          )

          is GraphicalLayer.PrimitiveClicked -> {
            console.log(e)
          }
        }
      },
  )

  graphicalLayer.redraw()
}
