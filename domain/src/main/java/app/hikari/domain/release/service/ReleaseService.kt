package app.hikari.domain.release.service

import app.hikari.domain.release.interactor.GetApplicationRelease
import app.hikari.domain.release.model.Release

interface ReleaseService {

    suspend fun latest(arguments: GetApplicationRelease.Arguments): Release?
}
