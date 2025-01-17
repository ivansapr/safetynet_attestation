import 'package:flutter_test/flutter_test.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';
import 'package:safetynet_attestation/models/jws_payload_model.dart';
import 'package:safetynet_attestation/safetynet_attestation.dart';
import 'package:safetynet_attestation/safetynet_attestation_method_channel.dart';
import 'package:safetynet_attestation/safetynet_attestation_platform_interface.dart';

class MockSafetynetAttestationPlatform
    with MockPlatformInterfaceMixin
    implements SafetynetAttestationPlatform {
  @override
  Future<String?> getPlatformVersion() => Future.value('42');

  @override
  Future<GooglePlayServicesAvailability?> googlePlayServicesAvailability() {
    // TODO: implement googlePlayServicesAvailability
    throw UnimplementedError();
  }

  @override
  Future<JWSPayloadModel> playIntegrityApiManualPayload({
    required int projectNumber,
    String keyType = "EC",
    String? nonce,
  }) {
    // TODO: implement playIntegrityApiManualPayload
    throw UnimplementedError();
  }

  @override
  Future<JWSPayloadModel> playIntegrityApiPayload({
    required int projectNumber,
    required String token,
    required applicationId,
    String? nonce,
  }) {
    // TODO: implement playIntegrityApiPayload
    throw UnimplementedError();
  }

  @override
  Future<String> playIntegrityApiToken({required int projectNumber, String? nonce}) {
    // TODO: implement playIntegrityApiToken
    throw UnimplementedError();
  }
}

void main() {
  final SafetynetAttestationPlatform initialPlatform =
      SafetynetAttestationPlatform.instance;

  test('$MethodChannelSafetynetAttestation is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelSafetynetAttestation>());
  });

  test('getPlatformVersion', () async {
    MockSafetynetAttestationPlatform fakePlatform =
        MockSafetynetAttestationPlatform();
    SafetynetAttestationPlatform.instance = fakePlatform;

    expect(await SafetynetAttestation.getPlatformVersion(), '42');
  });
}
