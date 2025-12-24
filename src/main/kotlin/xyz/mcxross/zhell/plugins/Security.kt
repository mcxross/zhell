package xyz.mcxross.zhell.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import java.security.MessageDigest
import xyz.mcxross.zhell.security.ApiKeyRepository

data class AdminPrincipal(val username: String) : Principal

data class ApiKeyPrincipal(val owner: String, val name: String) : Principal

fun Application.configureSecurity(apiKeyRepository: ApiKeyRepository) {
  val adminSecret =
    System.getenv("ADMIN_TOKEN")
      ?: throw IllegalStateException("ADMIN_TOKEN environment variable must be set.")

  authentication {
    bearer("admin-auth") {
      realm = "Admin Access"
      authenticate { tokenCredential ->
        if (secureCompare(tokenCredential.token, adminSecret)) {
          AdminPrincipal("admin")
        } else {
          null
        }
      }
    }

    provider("api-key-auth") {
      authenticate { context ->
        val apiKey = context.call.request.headers["X-API-Key"]

        if (apiKey == null) {
          context.challenge("ApiKeyAuth", AuthenticationFailedCause.NoCredentials) { challenge, call
            ->
            call.respond(HttpStatusCode.Unauthorized, "Missing API Key")
            challenge.complete()
          }
          return@authenticate
        }

        val hash = hashString(apiKey)

        val keyData = apiKeyRepository.find(hash)

        if (keyData != null && !keyData.revoked) {
          context.principal(ApiKeyPrincipal(keyData.owner, keyData.name))
        } else {
          context.challenge("ApiKeyAuth", AuthenticationFailedCause.InvalidCredentials) {
            challenge,
            call ->
            call.respond(HttpStatusCode.Unauthorized, "Invalid or Revoked API Key")
            challenge.complete()
          }
        }
      }
    }
  }
}

fun hashString(input: String): String {
  val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
  return bytes.joinToString("") { "%02x".format(it) }
}

fun secureCompare(s1: String, s2: String): Boolean {
  return MessageDigest.isEqual(s1.toByteArray(), s2.toByteArray())
}
