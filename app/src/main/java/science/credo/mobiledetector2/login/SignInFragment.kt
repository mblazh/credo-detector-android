package science.credo.mobiledetector2.login

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.mobsandgeeks.saripaar.ValidationError
import com.mobsandgeeks.saripaar.Validator
import com.mobsandgeeks.saripaar.annotation.NotEmpty
import com.mobsandgeeks.saripaar.annotation.Password
import kotlinx.coroutines.*
import org.json.JSONObject
import science.credo.mobiledetector2.App
import science.credo.mobiledetector2.main.MainActivity
import science.credo.mobiledetector2.R
import science.credo.mobiledetector2.network.RestInterface
import science.credo.mobiledetector2.utils.Prefs
import science.credo.mobiledetector2.utils.UiUtils

class SignInFragment private constructor() : Fragment(), Validator.ValidationListener {

    companion object {

        fun newInstance(): SignInFragment {
            val instance = SignInFragment()
            return instance
        }

    }

    @Password
    lateinit var etPassword: EditText
    lateinit var btSignIn: TextView
    lateinit var btForgotPassword: TextView
    @NotEmpty
    lateinit var etLogin: EditText
    lateinit var viewProgress: View

    private lateinit var validatior: Validator

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_sign_in, container, false)
        etPassword = v.findViewById(R.id.etPassword)
        etLogin = v.findViewById(R.id.etLogin)
        btForgotPassword = v.findViewById(R.id.btForgotPassword)
        btSignIn = v.findViewById(R.id.btSignIn)
        viewProgress = v.findViewById(R.id.viewProgress)

        etPassword.setOnEditorActionListener { v, actionId, event ->
            val b = actionId == EditorInfo.IME_ACTION_DONE
            if (b) {
                btSignIn.performClick()
            }
            return@setOnEditorActionListener b
        }

        btSignIn.setOnClickListener {
            UiUtils.hideSoftKeyboard(activity!!)
            validatior.validate()
        }

        btForgotPassword.setOnClickListener {

            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://api.credo.science/web/password_reset")
            )
            startActivity(browserIntent)


        }

        validatior = Validator(this)
        validatior.setValidationListener(this)

        return v
    }

    override fun onValidationFailed(errors: MutableList<ValidationError>?) {
        GlobalScope.launch(Dispatchers.Main) {
            for (error in errors ?: emptyList<ValidationError>()) {
                if (error.view is EditText) {
                    (error.view as EditText).error = error.getCollatedErrorMessage(context!!)
                } else if (error.view is CheckBox) {
                    UiUtils.showAlertDialog(context!!, error.getCollatedErrorMessage(context!!))
                }
            }
        }
    }

    override fun onValidationSucceeded() {
        signIn()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }


    private fun signIn() {
        val login = etLogin.text.toString()
        val password = etPassword.text.toString()

        GlobalScope.launch(Dispatchers.Main) {
            viewProgress.visibility = View.VISIBLE
            val result = RestInterface.login(context!!, login, password)
            App.token = result.getCastedResponse(LoginResponse::class.java)?.token ?: ""
            Prefs.put(context!!,App.token,Prefs.Keys.USER_TOKEN)
            viewProgress.visibility = View.GONE
            if (result.isSuccess()) {
                Prefs.put(context!!, login, Prefs.Keys.USER_LOGIN)
                Prefs.put(context!!, password, Prefs.Keys.USER_PASSWORD)
                startActivity(MainActivity.intent(context!!))
            } else {
//                startActivity(MainActivity.intent(context!!))
                val code = result.getCode()
                if (code == 400) {
                    val response = JSONObject(result.getResponse())
                    UiUtils.showAlertDialog(context!!, response.get("message").toString())
                } else {
                    UiUtils.showAlertDialog(context!!, result.getResponse())
                }
            }
        }

    }
}