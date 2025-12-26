package xyz.mcxross.zhell.routes

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.Serializable
import xyz.mcxross.ksui.Sui
import xyz.mcxross.ksui.account.Account
import xyz.mcxross.ksui.model.AccountAddress
import xyz.mcxross.ksui.model.Network
import xyz.mcxross.ksui.model.ObjectReference
import xyz.mcxross.ksui.model.SuiConfig
import xyz.mcxross.ksui.model.SuiSettings
import xyz.mcxross.ksui.model.TransactionDataComposer
import xyz.mcxross.ksui.model.GasLessTransactionData
import xyz.mcxross.ksui.model.ObjectDigest
import xyz.mcxross.ksui.model.Digest
import xyz.mcxross.ksui.model.Reference
import xyz.mcxross.ksui.model.Result
import xyz.mcxross.ksui.model.sign
import xyz.mcxross.ksui.ptb.TransactionKind
import xyz.mcxross.ksui.util.bcsDecode
import xyz.mcxross.ksui.util.bcsEncode
import xyz.mcxross.zhell.plugins.ApiKeyPrincipal

@OptIn(ExperimentalEncodingApi::class)
fun Route.gasRoutes() {

    val privateKey =
        System.getenv("SPONSOR_PRIVATE_KEY")
            ?: throw IllegalStateException("SPONSOR_PRIVATE_KEY not set")

    val sponsorAccount = Account.import(privateKey)
    val suiMainnet = Sui(config = SuiConfig(SuiSettings(Network.MAINNET)))
    val suiTestnet = Sui(config = SuiConfig(SuiSettings(Network.TESTNET)))
    val suiDevnet = Sui(config = SuiConfig(SuiSettings(Network.DEVNET)))
    val suiLocalnet = Sui(config = SuiConfig(SuiSettings(Network.LOCAL)))
    val customFullNode =
        System.getenv("SUI_CUSTOM_FULLNODE")?.trim()?.takeIf { it.isNotEmpty() }
    val suiCustom =
        customFullNode?.let { Sui(config = SuiConfig(SuiSettings(network = Network.CUSTOM, fullNode = it))) }

    suspend fun RoutingContext.handleGasRequest(sui: Sui) {
        val principal = call.principal<ApiKeyPrincipal>()

        val request =
            try {
                call.receive<GasRequest>()
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Invalid request body")
                return
            }

        if (request.txBytes.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, "Transaction bytes cannot be empty")
            return
        }

        val gasLess = try {
            bcsDecode<GasLessTransactionData>(Base64.decode(request.txBytes))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, "Invalid gasless transaction data")
            return
        }

        val gasPrice =
            when (val gp = sui.getReferenceGasPrice()) {
                is Result.Ok -> gp.value?.epoch?.referenceGasPrice?.toString()?.toULong() ?: 1000UL
                is Result.Err -> 1000UL
            }

        val coins =
            when (val c = sui.getCoins(sponsorAccount.address)) {
                is Result.Ok -> {
                    c.value?.address?.objects?.nodes?.map {
                        ObjectReference(
                            Reference(AccountAddress.fromString(it.address.toString())),
                            it.version.toString().toLong(),
                            ObjectDigest(Digest(it.digest.toString())),
                        )
                    } ?: emptyList()
                }

                is Result.Err -> emptyList()
            }

        if (coins.isEmpty()) {
            call.respond(HttpStatusCode.InternalServerError, "Sponsor has no gas coins")
            return
        }

        val pt =
            when (val kind = gasLess.kind) {
                is TransactionKind.ProgrammableTransaction -> kind.pt
                else -> {
                    call.respond(HttpStatusCode.BadRequest, "Unsupported transaction kind")
                    return
                }
            }

        val txData =
            TransactionDataComposer.programmableAllowSponsor(
                sender = gasLess.sender,
                gapPayment = listOf(coins.first()),
                pt = pt,
                gasBudget = 50_000_000UL,
                gasPrice = gasPrice,
                sponsor = sponsorAccount.address,
            )

        val sponsorSignature =
            when (val sig = txData.sign(sponsorAccount)) {
                is Result.Ok -> sig.value
                is Result.Err -> {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to sign transaction")
                    return
                }
            }

        val txBytes = Base64.encode(bcsEncode(txData))

        println("Sponsoring gas for user: ${principal?.owner}")

        call.respond(
            GasResponse(txBytes = txBytes, sponsorSignature = sponsorSignature, status = "SPONSORED")
        )
    }

    authenticate("api-key-auth") {
        post("/gas") {
            handleGasRequest(suiMainnet)
        }

        post("/testnet/gas") {
            handleGasRequest(suiTestnet)
        }

        post("/devnet/gas") {
            handleGasRequest(suiDevnet)
        }

        post("/localnet/gas") {
            handleGasRequest(suiLocalnet)
        }

        post("/custom/gas") {
            val custom = suiCustom
            if (custom == null) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "SUI_CUSTOM_FULLNODE environment variable must be set for custom gas sponsorship",
                )
                return@post
            }
            handleGasRequest(custom)
        }
    }
}

@Serializable
data class GasRequest(val txBytes: String, val sender: String)

@Serializable
data class GasResponse(val txBytes: String, val sponsorSignature: String, val status: String)
