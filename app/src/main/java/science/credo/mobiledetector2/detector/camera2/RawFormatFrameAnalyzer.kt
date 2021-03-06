package science.credo.mobiledetector2.detector.camera2

import android.graphics.ImageFormat
import android.util.Base64
import science.credo.mobiledetector2.detector.*
import science.credo.mobiledetector2.detector.old.OldCalibrationResult
import science.credo.mobiledetector2.detector.old.OldFrameAnalyzer
import science.credo.mobiledetector2.utils.BitmapUtils
import science.credo.mobiledetector2.utils.ConstantsNamesHelper
import science.credo.mobiledetector2.utils.LocationHelper
import science.credo.mobiledetector2.utils.SensorHelper
import kotlin.math.max
import kotlin.math.min

object RawFormatFrameAnalyzer : BaseFrameAnalyzer() {

    override suspend fun checkHit(
        frame: Frame,
        frameResult: BaseFrameResult,
        calibration: BaseCalibrationResult,
        thresholdAmplifier : Float?
    ): Hit? {
        frameResult as RawFormatFrameResult
        calibration as RawFormatCalibrationResult
        val max = calibration.detectionThreshold

        println("=============================== check hit ${frameResult.max}   $max")
        if (frameResult.max > max) {

            val margin = OldFrameAnalyzer.HIT_BITMAP_SIZE / 2
            val centerX = frameResult.maxIndex.rem(frame.width)
            val centerY = frameResult.maxIndex / frame.width

            val offsetX = max(0, centerX - margin)
            val offsetY = max(0, centerY - margin)
            val endX = min(frame.width, centerX + margin)
            val endY = min(frame.height, centerY + margin)


            val bitmap = BitmapUtils.createBitmap(
                frame.byteArray,
                if (frame.imageFormat == ImageFormat.RAW_SENSOR) 2 else 1,
                frame.width,
                frame.height
            )

            val scaledWidth = frame.width / calibration.clusterFactorWidth
            val x = frameResult.maxIndex % scaledWidth
            val y = frameResult.maxIndex / scaledWidth

            val croppedSize = 70
            var startRow = (y * calibration.clusterFactorHeight) - (croppedSize / 2)
            var startColumn = (x * calibration.clusterFactorWidth) - (croppedSize / 2)

            if (startColumn + croppedSize > bitmap.width) {
                startColumn = bitmap.width - croppedSize
            }
            if (startColumn < 0) {
                startColumn = 0
            }

            if (startRow + croppedSize > bitmap.height) {
                startRow = bitmap.height - croppedSize
            }
            if (startRow < 0) {
                startRow = 0
            }

            val croppedBitmap = BitmapUtils.createCropped(
                bitmap,
                startColumn,
                startRow,
                croppedSize
            ).await()


            val cropDataPNG = bitmap2png(croppedBitmap)
            val dataString = Base64.encodeToString(cropDataPNG, Base64.DEFAULT)

            val hit = Hit()
            hit.frameContent = dataString
            hit.timestamp = frame.timestamp
            hit.latitude = LocationHelper.location?.latitude
            hit.longitude = LocationHelper.location?.longitude
            hit.altitude = LocationHelper.location?.altitude
            hit.accuracy = LocationHelper.location?.accuracy
            hit.provider = LocationHelper.location?.provider
            hit.width = frame.width
            hit.height = frame.height
            hit.x = centerX
            hit.y = centerY
            hit.maxValue = frameResult.max
            hit.format = ConstantsNamesHelper.getFormatName(frame.imageFormat)

            hit.clusteringFactor =
                "${calibration.clusterFactorWidth}x${calibration.clusterFactorHeight}"
            hit.calibrationNoise = calibration.calibrationNoise
            hit.thresholdAmplifier = thresholdAmplifier
            hit.threshold = calibration.detectionThreshold

            if (calibration is OldCalibrationResult) {
                hit.blackThreshold = calibration.blackThreshold
                hit.processingMethod = "OFFICIAL"
            }else{
                hit.processingMethod = "EXPERIMENTAL"
            }
            hit.exposure = frame.exposureTime


            hit.average = frameResult.avg.toFloat()
//            hit.blacksPercentage = frameResult.blacksPercentage
            hit.ax = SensorHelper.accX
            hit.ay = SensorHelper.accY
            hit.az = SensorHelper.accZ
            hit.temperature = SensorHelper.temperature

            return hit
        } else {
            calibration.adjustThreshold(frameResult.max,thresholdAmplifier!!)
            return null
        }

    }


}