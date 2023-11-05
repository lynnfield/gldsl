import kotlinx.browser.document
import org.w3c.dom.HTMLButtonElement

fun addCreateNewBlockFeature(blocksAndConnections: BlocksAndConnections, redraw: () -> Unit) {
  blocksAndConnections.contextMenuItems.add(ContextMenu.Item {
    val button = document.createElement("button") as HTMLButtonElement
    button.textContent = "Create new block"
    button.addEventListener("click", { e ->
      blocksAndConnections.blocks.add(Block(Rectangle(it.offsetLeft, it.offsetTop, 50, 50)))
      console.log(blocksAndConnections.blocks)
      redraw()
    })
    button
  })
}
