@file:Suppress("NAME_SHADOWING")

package hoxkt.rendering

import hoxkt.types.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import org.owasp.encoder.Encode
import java.util.*
import kotlin.coroutines.cancellation.CancellationException

private interface ExtractedAttributes {
  val id: String?
  val classes: List<String>
  val attributes: List<Attribute>
}

private fun extractAttribues(attributes: List<Attribute>): ExtractedAttributes {
  var id: String? = null

  val initialPair = Pair<List<String>, List<Attribute>>(emptyList(), emptyList())

  val (classes, attributes) = attributes.fold(initialPair) { (classes, attributes), attribute ->
    when {
      // We only want to extract the first id even if the user
      // has specified multiple ids.
      id == null && attribute.name == "id" -> {
        id = attribute.value
        Pair(classes, attributes)
      }
      // We want to collect all classes.
      attribute.name == "class" && attribute.value != null -> Pair(classes.plus(attribute.value), attributes)

      // anything else should go untouched from the user
      else -> {
        Pair(classes, attributes.plus(attribute))
      }
    }
  }
  return object : ExtractedAttributes {
    override val id = id
    override val classes = classes
    override val attributes = attributes
  }
}

suspend fun renderNode(node: Node) = coroutineScope {
  val sb = StringBuilder()
  val stack = Stack<Pair<Node, Boolean>>()

  stack.push(Pair(node, false))

  while (!stack.empty()) {
    // support cancellation
    if (!isActive) throw CancellationException()

    val (node, isClosing) = stack.pop()

    when {
      node is El && isClosing -> sb.append("</${node.tag}>")
      node is El -> {
        sb.append("<${node.tag}")
        val extracted = extractAttribues(node.attr)

        // Anything except by "Raw" text should be encoded to help users
        // against XSS.

        if (extracted.id != null) sb.append(""" id="${Encode.forHtmlAttribute(extracted.id)}"""")

        if (extracted.classes.isNotEmpty()) {
          val classes = Encode.forHtmlAttribute(extracted.classes.joinToString(" "))
          sb.append(""" class="$classes"""")
        }

        for (attribute in extracted.attributes) {
          sb.append(""" ${attribute.name}="${Encode.forHtmlAttribute(attribute.value)}" """)
        }

        sb.append(">")
        stack.push(Pair(node, true))

        // We want to push the children in reverse order so that
        // they are rendered in the correct order given that we're using a stack.
        node.children.reversed().map { Pair(it, false) }.forEach { stack.push(it) }
      }

      node is Text -> sb.append(if (node.encoded) Encode.forHtml(node.text) else node.text)
      node is Comment -> sb.append("<!--").append(Encode.forHtml(node.comment)).append("-->")
      node is Fragment -> node.nodes.reversed().map { Pair(it, false) }.forEach { stack.push(it) }

      node is FragmentFlow -> {
        val buffer = mutableListOf<Pair<Node, Boolean>>()
        node.nodes.cancellable().map { Pair(it, false) }.collect { buffer.add(it) }
        buffer.reversed().forEach { stack.push(it) }
      }

      // lazy nodes are evaluated at this point
      // at any previous moment this work was left pending
      // it is good for async work, the cool thing is
      // that if we cancel the coroutine, the work is not done
      node is LazyNode -> {
        val result = node.invoke()
        stack.push(Pair(result, false))
      }
      else -> {}
    }
  }
  sb.toString()
}

/**
 * Sadly, this is not tail recursive, since flows are not tail recursive.
 * but they are cold streams so we should be fine for most purposes.
 * This also works in our favour as we can evaluate suspended nodes
 * lazily and emit them as soon as they are ready.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun renderNodeFlow(node: Node): Flow<String> = flow {
  when (node) {
    is Text -> emit(if (node.encoded) Encode.forHtml(node.text) else "")
    is Comment -> {
      emit("<!--")
      emit(Encode.forHtml(node.comment))
      emit("-->")
    }

    // Since we're using recursion, we don't need to reverse the order, as items
    // will be resolved in the correct order.
    // Howerver, I'm already aware that deep trees will cause a stack overflow.
    // I took a different approach in my F# lib, but since I'm not going serious in kotlin
    // I'll leave it as it is.
    is Fragment -> node.nodes.asFlow().flatMapConcat { renderNodeFlow(it) }.collect { emit(it) }
    is FragmentFlow -> node.nodes.flatMapConcat { renderNodeFlow(it) }.collect { emit(it) }
    is LazyNode -> renderNodeFlow(node.invoke()).collect { emit(it) }

    is El -> {
      val extracted = extractAttribues(node.attr)
      val id = extracted.id
      val classes = extracted.classes
      val attributes = extracted.attributes
      emit("<${node.tag}")
      if (id != null) emit(""" id="${Encode.forHtmlAttribute(id)}"""")

      if (classes.isNotEmpty()) {
        val classes = classes.joinToString(" ")
        emit(""" class="$classes"""")
      }

      for (attribute in attributes) {
        emit(""" ${attribute.name}="${Encode.forHtmlAttribute(attribute.value)}" """)
      }
      emit(">")
      node.children.asFlow().flatMapConcat { renderNodeFlow(it) }.collect { emit(it) }
      emit("</${node.tag}>")
    }
  }
}
