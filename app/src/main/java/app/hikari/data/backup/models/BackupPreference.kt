package app.hikari.data.backup.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class BackupPreference(
    @ProtoNumber(1) val key: String = "",
    @ProtoNumber(2) val value: PreferenceValue,
)

@Serializable
data class BackupSourcePreferences(
    @ProtoNumber(1) val sourceKey: String = "",
    @ProtoNumber(2) val prefs: List<BackupPreference> = emptyList(),
)

@Serializable
@SerialName("eu.kanade.tachiyomi.data.backup.models.PreferenceValue")
sealed class PreferenceValue

@Serializable
@SerialName("eu.kanade.tachiyomi.data.backup.models.IntPreferenceValue")
data class IntPreferenceValue(val value: Int = 0) : PreferenceValue()

@Serializable
@SerialName("eu.kanade.tachiyomi.data.backup.models.LongPreferenceValue")
data class LongPreferenceValue(val value: Long = 0L) : PreferenceValue()

@Serializable
@SerialName("eu.kanade.tachiyomi.data.backup.models.FloatPreferenceValue")
data class FloatPreferenceValue(val value: Float = 0f) : PreferenceValue()

@Serializable
@SerialName("eu.kanade.tachiyomi.data.backup.models.StringPreferenceValue")
data class StringPreferenceValue(val value: String = "") : PreferenceValue()

@Serializable
@SerialName("eu.kanade.tachiyomi.data.backup.models.BooleanPreferenceValue")
data class BooleanPreferenceValue(val value: Boolean = false) : PreferenceValue()

@Serializable
@SerialName("eu.kanade.tachiyomi.data.backup.models.StringSetPreferenceValue")
data class StringSetPreferenceValue(val value: Set<String> = emptySet()) : PreferenceValue()

@OptIn(ExperimentalSerializationApi::class)
val backupProtoBuf: ProtoBuf = ProtoBuf {
    serializersModule = SerializersModule {
        polymorphicDefaultDeserializer(PreferenceValue::class) { className ->
            when {
                className?.endsWith("IntPreferenceValue") == true -> IntPreferenceValue.serializer()
                className?.endsWith("LongPreferenceValue") == true -> LongPreferenceValue.serializer()
                className?.endsWith("FloatPreferenceValue") == true -> FloatPreferenceValue.serializer()
                className?.endsWith("StringPreferenceValue") == true -> StringPreferenceValue.serializer()
                className?.endsWith("BooleanPreferenceValue") == true -> BooleanPreferenceValue.serializer()
                className?.endsWith("StringSetPreferenceValue") == true -> StringSetPreferenceValue.serializer()
                else -> null
            }
        }
    }
}
