package info.androidabcd.plugins.custom.ll.domain.entity

data class DeviceDetails(
    val carrier: String,
    val deviceId: String,
    val userId: String,
    val phoneOS: String,
    val deviceType: String,
    val screenSpecs: ScreenSpecs
)

data class ScreenSpecs(
    val safeAreaPaddingTop: Int,
    val safeAreaPaddingBottom: Int,
    val width: Int,
    val height: Int,
    val diameter: Double
)