import kotlinx.browser.document
import org.w3c.dom.HTMLButtonElement

fun addCreateNewBlockFeature(blocksAndLinks: BlocksAndLinks, redraw: () -> Unit) {
  blocksAndLinks.contextMenuItems.add(ContextMenu.Item {
    val button = document.createElement("button") as HTMLButtonElement
    button.textContent = "Create new block"
    button.addEventListener("click", { e ->
      blocksAndLinks.blocks.add(Block(Rectangle(it.offsetLeft, it.offsetTop, 50, 50)))
      console.log(blocksAndLinks.blocks)
      redraw()
    })
    button
  })
}
