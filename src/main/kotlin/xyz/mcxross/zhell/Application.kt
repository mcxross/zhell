package xyz.mcxross.zhell

import io.ktor.server.application.*
import io.ktor.server.netty.EngineMain
import xyz.mcxross.zhell.plugins.configureSecurity
import xyz.mcxross.zhell.plugins.configureSerialization
import xyz.mcxross.zhell.security.FirestoreApiKeyRepository
import xyz.mcxross.zhell.security.InMemoryApiKeyRepository

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
  val isDebug = System.getenv("DEBUG") == "1"
  val apiKeyRepository =
    if (isDebug) {
      InMemoryApiKeyRepository()
    } else {
      FirestoreApiKeyRepository()
    }

  configureSecurity(apiKeyRepository)
  configureSerialization()
  configureRouting(apiKeyRepository)
}
