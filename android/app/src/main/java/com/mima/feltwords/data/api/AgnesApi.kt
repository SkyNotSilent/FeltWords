package com.mima.feltwords.data.api

import retrofit2.http.Body
import retrofit2.http.POST

/** Agnes REST 接口定义 */
interface AgnesApi {

    @POST("chat/completions")
    suspend fun chatCompletions(@Body body: ChatRequest): ChatResponse

    @POST("chat/completions")
    suspend fun textChatCompletions(@Body body: TextChatRequest): ChatResponse

    @POST("images/generations")
    suspend fun imageGenerations(@Body body: ImageGenerationRequest): ImageResponse
}
