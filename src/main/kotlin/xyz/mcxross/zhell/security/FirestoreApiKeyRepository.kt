package xyz.mcxross.zhell.security

import com.google.cloud.firestore.FirestoreOptions

class FirestoreApiKeyRepository : ApiKeyRepository {
  private val firestore = FirestoreOptions.getDefaultInstance().service

  override fun save(hash: String, data: ApiKeyData) {
    val map =
      mapOf(
        "prefix" to data.prefix,
        "name" to data.name,
        "owner" to data.owner,
        "revoked" to data.revoked,
        "createdAt" to data.createdAt,
      )
    firestore.collection("api_keys").document(hash).set(map).get()
  }

  override fun find(hash: String): ApiKeyData? {
    val docRef = firestore.collection("api_keys").document(hash)
    val snapshot = docRef.get().get()

    if (snapshot.exists()) {
      return ApiKeyData(
        prefix = snapshot.getString("prefix") ?: "",
        name = snapshot.getString("name") ?: "Unknown",
        owner = snapshot.getString("owner") ?: "Unknown",
        revoked = snapshot.getBoolean("revoked") ?: false,
        createdAt = snapshot.getLong("createdAt") ?: 0L,
      )
    }
    return null
  }
}
