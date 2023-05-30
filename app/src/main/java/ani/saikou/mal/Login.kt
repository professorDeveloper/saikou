package ani.saikou.mal

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ani.saikou.*
import ani.saikou.mal.MAL.clientId
import ani.saikou.mal.MAL.saveResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val data: Uri = intent?.data
                ?: throw Exception("Mal Login : Uri not Found")
            val codeChallenge: String = loadData("malCodeChallenge", this)
                ?: throw Exception("Mal Login : codeChallenge not found")
            val code = data.getQueryParameter("code")
                ?: throw Exception("Mal Login : Code not present in Redirected URI")

            snackString("Logging in MAL")
            lifecycleScope.launch(Dispatchers.IO) {
                tryWithSuspend(true) {
                    val res = client.post(
                        "https://myanimelist.net/v1/oauth2/token",
                        data = mapOf(
                            "client_id" to clientId,
                            "code" to code,
                            "code_verifier" to codeChallenge,
                            "grant_type" to "authorization_code"
                        )
                    ).parsed<MAL.ResponseToken>()
                    saveResponse(res)
                    MAL.token = res.accessToken
                    snackString("Getting User Data")
                    MAL.query.getUserData()
                    launch(Dispatchers.Main) {
                        startMainActivity(this@Login)
                    }
                }
            }
        }
        catch (e:Exception){
            logError(e,snackbar = false)
            startMainActivity(this)
        }
    }

}