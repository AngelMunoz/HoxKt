package hoxkt.types

import kotlinx.coroutines.flow.Flow

data class Attribute(val name: String, val value: String?)

sealed class Node

data class El(val tag: String, val attr: List<Attribute>, val children: List<Node>): Node()
data class Text(val text: String, val encoded: Boolean = true): Node()
data class Comment(val comment: String): Node()
data class Fragment(val nodes: Iterable<Node>): Node()
data class FragmentFlow(val nodes: Flow<Node>): Node()

data class LazyNode(val invoke: suspend () -> Node): Node()
