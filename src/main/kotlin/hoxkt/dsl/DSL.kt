package hoxkt.dsl

import hoxkt.types.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow


operator fun Node.plus(child: Node): Node {
  return when {
    this is El -> El(tag, attr, children + child)
    this is Text && child is Text -> Text(text + child.text, child.encoded)
    this is Comment && child is Comment -> Comment(comment + child.comment)
    this is FragmentFlow && child is FragmentFlow -> FragmentFlow(flow { emitAll(nodes); emitAll(child.nodes) })
    this is FragmentFlow -> FragmentFlow(flow { emitAll(nodes); emit(child) })
    this is LazyNode && child is LazyNode -> LazyNode { this.invoke() + child.invoke() }
    this is LazyNode -> LazyNode { this.invoke() + child }
    else -> this
  }
}

operator fun Node.plus(attr: Attribute): Node {
  return when (this) {
    is El -> this.copy(attr = this.attr + attr)
    is LazyNode -> LazyNode {
      val node = this.invoke()
      if (node is El) node.copy(attr = node.attr + attr) else node
    }

    else -> this
  }
}


fun Node.attr(name: String, value: String?): Node {
  return plus(Attribute(name, value))
}


sealed class H {
  companion object {

    fun text(text: String): Node {
      return Text(text, true)
    }

    fun raw(text: String): Node {
      return Text(text, false)
    }

    fun comment(comment: String): Node {
      return Comment(comment)
    }


    fun fragment(children: Flow<Node>): Node {
      return FragmentFlow(children)
    }

    fun fragment(children: Iterable<Node>): Node {
      return Fragment(children.toList())
    }

    fun fragment(vararg children: Node): Node {
      return Fragment(children.toList())
    }

    fun el(tag: String): Node {
      return El(tag, emptyList(), emptyList())
    }

    fun el(tag: String, vararg children: Node): Node {
      return El(tag, emptyList(), children.toList())
    }

    fun el(tag: String, fn: suspend () -> Node): Node {
      return El(tag, emptyList(), listOf(LazyNode(fn)))
    }

    fun el(tag: String, children: Iterable<Node>): Node {
      return El(tag, emptyList(), children.toList())
    }

    fun el(tag: String, attrs: Map<String, String?>, vararg children: Node): Node {
      return El(tag, attrs.map { Attribute(it.key, it.value) }, children.toList())
    }

    fun el(fn: suspend () -> Node): Node {
      return LazyNode(fn)
    }

    fun el(tag: String, vararg children: String): Node {
      return El(tag, emptyList(), children.map { Text(it, true) }.toList())
    }
  }
}
