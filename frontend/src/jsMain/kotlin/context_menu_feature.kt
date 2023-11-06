import kotlinx.browser.document
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.Node
import org.w3c.dom.events.MouseEvent

fun addContextMenuFeature(canvas: HTMLCanvasElement, blocksAndLinks: BlocksAndLinks,
  redraw: () -> Unit) {
  canvas.addEventListener("contextmenu", { e ->
    console.log(e)
    check(e is MouseEvent) { "should be a MouseEvent, but $e" }

    blocksAndLinks.contextMenu = ContextMenu(
        e.clientY,
        e.clientX,
        items = blocksAndLinks.contextMenuItems,
    )

    redraw()

    e.preventDefault()
    e.stopPropagation()
  })
  canvas.addEventListener("click", {
    blocksAndLinks.contextMenu = null
    redraw()
  })
  val contextMenuDiv = checkNotNull(document.getElementById("context-menu") as HTMLDivElement?) {
    "document should contain a div with id 'context-menu'"
  }
  contextMenuDiv.addEventListener("click", {
    blocksAndLinks.contextMenu = null
    redraw()
  })
}

class ContextMenu(
  val y: Int,
  val x: Int,
  val items: List<Item>,
) {

  fun interface Item {

    fun createElement(contextMenuDiv: HTMLDivElement): Node
  }
}

fun draw(contextMenu: ContextMenu?) {
  val contextMenuDiv = checkNotNull(document.getElementById("context-menu") as HTMLDivElement?) {
    "document should contain a div with id 'context-menu'"
  }

  while (contextMenuDiv.firstChild != null) {
    contextMenuDiv.lastChild?.also { contextMenuDiv.removeChild(it) }
  }

  if (contextMenu == null) {
    contextMenuDiv.style.display = "none"
  } else {
    contextMenuDiv.style.top = "${contextMenu.y}px"
    contextMenuDiv.style.left = "${contextMenu.x}px"
    contextMenuDiv.style.display = "block"

    contextMenu.items.forEach {
      contextMenuDiv.appendChild(it.createElement(contextMenuDiv))
    }
  }
}
