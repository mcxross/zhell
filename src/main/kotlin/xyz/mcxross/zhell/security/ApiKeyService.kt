package xyz.mcxross.zhell.security

import com.google.cloud.firestore.FirestoreOptions
import java.security.SecureRandom
import java.util.Base64
import xyz.mcxross.zhell.plugins.hashString

class ApiKeyService {
  private val secureRandom = SecureRandom()

  private val firestore = FirestoreOptions.getDefaultInstance().service

  fun generateApiKey(name: String, owner: String): String {
    val randomBytes = ByteArray(32)
    secureRandom.nextBytes(randomBytes)

    val randomString = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes)

    val apiKey = "zk_$randomString"
    val hash = hashString(apiKey)
    val prefix = apiKey.take(8)

    val keyData =
      mapOf(
        "prefix" to prefix,
        "name" to name,
        "owner" to owner,
        "revoked" to false,
        "createdAt" to System.currentTimeMillis(),
      )

    firestore.collection("api_keys").document(hash).set(keyData).get()

    return apiKey
  }
}
