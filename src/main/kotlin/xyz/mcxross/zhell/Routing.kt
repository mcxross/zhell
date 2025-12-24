package xyz.mcxross.zhell

import io.ktor.server.application.*
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.response.*
import io.ktor.server.routing.*
import xyz.mcxross.zhell.routes.adminRoutes
import xyz.mcxross.zhell.routes.gasRoutes
import xyz.mcxross.zhell.security.ApiKeyService

fun Application.configureRouting() {
  val apiKeyService = ApiKeyService()
  routing {
    authenticate("api-key-auth") {
      get("/protected") {
        val principal = call.principal<UserIdPrincipal>()
        call.respondText("Hello, ${principal?.name}! You are using a valid API key.")
      }
    }

    gasRoutes()
    adminRoutes(apiKeyService)
  }
}
