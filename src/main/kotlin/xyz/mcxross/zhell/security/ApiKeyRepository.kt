package xyz.mcxross.zhell.security

data class ApiKeyData(
  val prefix: String,
  val name: String,
  val owner: String,
  val revoked: Boolean,
  val createdAt: Long,
)

interface ApiKeyRepository {
  fun save(hash: String, data: ApiKeyData)

  fun find(hash: String): ApiKeyData?
}
