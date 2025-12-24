package xyz.mcxross.zhell.security

import java.util.concurrent.ConcurrentHashMap

class InMemoryApiKeyRepository : ApiKeyRepository {
  private val storage = ConcurrentHashMap<String, ApiKeyData>()

  override fun save(hash: String, data: ApiKeyData) {
    storage[hash] = data
  }

  override fun find(hash: String): ApiKeyData? {
    return storage[hash]
  }
}
