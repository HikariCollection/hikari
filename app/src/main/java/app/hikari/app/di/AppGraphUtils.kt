package app.hikari.app.di

import android.content.Context
import app.hikari.core.metro.metroGraph

val Context.appGraph get() = metroGraph<AppGraph>()
