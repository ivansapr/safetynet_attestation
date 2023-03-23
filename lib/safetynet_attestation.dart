import 'package:safetynet_attestation/models/jws_payload_model.dart';

import 'safetynet_attestation_platform_interface.dart';

class SafetynetAttestation {
  static Future<String?> getPlatformVersion() {
    return SafetynetAttestationPlatform.instance.getPlatformVersion();
  }

  static Future<JWSPayloadModel> playIntegrityApiPayload({
    required int projectNumber,
    required String token,
    required String applicationId,
    String? nonce,
  }) {
    return SafetynetAttestationPlatform.instance.playIntegrityApiPayload(
      projectNumber: projectNumber,
      token: token,
      applicationId: applicationId,
      nonce: nonce,
    );
  }

  static Future<JWSPayloadModel> playIntegrityApiManualPayload({
    required int projectNumber,
    String keyType = "EC",
    String? nonce,
  }) {
    return SafetynetAttestationPlatform.instance.playIntegrityApiManualPayload(
      projectNumber: projectNumber,
      keyType: keyType,
      nonce: nonce,
    );
  }

  static Future<GooglePlayServicesAvailability?>
      googlePlayServicesAvailability() {
    return SafetynetAttestationPlatform.instance
        .googlePlayServicesAvailability();
  }
}
