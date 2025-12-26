@file:JsModule("@dnd-kit/core")
@file:JsNonModule

package io.amichne.konditional.ui.external

import react.FC
import react.Props
import react.ReactNode

/**
 * External declarations for @dnd-kit/core and @dnd-kit/sortable.
 *
 * These provide drag-and-drop functionality for array reordering.
 */

// DndContext types
external interface DndContextProps : Props {
    var onDragEnd: (DragEndEvent) -> Unit
    var onDragStart: ((DragStartEvent) -> Unit)?
    var onDragOver: ((DragOverEvent) -> Unit)?
    var onDragCancel: (() -> Unit)?
    var sensors: Array<dynamic>?
    var collisionDetection: dynamic
    var children: ReactNode?
}

external interface DragEndEvent {
    val active: DragItem
    val over: DragItem?
    val delta: DragDelta
}

external interface DragStartEvent {
    val active: DragItem
}

external interface DragOverEvent {
    val active: DragItem
    val over: DragItem?
}

external interface DragItem {
    val id: String
    val data: dynamic
}

external interface DragDelta {
    val x: Double
    val y: Double
}

external val DndContext: FC<DndContextProps>

// Sensor hooks (useSensor, useSensors)
external interface SensorOptions {
    var activationConstraint: ActivationConstraint?
}

external interface ActivationConstraint {
    var distance: Int?
    var delay: Int?
    var tolerance: Int?
}

external fun useSensor(sensor: dynamic, options: SensorOptions? = definedExternally): dynamic

external fun useSensors(vararg sensors: dynamic): Array<dynamic>

// Sensors
external val PointerSensor: dynamic
external val KeyboardSensor: dynamic
external val TouchSensor: dynamic
external val MouseSensor: dynamic

// Collision detection
external val closestCenter: dynamic
external val closestCorners: dynamic
external val rectIntersection: dynamic
external val pointerWithin: dynamic

// DragOverlay
external interface DragOverlayProps : Props {
    var children: ReactNode?
    var dropAnimation: dynamic
    var modifiers: Array<dynamic>?
    var adjustScale: Boolean?
    var zIndex: Int?
}

external val DragOverlay: FC<DragOverlayProps>
