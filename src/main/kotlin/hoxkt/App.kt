package hoxkt

import hoxkt.dsl.H.Companion.el
import hoxkt.dsl.H.Companion.fragment
import hoxkt.dsl.H.Companion.text
import hoxkt.dsl.attr
import hoxkt.rendering.renderNode
import hoxkt.rendering.renderNodeFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

suspend fun main() {

  val root =
    el("main",
      el("header", el("h1", "Hello world!")),
      el("article",
        el("p", "This is a paragraph!"),
        el("ul",
          List(10) { i ->
            el("li", text("This is list item $i"))
          }
        )
          .attr("id", "my-list")
          .attr("class", "my name is list"),
        el("ul", fragment(flow {
          for (i in 0..10) {
            emit(el("li", text("This is list item $i")))
            delay(500)
          }
        }))
      ),
      el("footer", el("p", "This is a footer!"))
    )

  val result = renderNode(root)
  println("Suspend version:\n\n")
  println(result)
  println("Flow version:\n\n")
  renderNodeFlow(root).collect { print(it) }
}
