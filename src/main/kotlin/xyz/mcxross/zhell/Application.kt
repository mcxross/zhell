package xyz.mcxross.zhell

import io.ktor.server.application.*
import io.ktor.server.netty.EngineMain
import xyz.mcxross.zhell.plugins.configureSecurity
import xyz.mcxross.zhell.plugins.configureSerialization

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
  configureSecurity()
  configureSerialization()
  configureRouting()
}
