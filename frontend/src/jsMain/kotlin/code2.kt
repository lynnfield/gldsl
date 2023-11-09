import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.MouseEvent

class GraphicalLayer<Key : Hashable>(
  val canvas: HTMLCanvasElement,
  val onClick: GraphicalLayer<Key>.(ClickEvent<Key>) -> Unit
) {

  sealed interface ClickEvent<Key : Hashable>
  data class EmptySpaceClicked<Key : Hashable>(val x: Int, val y: Int) : ClickEvent<Key>
  data class PrimitiveClicked<Key : Hashable>(val tag: Hashable) : ClickEvent<Key>

  private val drawingContext = canvas.getContext("2d") as CanvasRenderingContext2D
  private val primitives = mutableListOf<Primitive>()
  private val tagToPrimitive = mutableMapOf<Hashable, Primitive>()
  private val primitiveToTag = mutableMapOf<Primitive, Hashable>()

  init {
    drawingContext.fillStyle = "rgb(30 31 34)"
    drawingContext.strokeStyle = "rgb(160 160 160)"

    var movingContext: MovingContext? = null
    canvas.addEventListener("mousedown", { event ->
      event as MouseEvent

      movingContext = primitives
        .findLast { it.x <= event.clientX && event.clientX <= it.x + it.width && it.y <= event.clientY && event.clientY <= it.y + it.height }
        ?.let { MovingContext(it, event.clientX - it.x, event.clientY - it.y) }
    })
    canvas.addEventListener("mousemove", { event ->
      event as MouseEvent

      movingContext
        ?.moveTo(event.clientX, event.clientY)
        ?.also { redraw() }
    })
    canvas.addEventListener("mouseup", { event ->
      if (movingContext?.dirty != true) {
        event as MouseEvent

        val found = primitives
          .findLast { it.x <= event.clientX && event.clientX <= it.x + it.width && it.y <= event.clientY && event.clientY <= it.y + it.height }
          ?.let { primitiveToTag[it] }

        if (found != null) {
          onClick(PrimitiveClicked(found))
        } else {
          onClick(EmptySpaceClicked(event.clientX, event.clientY))
        }
      }
      movingContext = null
    })
  }

  inner class MovingContext(val primitive: Primitive, val offsetX: Int, val offsetY: Int) {

    var dirty: Boolean = false
      private set

    fun moveTo(x: Int, y: Int) {
      primitive.x = x - offsetX
      primitive.y = y - offsetY
      dirty = true
    }
  }

  sealed interface Primitive

  data class Rectangle(var x: Int = 0, var y: Int = 0, val width: Int = 100,
    val height: Int = 100) :
    Primitive

  data class Stack(var x: Int, var y: Int, val primitives: List<Primitive>) : Primitive {

    val width: Int = primitives.maxOf { it.x + it.width } - primitives.minOf { it.x }
    val height: Int = primitives.maxOf { it.y + it.height } - primitives.minOf { it.y }
  }

  fun EmptySpaceClicked<*>.createStackHereFor(tag: Key, item: Primitive, vararg items: Primitive) {
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

var GraphicalLayer.Primitive.x: Int
  get() = when (this) {
    is GraphicalLayer.Rectangle -> x
    is GraphicalLayer.Stack -> x
  }
  set(value) = when (this) {
    is GraphicalLayer.Rectangle -> x = value
    is GraphicalLayer.Stack -> x = value
  }

private var GraphicalLayer.Primitive.y: Int
  get() = when (this) {
    is GraphicalLayer.Rectangle -> y
    is GraphicalLayer.Stack -> y
  }
  set(value) = when (this) {
    is GraphicalLayer.Rectangle -> y = value
    is GraphicalLayer.Stack -> y = value
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

data class StringTag(val value: String) : Hashable

fun newInit() {
  val canvasElement = checkNotNull(document.getElementById("canvas")) {
    "element with id 'canvas' is expected"
  }
  val canvas = checkNotNull(canvasElement as? HTMLCanvasElement) {
    "element with id 'canvas' should be of type HtmlCanvasElement"
  }
  canvas.width = window.innerWidth
  canvas.height = window.innerHeight

  val graphicalLayer: GraphicalLayer<StringTag> = GraphicalLayer(
      canvas = canvas,
      onClick = { e ->
        when (e) {
          is GraphicalLayer.EmptySpaceClicked -> e.createStackHereFor(
              StringTag("tag"),
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
