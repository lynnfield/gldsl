import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.MouseEvent

class GraphicalLayer<KeyType : Hashable>(
  val canvas: HTMLCanvasElement,
  val onClick: GraphicalLayer<KeyType>.(ClickEvent<KeyType>) -> Unit
) {

  sealed interface ClickEvent<KeyType : Hashable>
  data class EmptySpaceClicked<KeyType : Hashable>(val x: Int, val y: Int) : ClickEvent<KeyType>
  data class PrimitiveClicked<KeyType : Hashable>(
    val path: List<KeyType>,
    val x: Int,
    val y: Int,
    val primitives: List<Primitive>
  ) : ClickEvent<KeyType>

  private val drawingContext = canvas.getContext("2d") as CanvasRenderingContext2D

  sealed interface KeyOrRoot<out KeyType : Hashable> : Hashable
  data object RootKey : KeyOrRoot<Nothing>
  data class Key<KeyType : Hashable>(val value: KeyType) : KeyOrRoot<KeyType>

  private val root = Stack(0, 0, canvas.height, canvas.width)
  private val hierarchy = mutableMapOf<Stack, MutableList<Primitive>>(root to mutableListOf())
  private val primitivesKeys = mutableMapOf<Primitive, KeyOrRoot<KeyType>>(root to RootKey)

  //region primitives' extensions
  private fun Stack.add(primitive: Primitive, key: KeyType?) {
    hierarchy.getOrPut(this) { mutableListOf() }.add(primitive)
    if (key != null) primitivesKeys[primitive] = Key(key)
  }

  private fun Stack.recalculateSize() {
    hierarchy[this]?.also { children ->
      width = children.maxOf { it.x + it.width } - children.minOf { it.x }
      height = children.maxOf { it.y + it.height } - children.minOf { it.y }
    }
  }

  private fun Primitive.findAt(targetX: Int, targetY: Int): List<Primitive> {
    return when (this) {
      is Rectangle -> findAt(targetX, targetY)
      is Stack -> findAt(targetX, targetY)
    }
  }

  private fun Rectangle.findAt(targetX: Int, targetY: Int): List<Primitive> {
    return listOfNotNull(takeIf { targetX - x in 0..width && targetY - y in 0..height })
  }

  private fun Stack.findAt(targetX: Int, targetY: Int): List<Primitive> {
    val localX = targetX - x
    val localY = targetY - y
    return if (localX in 0..width && localY in 0..height) {
      listOf(this) + hierarchy.getOrPut(this) { mutableListOf() }
        .flatMap { it.findAt(localX, localY) }
    } else {
      emptyList()
    }
  }
  //endregion

  private var currentAction: CurrentAction? = null

  init {
    drawingContext.fillStyle = "rgb(30 31 34)"
    drawingContext.strokeStyle = "rgb(160 160 160)"
    canvas.addEventListener("mousedown", { event ->
      event as MouseEvent

      currentAction = root.findAt(event.clientX, event.clientY)
        .filterIsInstance<Stack>()
        .minus(root) // root is not draggable, yet
        .lastOrNull()
        ?.let { Moving(it, event.clientX - it.x, event.clientY - it.y) }
    })
    canvas.addEventListener("mousemove", { event ->
      event as MouseEvent

      when (val action = currentAction) {
        is Moving -> action.moveTo(event.clientX, event.clientY)
        is Connecting -> {
          action.pointerX = event.clientX
          action.pointerY = event.clientY
        }

        null -> Unit
      }

      currentAction?.also { redraw() }
    })
    canvas.addEventListener("mouseup", { event ->
      when (val action = currentAction) {
        is Moving -> {
          currentAction = null
          if (!action.dirty) {
            event as MouseEvent

            val primitives = root.findAt(event.clientX, event.clientY)
            val keys = primitives.mapNotNull { (primitivesKeys[it] as? Key<KeyType>)?.value }
            if (keys.isNotEmpty()) {
              onClick(PrimitiveClicked(keys, event.clientX, event.clientY, primitives))
            } else {
              onClick(EmptySpaceClicked(event.clientX, event.clientY))
            }
          }
        }

        is Connecting -> {
          event as MouseEvent

          val primitives = root.findAt(event.clientX, event.clientY)
          val keys = primitives.mapNotNull { (primitivesKeys[it] as? Key<KeyType>)?.value }
          if (keys.isNotEmpty()) {
            onClick(PrimitiveClicked(keys, event.clientX, event.clientY, primitives))
          } else {
            onClick(EmptySpaceClicked(event.clientX, event.clientY))
          }
        }

        null -> {
          event as MouseEvent

          val primitives = root.findAt(event.clientX, event.clientY)
          val keys = primitives.mapNotNull { (primitivesKeys[it] as? Key<KeyType>)?.value }
          if (keys.isNotEmpty()) {
            onClick(PrimitiveClicked(keys, event.clientX, event.clientY, primitives))
          } else {
            onClick(EmptySpaceClicked(event.clientX, event.clientY))
          }
        }
      }
    })
  }

  sealed interface CurrentAction

  // todo not working when a Stack moved about a half below bottom fo the screen (canvas?)
  private class Moving(
    val primitive: Primitive,
    val offsetX: Int,
    val offsetY: Int,
  ) : CurrentAction {

    var dirty: Boolean = false
      private set

    fun moveTo(x: Int, y: Int) {
      primitive.x = x - offsetX
      primitive.y = y - offsetY
      dirty = true
    }
  }

  private class Connecting(
    val primitive: Primitive,
    val offsetX: Int,
    val offsetY: Int,
    var pointerX: Int = offsetX,
    var pointerY: Int = offsetY,
  ) : CurrentAction

  sealed interface Primitive : Hashable

  data class Rectangle(var x: Int, var y: Int, val width: Int, val height: Int) : Primitive {

    // todo find another way to use primitives as keys
    private val key = intArrayOf(x, y, width, height)
    override fun equals(other: Any?) = (other as? Rectangle)?.key.contentEquals(key)
    override fun hashCode() = key.hashCode()
  }

  data class Stack(var x: Int, var y: Int, var width: Int, var height: Int) : Primitive {

    // todo find another way to use primitives as keys
    private val key = intArrayOf(x, y, width, height)
    override fun equals(other: Any?) = (other as? Stack)?.key.contentEquals(key)
    override fun hashCode() = key.hashCode()
  }

  @DslMarker
  annotation class StackBuilderDsl

  @StackBuilderDsl
  inner class StackBuilder(
    private val add: (key: KeyType?, primitive: Primitive) -> Unit,
  ) {

    fun rectangle(tag: KeyType? = null, x: Int = 0, y: Int = 0, width: Int = 100,
      height: Int = 100) {
      add(tag, Rectangle(x, y, width, height))
    }
  }

  fun EmptySpaceClicked<KeyType>.createStackHereFor(tag: KeyType,
    buildStack: StackBuilder.() -> Unit) {
    Stack(x, y, 0, 0).also {
      root.add(it, tag)
      StackBuilder { key, primitive -> it.add(primitive, key) }.buildStack()
      it.recalculateSize()
    }

    redraw()
  }

  fun PrimitiveClicked<KeyType>.connectFromHere() {
    currentAction = Connecting(primitives.last(), x, y)
    redraw()
  }

  fun redraw() {
    window.requestAnimationFrame {
      with(drawingContext) {
        save()
        fillRect(0.0, 0.0, canvas.width.toDouble(), canvas.height.toDouble())
        translate(0.5, 0.5)
        draw(root)
        when (val action = currentAction) {
          is Connecting -> {
            beginPath()
            moveTo(action.offsetX.toDouble(), action.offsetY.toDouble())
            lineTo(action.pointerX.toDouble(), action.pointerY.toDouble())
            stroke()
          }

          is Moving, null -> Unit
        }
        restore()
      }
    }
  }

  private fun CanvasRenderingContext2D.draw(primitives: List<Primitive>) {
    for (primitive in primitives) {
      draw(primitive)
    }
  }

  private fun CanvasRenderingContext2D.draw(primitive: Primitive) {
    when (primitive) {
      is Rectangle -> draw(primitive)
      is Stack -> draw(primitive)
    }
  }

  private fun CanvasRenderingContext2D.draw(rectangle: Rectangle) {
    strokeRect(
        rectangle.x.toDouble(),
        rectangle.y.toDouble(),
        rectangle.width.toDouble(),
        rectangle.height.toDouble(),
    )
  }

  private fun CanvasRenderingContext2D.draw(stack: Stack) {
    hierarchy[stack]?.also {
      save()
      translate(stack.x.toDouble(), stack.y.toDouble())
      draw(it)
      restore()
    }
  }

  companion object {

    private var Primitive.x: Int
      get() = when (this) {
        is Rectangle -> x
        is Stack -> x
      }
      set(value) = when (this) {
        is Rectangle -> x = value
        is Stack -> x = value
      }

    private var Primitive.y: Int
      get() = when (this) {
        is Rectangle -> y
        is Stack -> y
      }
      set(value) = when (this) {
        is Rectangle -> y = value
        is Stack -> y = value
      }

    private val Primitive.width: Int
      get() = when (this) {
        is Rectangle -> width
        is Stack -> width
      }

    private val Primitive.height: Int
      get() = when (this) {
        is Rectangle -> height
        is Stack -> height
      }
  }
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

  val randomName = { generateSequence("tag"::random).take(5).joinToString("") }
  val graphicalLayer: GraphicalLayer<StringTag> = GraphicalLayer(
      canvas = canvas,
      onClick = { e ->
        when (e) {
          // todo add feature to add names on creation and rename block
          // todo add feature to add names and rename input and output
          is GraphicalLayer.EmptySpaceClicked -> e.createStackHereFor(StringTag(randomName())) {
            rectangle(StringTag("name"), height = 30, width = 200)
            rectangle(StringTag("input"), y = 30, height = 100, width = 30)
            rectangle(StringTag("output"), x = 170, y = 30, height = 100, width = 30)
            rectangle(StringTag("body"), x = 30, y = 30, height = 100, width = 140)
          }

          is GraphicalLayer.PrimitiveClicked -> {
            console.log(e.path.joinToString(" > "))
            // todo add behaviour like "click-to-connect"
            //  add behaviour to connect to selected target
            //  add behaviour to cancel connection when clicked outside
            e.connectFromHere()
            // todo add behaviour like "click-to-rename"
          }
        }
      },
  )

  graphicalLayer.redraw()
}
