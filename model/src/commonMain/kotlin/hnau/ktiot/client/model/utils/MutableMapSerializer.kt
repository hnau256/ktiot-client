package org.hnau.ktiot.client.model.utils

import arrow.core.identity
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import org.hnau.commons.kotlin.mapper.Mapper
import org.hnau.commons.kotlin.serialization.MappingKSerializer

class MutableMapSerializer<K, V>(
    keySerializer: KSerializer<K>,
    valueSerializer: KSerializer<V>,
) : MappingKSerializer<Map<K, V>, MutableMap<K, V>>(
    base = MapSerializer(keySerializer, valueSerializer),
    mapper = Mapper(
        direct = Map<K, V>::toMutableMap,
        reverse = ::identity
    )
)