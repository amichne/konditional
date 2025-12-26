@file:JsModule("@dnd-kit/sortable")
@file:JsNonModule

package io.amichne.konditional.ui.external

import react.FC
import react.Props
import react.ReactNode

/**
 * External declarations for @dnd-kit/sortable.
 */

// SortableContext
external interface SortableContextProps : Props {
    var items: Array<String>
    var strategy: dynamic
    var children: ReactNode?
}

external val SortableContext: FC<SortableContextProps>

// Sorting strategies
external val verticalListSortingStrategy: dynamic
external val horizontalListSortingStrategy: dynamic
external val rectSortingStrategy: dynamic
external val rectSwappingStrategy: dynamic

// useSortable hook
external interface UseSortableArguments {
    var id: String
    var data: dynamic
    var disabled: Boolean?
    var resizeObserverConfig: dynamic
}

external interface UseSortableResult {
    val attributes: dynamic
    val listeners: dynamic
    val setNodeRef: (element: dynamic) -> Unit
    val transform: Transform?
    val transition: String?
    val isDragging: Boolean
    val isSorting: Boolean
    val isOver: Boolean
    val active: dynamic
    val over: dynamic
}

external interface Transform {
    val x: Double
    val y: Double
    val scaleX: Double
    val scaleY: Double
}

external fun useSortable(args: UseSortableArguments): UseSortableResult

// arrayMove utility
external fun arrayMove(array: Array<dynamic>, from: Int, to: Int): Array<dynamic>

// sortableKeyboardCoordinates for keyboard accessibility
external val sortableKeyboardCoordinates: dynamic

// CSS utilities
external object CSS {
    val Transform: TransformUtil
    val Transition: TransitionUtil
}

external interface TransformUtil {
    fun toString(transform: Transform?): String?
}

external interface TransitionUtil {
    fun toString(args: dynamic): String
}
