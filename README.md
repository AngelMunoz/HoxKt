# HoxKt

> This is a kotlin PoC of my [Hox](https://github.com/AngelMunoz/Hox) project. If you're interested deeper details to this approach, feel free to check it out.

HoxKt is a kotlin PoC for an Async/Chunked HTML rendering engine. It's based on the [Hox](https://github.com/AngelMunoz/Hox) project the main goal of this project is to play and practice with kotlin however, it can grow to a full functional project as the core building blocks are already there. If you're interested in making this happen, feel free to contribute.

The original goals of Hox can be summarized in the following points

- Use the power of a programming language to create HTML documents
- Don't get limited by the DSL (like sometimes happens with `Kotlinx.html`)
- Side-by-side sync/async nodes
- Produce HTML in chunks rather than a single go (to leverage Browser's incremental rendering)

## Usage

Hox provides an `el` set of overloads in the `H.Companion` object to create HTML elements. The `el` function returns a `Node` object that can be rendered to a `String` using any of the two available rendering approaches. There's also helper functions for other nodes like `fragment`, `text`, `comment`, etc.

An `attr` extension function is provided in Nodes to add attributes to them.

```kotlin
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
            // simulate a heavy operation
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
  // you will be able to see how the HTML is rendered in chunks rather than a single go
  // very, very useful for large HTML documents, for slow connections or heavy operations.
  renderNodeFlow(root).collect { print(it) }
}
```
