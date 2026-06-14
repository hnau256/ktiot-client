package org.hnau.ktiot.client.model.property.value

import org.hnau.ktiot.scheme.PropertyType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.hnau.commons.app.model.goback.GoBackHandler
import org.hnau.commons.app.model.goback.NeverGoBackHandler
import org.hnau.commons.gen.pipe.annotations.Pipe

class FlagModel(
    private val scope: CoroutineScope,
    dependencies: Dependencies,
    skeleton: Skeleton,
    val value: StateFlow<Boolean>,
    val publish: StateFlow<((Boolean) -> Unit)?>,
    type: PropertyType.State.Flag,
    val mutable: Boolean,
) : ValueModel {

    companion object {

        val factory: ValueModel.Factory<Boolean, PropertyType.State.Flag, Dependencies, Skeleton, FlagModel> =
            ValueModel.Factory(::FlagModel)
    }

    @Pipe
    interface Dependencies

    @Serializable
    @SerialName("flag")
    /*data*/ class Skeleton : ValueModel.Skeleton

    override val goBackHandler: GoBackHandler
        get() = NeverGoBackHandler
}