package com.v2ray.ang.fakesni

/** Configuration for the embedded FakeSNI local TLS proxy. */
data class FakeSniConfig(
    val enabled: Boolean = false,
    val listenHost: String = "127.0.0.1",
    val listenPort: Int = 40443,
    val connectHost: String = "",
    val connectPort: Int = 443,
    val fakeSni: String = "hcaptcha.com",
    val utls: String = "firefox",
    val injector: String = "passive",
    val fakeRepeat: Int = 1,
    val fakeDelay: String = "2ms",
    val ackTimeout: String = "2s",
    val enableFragment: Boolean = false,
    val fragmentDelay: String = "500ms",
    val sniChunk: Int = 3,
    val networkInterface: String = "rmnet_data0",
    val addIpRule: Boolean = false,
) {
    fun binaryArgs(binaryPath: String): String = buildString {
        append("'$binaryPath'")
        append(" -listen '$listenHost:$listenPort'")
        append(" -connect '$connectHost:$connectPort'")
        append(" -fake-sni '$fakeSni'")
        append(" -utls '$utls'")
        append(" -injector $injector")
        append(" -fake-repeat $fakeRepeat")
        append(" -fake-delay $fakeDelay")
        append(" -ack-timeout $ackTimeout")
        append(" -enable-fragment=$enableFragment")
        if (enableFragment) {
            append(" -fragment-delay $fragmentDelay")
            append(" -sni-chunk $sniChunk")
        }
    }
}
