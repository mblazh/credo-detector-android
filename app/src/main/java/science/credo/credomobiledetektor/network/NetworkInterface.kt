package science.credo.credomobiledetektor.network

import android.content.Context
import android.util.Log
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import science.credo.credomobiledetektor.database.DataManager
import science.credo.credomobiledetektor.info.ConfigurationInfo
import science.credo.credomobiledetektor.info.IdentityInfo
import science.credo.credomobiledetektor.info.UserInfo
import science.credo.credomobiledetektor.network.message.`in`.ErrorFrame
import science.credo.credomobiledetektor.network.message.out.*

/**
 * Created by poznan on 02/09/2017.
 */
class NetworkInterface (context: Context){

    val mContext = context
    val mMapper = jacksonObjectMapper()
    val mConfigurationManager = ConfigurationInfo(context)
    val mDataManager = DataManager.getInstance(context)
    val mIdentityInfo = IdentityInfo.getInstance(context)

    fun send(outFrame: OutFrame): NetworkCommunication.Response {
        return NetworkCommunication.post(mMapper.writeValueAsString(outFrame))
    }

    fun sendPing() : Boolean {
        val userData = UserInfo.getNewInstance(mContext).getUserData()
        val deviceData = mIdentityInfo.getIdentityData()
        val result = send(PingFrame(userData, deviceData))
        return result.code == ok
    }

    fun sendUserInfo(): Boolean {
        val userData = UserInfo.getNewInstance(mContext).getUserData()
        val deviceData = mIdentityInfo.getIdentityData()
        val result = send(UserInfoFrame(userData, deviceData))
        return result.code == ok
    }

    fun sendRegister(): NetworkCommunication.Response {
        val userData = UserInfo.getNewInstance(mContext).getUserDataRegister()
        val deviceData = mIdentityInfo.getIdentityData()
        val result = send(RegisterFrame(userData, deviceData))
        return result
    }

    fun sendLogin(): NetworkCommunication.Response {
        val userData = UserInfo.getNewInstance(mContext).getUserData()
        val deviceData = mIdentityInfo.getIdentityData()
        val result = send(LoginFrame(userData.key, deviceData))
        return result
    }

    fun getError(message: String) : ErrorFrame? {
        return mMapper.readValue(message, ErrorFrame::class.java)
    }

    fun sendHitsToNetwork(): Boolean {
        if (mConfigurationManager.canUpload) {
            val hits = mDataManager.getHits()
            for (hit in hits) {
                val userData = UserInfo.getNewInstance(mContext).getUserData()
                val deviceData = mIdentityInfo.getIdentityData()
                val result = send(DetectionFrame(hit, userData,deviceData))
                Log.d("upload.message",result.message)
                Log.d("upload.code",result.code.toString())
                if (result.code != ok) return false
                else {
                    mDataManager.storeCachedHit(hit)
                    mDataManager.removeHit(hit)
                }
            }
            return true
        } else {
            return false
        }
    }

    companion object {
        private var mNetworkInterface: NetworkInterface? = null
        val ok = 200
        val notImplemented = 501
        val error = 400
        val forbidden = 403
        fun getInstance(context: Context): NetworkInterface {
            if (mNetworkInterface == null) {
                mNetworkInterface = NetworkInterface(context)
            }
            return mNetworkInterface!!
        }
    }
}