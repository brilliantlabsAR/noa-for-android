package xyz.brilliant.argpt.helpers

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.min

class BleHelper(
    private val bluetoothGatt: BluetoothGatt?,
    private val rawRxCharacteristic: BluetoothGattCharacteristic?,
    private val rxCharacteristic: BluetoothGattCharacteristic?,
    private val frameRxCharacteristic: BluetoothGattCharacteristic?
) {

    private var writingREPLProgress: Boolean = false

    @SuppressLint("MissingPermission")
     fun rawBleWrite(data: ByteArray) {
        val characteristic = rawRxCharacteristic
        if (bluetoothGatt != null && characteristic != null) {
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            characteristic.value = data
            bluetoothGatt.writeCharacteristic(characteristic)
        }
    }

    @SuppressLint("MissingPermission")
     fun bleWrite(characteristic: BluetoothGattCharacteristic?, data: ByteArray, resultDeferred: CompletableDeferred<String>) {
        if (bluetoothGatt != null && characteristic != null) {
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            var offset = 0
            val chunkSize = 100
            while (offset < data.size) {
                if (writingREPLProgress) {
                    continue
                }
                val length = min(chunkSize, data.size - offset)
                val chunkData = data.sliceArray(offset until offset + length)
                characteristic.value = chunkData
                writingREPLProgress = true
                bluetoothGatt.writeCharacteristic(characteristic)
                offset += length
            }
        }
        resultDeferred.complete("Done")
    }

    private suspend fun sendBle(characteristic: BluetoothGattCharacteristic?, data: ByteArray): String {
        return coroutineScope {
            val resultDeferred = CompletableDeferred<String>()
            val handler = Handler(Looper.getMainLooper())
            GlobalScope.launch {
                bleWrite(characteristic, data, resultDeferred)
            }

            // Set up the response handler callback
            val bleWriteComplete = CompletableDeferred<String>()
            val responseCallback = { responseString: String ->
                println("[RECEIVED]: $responseString\n")
                if (bleWriteComplete.isActive) {
                    bleWriteComplete.complete(responseString)
                }
            }

            // Resolve if the response handler callback isn't called
            launch {
                handler.postDelayed({
                    if (!resultDeferred.isCompleted) {
                        resultDeferred.complete("")
                    }
                    if (!bleWriteComplete.isCompleted) {
                        bleWriteComplete.complete("")
                    }
                }, 3000)
            }
            resultDeferred.await()
            bleWriteComplete.await()
        }
    }

    fun rawBleWrite(data: String) {
        val chunkSize = 90
        var offset = 0
        val actualData = data.substring(4)
        val command = data.substring(0, 4)
        println(actualData)
        writingREPLProgress = false
        while (offset < actualData.length) {
            if (writingREPLProgress) {
                continue
            }
            val length = min(chunkSize, actualData.length - offset)
            val chunk = command + actualData.substring(offset, offset + length)
            writingREPLProgress = true
            rawBleWrite(chunk.toByteArray())
            offset += length
        }
    }

    suspend fun replSendBle(data: String): String {
        return sendBle(rxCharacteristic, (data.toByteArray() + byteArrayOf(0x04)))
    }

    suspend fun frameSendBle(data: String): String {
        return sendBle(frameRxCharacteristic, data.toByteArray())
    }

    suspend fun replSendBle(data: ByteArray): String {
        return sendBle(rxCharacteristic, data)
    }

    suspend fun frameSendBle(data: ByteArray): String {
        return sendBle(frameRxCharacteristic, data)
    }
}
