package com.codesfirst.safetynet_attestation

import android.app.Activity
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import androidx.annotation.NonNull
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.tasks.Task
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.google.android.play.core.integrity.IntegrityTokenResponse
import com.google.android.gms.common.GoogleApiAvailability

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import okhttp3.*
import org.jose4j.jwe.JsonWebEncryption
import org.jose4j.jws.JsonWebSignature
import org.jose4j.jwx.JsonWebStructure
import java.io.IOException
import java.net.URL
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import kotlin.math.floor

/** SafetynetAttestationPlugin */
class SafetynetAttestationPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel

    private var activity: Activity? = null

    /** Private Service: https://www.googleapis.com/auth/playintegrity.  */
    private val urlPlayIntegrity: String = "https://playintegrity.googleapis.com"

    private val DECRYPTION_KEY = "decryption_api_key"
    private val VERIFICATION_KEY = "verification_key"

    // The Methods add is for get context activity
    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        this.activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {}

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onDetachedFromActivity() {}
    //End

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "safetynet_attestation")
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "getPlatformVersion" -> {
                result.success("Android ${android.os.Build.VERSION.RELEASE}")
            }

            "checkGooglePlayServicesAvailability" -> checkGooglePlayServicesAvailability(result)
            "requestPlayIntegrityApi" -> {
                requestPlayIntegrityApi(call, result)
            }

            "requestPlayIntegrityApiManual" -> {
                requestPlayIntegrityApiManual(call, result)
            }
            "requestPlayIntegrityApiToken" -> {
                requestPlayIntegrityApiToken(call, result)
            }

            else -> {
                result.notImplemented()
            }
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }


    private fun requestPlayIntegrityApi(call: MethodCall, result: Result) {
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(activity!!)
                != ConnectionResult.SUCCESS) {
            result.error("Error", "Google Play Services are not available, please call the checkGooglePlayServicesAvailability() method to understand why", null)
            return
        } else if (!call.hasArgument("cloud_project_number")) {
            result.error("Error", "Please include the cloud_project_number in the request", null)
            return
        }
        else if (!call.hasArgument("token")) {
            result.error("Error", "Please include the token in the request", null)
            return
        } else if (!call.hasArgument("application_id")) {
            result.error("Error", "Please include the application_id in the request", null)
            return
        }

        val nonce: String? = call.argument("nonce") as String?
        val cloudProjectNumber: Long = call.argument("cloud_project_number") as Long? ?: 0
        val tokenBearer: String = call.argument("token") as String? ?: ""
        val applicationId: String = call.argument("application_id") as String? ?: ""


        // Create an instance of a manager.
        val integrityManager =
                IntegrityManagerFactory.create(activity)

        val integrityResponseBuilder = IntegrityTokenRequest.builder()
                .setCloudProjectNumber(cloudProjectNumber)
        if (nonce != null) {
            if (!checkNonce(nonce)) {
                result.error("Error", "The nonce should be larger than the 16 bytes", null)
                return
            }
            integrityResponseBuilder.setNonce(nonce)
        }


        // Request the integrity token by providing a nonce.
        val integrityTokenResponse: Task<IntegrityTokenResponse> =
                integrityManager.requestIntegrityToken(integrityResponseBuilder.build())

        integrityTokenResponse.addOnSuccessListener { integrityTokenResponse: IntegrityTokenResponse ->
            val integrityToken = integrityTokenResponse.token()
            result.success(integrityToken)
        }

        integrityTokenResponse.addOnFailureListener { e ->
            e.printStackTrace()
            if (e is ApiException) {
                result.error("Error",
                        CommonStatusCodes.getStatusCodeString(e.statusCode) + " : " +
                                e.message, null)
            } else {
                result.error("Error", e.message, null)
            }
        }
    }

    private fun requestPlayIntegrityApiToken(call: MethodCall, result: Result) {
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(activity!!)
                != ConnectionResult.SUCCESS) {
            result.error("Error", "Google Play Services are not available, please call the checkGooglePlayServicesAvailability() method to understand why", null)
            return
        } else if (!call.hasArgument("cloud_project_number")) {
            result.error("Error", "Please include the cloud_project_number in the request", null)
            return
        }

        val nonce: String? = call.argument("nonce") as String?
        val cloudProjectNumber: Long = call.argument("cloud_project_number") as Long? ?: 0

        // Create an instance of a manager.
        val integrityManager =
                IntegrityManagerFactory.create(activity)

        val integrityResponseBuilder = IntegrityTokenRequest.builder()
                .setCloudProjectNumber(cloudProjectNumber)
        if (nonce != null) {
            if (!checkNonce(nonce)) {
                result.error("Error", "The nonce should be larger than the 16 bytes", null)
                return
            }
            integrityResponseBuilder.setNonce(nonce)
        }


        // Request the integrity token by providing a nonce.
        val integrityTokenResponse: Task<IntegrityTokenResponse> =
                integrityManager.requestIntegrityToken(integrityResponseBuilder.build())

        integrityTokenResponse.addOnSuccessListener { integrityTokenResponse: IntegrityTokenResponse ->
            val integrityToken = integrityTokenResponse.token()
            result.success(integrityToken)
        }

        integrityTokenResponse.addOnFailureListener { e ->
            e.printStackTrace()
            if (e is ApiException) {
                result.error("Error",
                        CommonStatusCodes.getStatusCodeString(e.statusCode) + " : " +
                                e.message, null)
            } else {
                result.error("Error", e.message, null)
            }
        }
    }

    private fun requestPlayIntegrityApiManual(call: MethodCall, result: Result) {
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(activity!!)
                != ConnectionResult.SUCCESS) {
            result.error("Error", "Google Play Services are not available, please call the checkGooglePlayServicesAvailability() method to understand why", null)
            return
        } else if (!checkDecryptionKeyInManifest()) {
            result.error("Error", "The Decryption Key is missing in the manifest", null)
            return
        } else if (!checkVerificationKeyInManifest()) {
            result.error("Error", "The Verification Key is missing in the manifest", null)
            return
        } else if (!call.hasArgument("cloud_project_number")) {
            result.error("Error", "Please include the cloud_project_number in the request", null)
            return
        }
        // Check nonce
        val nonce: String? = call.argument("nonce") as String?
        val cloudProjectNumber: Long = call.argument("cloud_project_number") as Long? ?: 0
        val ecKeyType: String = call.argument("ec_key_type") as String? ?: "EC"
        if (nonce == null || nonce.length < 16) {
            result.error("Error", "The nonce should be larger than the 16 bytes", null)
            return
        }

        // Create an instance of a manager.
        val integrityManager =
                IntegrityManagerFactory.create(activity)

        val integrityResponseBuilder = IntegrityTokenRequest.builder()
                .setCloudProjectNumber(cloudProjectNumber)
        if (nonce != null) {
            if (!checkNonce(nonce)) {
                result.error("Error", "The nonce should be larger than the 16 characters and less then 500 characters. ", null)
                return
            }
            integrityResponseBuilder.setNonce(nonce)
        }

        // Request the integrity token by providing a nonce.
        val integrityTokenResponse: Task<IntegrityTokenResponse> =
                integrityManager.requestIntegrityToken(
                        integrityResponseBuilder
                                .build())

        integrityTokenResponse.addOnSuccessListener { integrityTokenResponse1: IntegrityTokenResponse ->
            val integrityToken = integrityTokenResponse1.token()

            try {
                // base64OfEncodedDecryptionKey is provided through Play Console.
                var decryptionKeyBytes: ByteArray =
                        Base64.decode(getDecryptionKey(), Base64.DEFAULT)

                // Deserialized encryption (symmetric) key.
                var decryptionKey: SecretKey = SecretKeySpec(
                        decryptionKeyBytes,
                        0,
                        decryptionKeyBytes.size,
                        "AES"
                )

                // base64OfEncodedVerificationKey is provided through Play Console.
                var encodedVerificationKey: ByteArray =
                        Base64.decode(getVerificationKey(), Base64.DEFAULT)

                // Deserialized verification (public) key.
                var verificationKey: PublicKey = KeyFactory.getInstance(ecKeyType)
                        .generatePublic(X509EncodedKeySpec(encodedVerificationKey))

                val jwe: JsonWebEncryption =
                        JsonWebStructure.fromCompactSerialization(integrityToken) as JsonWebEncryption
                jwe.key = decryptionKey

                // This also decrypts the JWE token.
                val compactJws: String = jwe.payload
                //log.d("payload1", compactJws)
                val jws: JsonWebSignature =
                        JsonWebStructure.fromCompactSerialization(compactJws) as JsonWebSignature
                jws.key = verificationKey

                // This also verifies the signature.
                val payload: String = jws.payload
                //log.d("payload2", payload)
                result.success(payload)

            } catch (e: Exception) {
                result.error("Error", e.message, null)
            }

        }

        integrityTokenResponse.addOnFailureListener { e ->
            e.printStackTrace()
            if (e is ApiException) {
                result.error("Error",
                        CommonStatusCodes.getStatusCodeString(e.statusCode) + " : " +
                                e.message, null)
            } else {
                result.error("Error", e.message, null)
            }
        }
    }

    private fun checkGooglePlayServicesAvailability(result: Result) {
        when (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(activity!!)) {
            ConnectionResult.SUCCESS -> result.success("success")
            ConnectionResult.SERVICE_MISSING -> result.success("serviceMissing")
            ConnectionResult.SERVICE_UPDATING -> result.success("serviceUpdating")
            ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> result.success("serviceVersionUpdateRequired")
            ConnectionResult.SERVICE_DISABLED -> result.success("serviceDisabled")
            ConnectionResult.SERVICE_INVALID -> result.success("serviceInvalid")
            else -> result.error("Error", "Unknown error code", null)
        }
    }

    private fun checkNonce(nonce: String): Boolean {
        if (nonce.length < 16 || nonce.length > 500) {
            return false
        }
        return true
    }

    private fun requestPlayIntegrity(token: String, bearer: String, applicationId: String, result: Result) {
        try {
            var client: OkHttpClient = OkHttpClient();

            val formBody: RequestBody = FormBody.Builder()
                    .add("integrity_token", token)
                    .build()

            val url = URL("$urlPlayIntegrity/v1/$applicationId:decodeIntegrityToken")
            //log.d("URL", url.path)
            // Build request
            val request = Request.Builder().addHeader("Authorization", "bearer $token").post(formBody).url(url).build()
            //log.d("URL", "build")
            // Execute request
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    //log.d("URL", "error")
                    result.error("Api request error", "Error when executing get request: " + e.localizedMessage, null)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        //log.d("URL", "ok")
                        if (!response.isSuccessful) {
                            //log.d("URL", "ok perro error" + response.code)
                            result.error("Api request error", "Error code: " + response.code, null)
                            return
                        }
                        val responseBody: ResponseBody? = response.body
                        if (responseBody == null) {
                            result.error("Api request error", "Error code: " + response.code, null)
                            return
                        }
                        result.success(responseBody.string())

                        //log.d("URL",response.body!!.string())
                    }
                }
            })
        } catch (err: Error) {
            result.error("Api request error", "Error when executing get request: " + err.localizedMessage, null)
            return
        }

    }

    private fun checkDecryptionKeyInManifest(): Boolean {
        return !TextUtils.isEmpty(getDecryptionKey())
    }

    private fun checkVerificationKeyInManifest(): Boolean {
        return !TextUtils.isEmpty(getVerificationKey())
    }

    private fun getDecryptionKey(): String? {
        return Utils.getMetadataFromManifest(activity!!, DECRYPTION_KEY)
    }

    private fun getVerificationKey(): String? {
        return Utils.getMetadataFromManifest(activity!!, VERIFICATION_KEY)
    }

}
