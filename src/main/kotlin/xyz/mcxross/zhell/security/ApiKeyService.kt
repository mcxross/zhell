package xyz.mcxross.zhell.security

import java.security.SecureRandom
import java.util.Base64
import xyz.mcxross.zhell.plugins.hashString

class ApiKeyService(private val repository: ApiKeyRepository) {
  private val secureRandom = SecureRandom()

  fun generateApiKey(name: String, owner: String): String {
    val randomBytes = ByteArray(32)
    secureRandom.nextBytes(randomBytes)

    val randomString = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes)

    val apiKey = "zk_$randomString"
    val hash = hashString(apiKey)
    val prefix = apiKey.take(8)

    val keyData =
      ApiKeyData(
        prefix = prefix,
        name = name,
        owner = owner,
        revoked = false,
        createdAt = System.currentTimeMillis(),
      )

    repository.save(hash, keyData)

    return apiKey
  }
}
