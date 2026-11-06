package app.hikari.i18n

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

@JvmInline
value class StringResource(@param:StringRes val resourceId: Int)

@JvmInline
value class PluralsResource(@param:PluralsRes val resourceId: Int)
