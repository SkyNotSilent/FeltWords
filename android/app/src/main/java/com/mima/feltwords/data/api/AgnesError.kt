package com.mima.feltwords.data.api

/** Agnes 接口错误 —— 中文文案逐字对齐 iOS AgnesError */
sealed class AgnesError(message: String) : Exception(message) {

    /** API Key 未配置 */
    data object MissingApiKey : AgnesError(
        "请先在 Config/Secrets.xcconfig 中配置 Agnes API Key。"
    )

    /** 图片无法处理 */
    data object InvalidImage : AgnesError(
        "这张照片暂时读不出来，请再拍一次。"
    )

    /** 响应无法解析 */
    data object InvalidResponse : AgnesError(
        "毛毛没有看清楚，请再试一次。"
    )

    /** 服务端返回的错误信息 */
    data class Server(val msg: String) : AgnesError(msg)
}
