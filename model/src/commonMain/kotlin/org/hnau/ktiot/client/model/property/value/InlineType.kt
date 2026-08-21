package org.hnau.ktiot.client.model.property.value

import org.hnau.commons.app.model.input.InputType
import org.hnau.ktiot.scheme.PropertyType

sealed interface InlineType<T, P: PropertyType.State<T>, I: InputType<T>> {

    val property: P

    val input: I

    data object Flag: InlineType<Boolean, PropertyType.State.Flag, InputType.Flag> {

        override val property: PropertyType.State.Flag
            get() = PropertyType.State.Flag
        override val input: InputType.Flag
            get() = InputType.Flag
    }
}