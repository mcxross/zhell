package xyz.mcxross.zhell.plugins

import com.google.cloud.firestore.FirestoreOptions
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import java.security.MessageDigest

data class AdminPrincipal(val username: String) : Principal

data class ApiKeyPrincipal(val owner: String, val name: String) : Principal

fun Application.configureSecurity() {
  val adminSecret =
    System.getenv("ADMIN_TOKEN")
      ?: throw IllegalStateException("ADMIN_TOKEN environment variable must be set.")

  val firestore = FirestoreOptions.getDefaultInstance().service

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

        // Firestore Lookup
        val docRef = firestore.collection("api_keys").document(hash)
        val snapshot = docRef.get().get()

        if (snapshot.exists() && snapshot.getBoolean("revoked") == false) {
          val owner = snapshot.getString("owner") ?: "Unknown"
          val name = snapshot.getString("name") ?: "Unknown"
          context.principal(ApiKeyPrincipal(owner, name))
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
