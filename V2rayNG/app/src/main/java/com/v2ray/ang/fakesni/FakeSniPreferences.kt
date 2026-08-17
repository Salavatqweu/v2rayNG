package com.v2ray.ang.fakesni

import android.content.Context
import com.v2ray.ang.handler.MmkvManager

/** Stores FakeSNI settings in the same MMKV store used by v2rayNG. */
object FakeSniPreferences {
    private const val ENABLED = "fakesni.enabled"
    private const val FAKE_SNI = "fakesni.fake_sni"
    private const val UTLS = "fakesni.utls"
    private const val INJECTOR = "fakesni.injector"
    private const val LISTEN_PORT = "fakesni.listen_port"
    private const val FAKE_REPEAT = "fakesni.fake_repeat"
    private const val FAKE_DELAY = "fakesni.fake_delay"
    private const val ACK_TIMEOUT = "fakesni.ack_timeout"
    private const val FRAGMENT = "fakesni.fragment"
    private const val FRAGMENT_DELAY = "fakesni.fragment_delay"
    private const val SNI_CHUNK = "fakesni.sni_chunk"
    private const val NETWORK_INTERFACE = "fakesni.network_interface"
    private const val ADD_IP_RULE = "fakesni.add_ip_rule"

    fun load(context: Context): FakeSniConfig = FakeSniConfig(
        enabled = MmkvManager.decodeSettingsBool(ENABLED, false),
        listenPort = MmkvManager.decodeSettingsString(LISTEN_PORT, "40443").toIntOrNull() ?: 40443,
        fakeSni = MmkvManager.decodeSettingsString(FAKE_SNI, "hcaptcha.com").orEmpty(),
        utls = MmkvManager.decodeSettingsString(UTLS, "firefox").orEmpty(),
        injector = MmkvManager.decodeSettingsString(INJECTOR, "passive").orEmpty(),
        fakeRepeat = MmkvManager.decodeSettingsString(FAKE_REPEAT, "1").toIntOrNull()?.coerceIn(1, 20) ?: 1,
        fakeDelay = MmkvManager.decodeSettingsString(FAKE_DELAY, "2ms").orEmpty(),
        ackTimeout = MmkvManager.decodeSettingsString(ACK_TIMEOUT, "2s").orEmpty(),
        enableFragment = MmkvManager.decodeSettingsBool(FRAGMENT, false),
        fragmentDelay = MmkvManager.decodeSettingsString(FRAGMENT_DELAY, "500ms").orEmpty(),
        sniChunk = MmkvManager.decodeSettingsString(SNI_CHUNK, "3").toIntOrNull()?.coerceIn(1, 64) ?: 3,
        networkInterface = MmkvManager.decodeSettingsString(NETWORK_INTERFACE, "rmnet_data0").orEmpty(),
        addIpRule = MmkvManager.decodeSettingsBool(ADD_IP_RULE, false),
    )

    fun setEnabled(enabled: Boolean) = MmkvManager.encodeSettings(ENABLED, enabled)
    fun isEnabled() = MmkvManager.decodeSettingsBool(ENABLED, false)

    fun save(config: FakeSniConfig) {
        MmkvManager.encodeSettings(ENABLED, config.enabled)
        MmkvManager.encodeSettings(FAKE_SNI, config.fakeSni)
        MmkvManager.encodeSettings(UTLS, config.utls)
        MmkvManager.encodeSettings(INJECTOR, config.injector)
        MmkvManager.encodeSettings(LISTEN_PORT, config.listenPort.toString())
        MmkvManager.encodeSettings(FAKE_REPEAT, config.fakeRepeat.toString())
        MmkvManager.encodeSettings(FAKE_DELAY, config.fakeDelay)
        MmkvManager.encodeSettings(ACK_TIMEOUT, config.ackTimeout)
        MmkvManager.encodeSettings(FRAGMENT, config.enableFragment)
        MmkvManager.encodeSettings(FRAGMENT_DELAY, config.fragmentDelay)
        MmkvManager.encodeSettings(SNI_CHUNK, config.sniChunk.toString())
        MmkvManager.encodeSettings(NETWORK_INTERFACE, config.networkInterface)
        MmkvManager.encodeSettings(ADD_IP_RULE, config.addIpRule)
    }
}
