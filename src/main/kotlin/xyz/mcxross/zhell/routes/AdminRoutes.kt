package xyz.mcxross.zhell.routes

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import xyz.mcxross.zhell.plugins.AdminPrincipal
import xyz.mcxross.zhell.security.ApiKeyService

fun Route.adminRoutes(apiKeyService: ApiKeyService) {
  authenticate("admin-auth") {
    post("/admin/api-keys") {
      call.principal<AdminPrincipal>() ?: return@post call.respond(HttpStatusCode.Unauthorized)

      val params =
        try {
          call.receive<ApiKeyRequest>()
        } catch (e: Exception) {
          call.respond(HttpStatusCode.BadRequest, "Invalid request body format")
          return@post
        }

      if (params.name.isBlank() || params.owner.isBlank()) {
        call.respond(HttpStatusCode.BadRequest, "Name and Owner fields cannot be empty")
        return@post
      }

      try {
        val apiKey = apiKeyService.generateApiKey(params.name, params.owner)
        call.respond(HttpStatusCode.Created, ApiKeyResponse(apiKey))
      } catch (e: Exception) {
        call.respond(HttpStatusCode.InternalServerError, "Failed to generate API Key")
      }
    }
  }
}

@Serializable data class ApiKeyRequest(val name: String, val owner: String)

@Serializable data class ApiKeyResponse(val apiKey: String)
