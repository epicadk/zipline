/*
 * Copyright (C) 2021 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.cash.zipline

import app.cash.zipline.internal.bridge.OutboundCallHandler
import app.cash.zipline.internal.bridge.ReturningZiplineFunction
import app.cash.zipline.internal.bridge.SuspendCallback
import app.cash.zipline.internal.bridge.SuspendingZiplineFunction
import app.cash.zipline.internal.bridge.ZiplineServiceAdapter
import app.cash.zipline.internal.bridge.contextualSerializerForType
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer

@Serializable
data class SampleRequest(
  val message: String
)

@Serializable
data class SampleResponse(
  val message: String
)

/**
 * Note that this interface is unused. It only serves as a sample for what the code generators
 * should consume and produce.
 */
interface SampleService<T> : ZiplineService {
  fun ping(request: SampleRequest): SampleResponse

  suspend fun reduce(request: List<T>): T

  fun print(request: @Contextual SampleRequest): @Contextual SampleResponse

  companion object {
    /** This function's body is what callers use to create a properly-typed adapter. */
    internal inline fun <reified T> manualAdapter(): ManualAdapter<T> {
      return ManualAdapter<T>(
        listOf<KType>(
          typeOf<SampleRequest>(),
          typeOf<SampleResponse>(),
          typeOf<List<T>>(),
          typeOf<Unit>(),
        ),
        listOf<KSerializer<*>>(
          ziplineServiceSerializer<SuspendCallback<T>>()
        )
      )
    }

    /**
     * We expect this manually-written adapter to be consistent with the generated one. This exists
     * mostly to model what the expected generated code should look like when making changes to
     * `AdapterGenerator`.
     */
    internal class ManualAdapter<TX>(
      private val types: List<KType>,
      private val serializers: List<KSerializer<*>>,
    ) : ZiplineServiceAdapter<SampleService<TX>>() {
      override val serialName: String = "SampleService"

      class ZiplineFunction0<TF>(
        argSerializers: List<KSerializer<*>>,
        resultSerializer: KSerializer<*>,
      ) : ReturningZiplineFunction<SampleService<TF>>(
        "fun ping(app.cash.zipline.SampleRequest): app.cash.zipline.SampleResponse",
        argSerializers,
        resultSerializer,
      ) {
        override fun call(service: SampleService<TF>, args: List<*>): Any? =
          service.ping(args[0] as SampleRequest)
      }

      class ZiplineFunction1<TF>(
        argSerializers: List<KSerializer<*>>,
        resultSerializer: KSerializer<*>,
      ) : SuspendingZiplineFunction<SampleService<TF>>(
        "suspend fun reduce(List<T>): T",
        argSerializers,
        resultSerializer,
      ) {
        override suspend fun callSuspending(service: SampleService<TF>, args: List<*>) =
          service.reduce(args[0] as List<TF>)
      }

      class ZiplineFunction2<TF>(
        argSerializers: List<KSerializer<*>>,
        resultSerializer: KSerializer<*>,
      ) : ReturningZiplineFunction<SampleService<TF>>(
        "fun close(): kotlin.Unit",
        argSerializers,
        resultSerializer,
      ) {
        override fun call(service: SampleService<TF>, args: List<*>): Any? = service.close()
      }

      override fun ziplineFunctions(
        serializersModule: SerializersModule,
      ): List<ZiplineFunction<SampleService<TX>>> {
        val types = types
        val sampleRequestSerializer = serializersModule.serializer(types[0])
        val sampleResponseSerializer = serializersModule.serializer(types[1])
        val listOfTSerializer = serializersModule.serializer(types[2])
        val unitSerializer = serializersModule.serializer(types[3])
        val unitSerializer2 = serializersModule.contextualSerializerForType(types[3])
        val serializers = serializers
        val suspendCallbackTSerializer = serializers[0]
        return listOf(
          ZiplineFunction0(listOf(sampleRequestSerializer), sampleResponseSerializer),
          ZiplineFunction1(listOf(listOfTSerializer), suspendCallbackTSerializer),
          ZiplineFunction2(listOf(), unitSerializer),
        )
      }

      override fun outboundService(
        callHandler: OutboundCallHandler
      ): SampleService<TX> = GeneratedOutboundService(callHandler)

      private class GeneratedOutboundService<TS>(
        private val callHandler: OutboundCallHandler
      ) : SampleService<TS> {
        override fun ping(request: SampleRequest): SampleResponse {
          val callHandler = callHandler
          return callHandler.call(this, 0, request) as SampleResponse
        }

        override suspend fun reduce(request: List<TS>): TS {
          val callHandler = callHandler
          return callHandler.callSuspending(this, 1, request) as TS
        }

        override fun close() {
          val callHandler = callHandler
          callHandler.closed = true
          return callHandler.call(this, 2) as Unit
        }
      }
    }
  }
}
