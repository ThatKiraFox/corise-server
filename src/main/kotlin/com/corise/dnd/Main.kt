package com.corise.dnd

import com.corise.dnd.config.loadDotEnv
import io.ktor.server.netty.EngineMain

fun main(args: Array<String>) {
    loadDotEnv()
    EngineMain.main(args)
}
