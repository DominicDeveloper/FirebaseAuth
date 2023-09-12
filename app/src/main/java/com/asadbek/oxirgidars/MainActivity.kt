package com.asadbek.oxirgidars

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import com.asadbek.oxirgidars.databinding.ActivityMainBinding
import com.google.android.play.core.integrity.e
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    lateinit var binding:ActivityMainBinding
    lateinit var auth: FirebaseAuth
    private val TAG = "PhoneActivity"
    lateinit var storedVerificationId:String
    lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        auth.setLanguageCode("uz")


        binding.btn.setOnClickListener {
            val phoneNumber = binding.edt1.text.toString().trim()
            sendVerificationCode(phoneNumber)
        }

        // kelgan sms codni avtomatik o'qib olish
        binding.edt2.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_DONE){
                verifyCode()
                val view = currentFocus
                if (view != null){
                    val imm:InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken,0)
                }
            }

            true
        }
        //codni qo'lda yozish
        binding.edt2.addTextChangedListener {
            if (it.toString().length == 6){
                verifyCode()
            }
        }
        binding.txtRestart.setOnClickListener {
            val phoneNumber = binding.edt1.text.toString().trim()
            resentCode(phoneNumber)
        }


    }

    // tasdiqlanish qanday yakunlanganligini bilishimiz uchun
    private val callback = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks(){
        override fun onVerificationCompleted(p0: PhoneAuthCredential) {
            // This callback will be invoked in two situations:
            // Callback 2 xolat uchun chaqiriladi
            // 1 - Instant verification. In some cases the phone number can be instantly
            //     verified without needing to send or enter a verification code.
            // 1 - Tezkor tekshirish. Ba'zi hollarda telefon raqami darhol bo'lishi mumkin
            //            // tasdiqlash kodini yuborish yoki kiritish zaruratisiz tasdiqlangan.
            // 2 - Auto-retrieval. On some devices Google Play services can automatically
            //     detect the incoming verification SMS and perform verification without
            //     user action.
            // Avtomatik qidirish. Ba'zi qurilmalarda Google Play xizmatlari avtomatik ravishda bo'lishi mumkin
            //            // kiruvchi tasdiqlash SMS-xabarini aniqlang va tasdiqlashsiz amalga oshiring
            //            // foydalanuvchi harakati.
            Log.d(TAG, "onVerificationCompleted:$p0")
            signInWithPhoneAuthCredential(p0)
        }

        override fun onVerificationFailed(p0: FirebaseException) {
            // This callback is invoked in an invalid request for verification is made,
            // Bu callback so`rov yuborishda muvaffaqiyatsizlikka uchradi
            // for instance if the the phone number format is not valid.
            // telefon raqami notogri kiritilgani sababli yoki telefon raqam mavjud bo`lmaganligi sabab
            Log.w(TAG, "onVerificationFailed", p0)

            if (p0 is FirebaseAuthInvalidCredentialsException) {
                // Invalid request
                 // telefon raqamdagi xatolik
            } else if (p0 is FirebaseTooManyRequestsException) {
                // kop marotaba kod qabul qilish uchun so`rov yubirilgandagi xatolik
                // The SMS quota for the project has been exceeded
            }

            // Show a message and update the UI
        }

        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
            super.onCodeSent(verificationId, token)
            // The SMS verification code has been sent to the provided phone number, we
            // Sms orqali tasdiqlanadigan kod ko`rsatilgan telefon raqamga yuborildi
            // now need to ask the user to enter the code and then construct a credential
            // endi userdan ushbu kodni kiritishini sorashimiz kerak bo`ladi
            // by combining the code with a verification ID.
            // kodni tasdiqlash identifikatori bilan birlashtirish orqali.
            Log.d(TAG, "onCodeSent:$verificationId")

            // Save verification ID and resending token so we can use them later
            // verification Id ni saqlash va qayta tokenga yuborish, buni keyinroqxam ishlatishimiz mumkin
            storedVerificationId = verificationId
            resendToken = token
        }

    }

    // agar tasdiqlash muvaffaqiyatli bo`lsa yoki aksi bo`lsa bizga xabar beriladi
    private fun signInWithPhoneAuthCredential(p0: PhoneAuthCredential) {
        auth.signInWithCredential(p0)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful){
                    // Sign in success, update UI with the signed-in user's information
                    // Muvaffaqiyatli kirildi, UI user malumotlari bilan kirish orqali yangilanadi
                    Log.d(TAG, "signInWithCredential:success")

                    Toast.makeText(this, "Muvaffaqiyatli", Toast.LENGTH_SHORT).show()

                    val user = task.result?.user
                }else{
                    // Sign in failed, display a message and update the UI
                    // Kirishda xatolik, bir habar chiqadi va ui yangilanadi
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(this, "Muvaffaqiyatsiz!!!", Toast.LENGTH_SHORT).show()
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        // The verification code entered was invalid
                        Toast.makeText(this, "Kod xato kiritildi", Toast.LENGTH_SHORT).show()
                    }
                    // Update UI
                }
            }
    }

    // tasdiqlash kodini yuborish uchun funksiya
    private fun sendVerificationCode(phoneNumber: String) {
        val option = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber) // telefon raqam
            .setTimeout(60L,TimeUnit.SECONDS)
            .setActivity(this) // Activity for call back binding
            .setCallbacks(callback) // call backni o`rnatish
            .build() // ushbu so`rovni qurish

        PhoneAuthProvider.verifyPhoneNumber(option)
    }

    private fun verifyCode(){
        val code = binding.edt2.text.toString().trim()
        if (code.length == 6){
            val credential = PhoneAuthProvider.getCredential(storedVerificationId,code)
            signInWithPhoneAuthCredential(credential)
        }
    }
    private fun resentCode(phoneNumber: String){
        val option = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber) // telefon raqam
            .setTimeout(60L,TimeUnit.SECONDS)
            .setActivity(this) // Activity for call back binding
            .setCallbacks(callback) // call backni o`rnatish
            .setForceResendingToken(resendToken) // kodni qayta jo`natish uchun
            .build() // ushbu so`rovni qurish

        PhoneAuthProvider.verifyPhoneNumber(option)
    }

}