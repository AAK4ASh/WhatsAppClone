package com.example.whatsapp

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import com.example.whatsapp.databinding.ActivityOtpBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

const val  PHONE_NUMBER ="phoneNumber"
class OtpActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private lateinit var binding: ActivityOtpBinding
    private var phoneNumber: String? = null
    private var mVerificationId: String? = null
    private var mCounterDown: CountDownTimer? = null
    private var mResendToken: PhoneAuthProvider.ForceResendingToken? = null
    private lateinit var progressDialog: ProgressDialog
    private var timeLeft: Long = -1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtpBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        initView()
        startVerify()
    }

    private fun startVerify() {
        startPhoneNumberVerification(phoneNumber!!)
        showTimer(60000)
        progressDialog = createProgressDialog("Sending a Verification Code", isCancelable = false)
        progressDialog.show()
    }


    private fun showTimer(milliesInFuture: Long) {// to show a timer
        binding.resendBtn.isEnabled = false
        mCounterDown = object : CountDownTimer(milliesInFuture, 1000) {
            override fun onTick(milliesInFuture: Long) {
                timeLeft = milliesInFuture
                binding.counterTv.isVisible = true
                binding.counterTv.text = "Seconds remaining: " + milliesInFuture / 1000


            }

            override fun onFinish() {
                binding.resendBtn.isEnabled = true
                binding.counterTv.isVisible = false
            }

        }.start()

    }


    private fun initView() {
        phoneNumber = intent.getStringExtra(PHONE_NUMBER)//ph no from login
        binding.verifyTv.text = getString(R.string.verify_number, phoneNumber)
        setSpannableString()

        binding.verificationBtn.setOnClickListener(this)
        binding.resendBtn.setOnClickListener(this)
        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verification without
                //     user action.
                if (::progressDialog.isInitialized) {
                    progressDialog.dismiss()
                }
                val smsMessageSent = credential.smsCode
                if (!smsMessageSent.isNullOrBlank())
                    binding.sentCodeEt.setText(smsMessageSent)
                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                if (::progressDialog.isInitialized) {
                    progressDialog.dismiss()
                }

                if (e is FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                    Log.e("Exception:", "FirebaseAuthInvalidCredentialsException", e)
                    Log.e("Exception:", "FirebaseAuthInvalidCredentialsException" + e.message)
                } else if (e is FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                    Log.e("Exception:", "FirebaseTooManyRequestException", e)
                }

                // Show a message and update the UI
                notifyUserAndRetry("Your Phone Number might be wrong or connection error . Retry Again")
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.
                progressDialog.dismiss()
                binding.counterTv.isVisible = false
                Log.e("onCodeSent==", "onCodeSent:$verificationId")

                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId
                mResendToken = token
            }
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        val mAuth = FirebaseAuth.getInstance()
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {

                    if (::progressDialog.isInitialized) {
                        progressDialog.dismiss()
                    }
                    //First Time Login
                    if (task.result?.additionalUserInfo?.isNewUser == true) {
                        showSignUpActivity()
                    } else {
                        showHomeActivity()
                    }
                } else {

                    if (::progressDialog.isInitialized) {
                        progressDialog.dismiss()
                    }

                    notifyUserAndRetry("Your Phone Number Verification is failed.Retry again!")
                }
            }
    }

    private fun showSignUpActivity() {
        startActivity(Intent(this, SignUpActivity::class.java))
        finish()
    }

    private fun showHomeActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onBackPressed() {

    }

    override fun onDestroy() {
        super.onDestroy()
        if (mCounterDown!=null){
            mCounterDown!!.cancel()
        }
    }

    private fun setSpannableString() {
        val span = SpannableString(getString(R.string.waiting_text, phoneNumber))
        val clickSpan: ClickableSpan = object : ClickableSpan() {
            override fun updateDrawState(ds: TextPaint) {
                ds.color = ds.linkColor
                ds.isUnderlineText = false
            }

            override fun onClick(p0: View) {
                showLoginActivity()
            }
        }
        span.setSpan(clickSpan, span.length - 13, span.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.waitingTv.movementMethod = LinkMovementMethod.getInstance()
        binding.waitingTv.text = span
    }

    private fun notifyUserAndRetry(message: String) {
        MaterialAlertDialogBuilder(this).apply {
            setMessage(message)
            setPositiveButton("ok") { _, _ ->
                showLoginActivity()
            }
            setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            setCancelable(false)
            create()
            show()
        }
    }

    private fun showLoginActivity() {
        startActivity(
            Intent(this, LoginActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)//clear stacks
        )
    }


    private fun startPhoneNumberVerification(phoneNumber: String) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            phoneNumber,
            60,
            TimeUnit.SECONDS,
            this,
            callbacks
        )
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.verificationBtn -> {
                var code = binding.sentCodeEt.text.toString()
                if (code.isNotEmpty() && !mVerificationId.isNullOrEmpty()) {
                    progressDialog = createProgressDialog("Please Wait", isCancelable = false)
                    progressDialog.show()
                    val credential =
                        PhoneAuthProvider.getCredential(mVerificationId!!, code.toString())
                    signInWithPhoneAuthCredential(credential)
                }
            }
            binding.resendBtn -> {
                if (mResendToken != null) {
                    resendVerificationCode(phoneNumber.toString(),mResendToken )
                    showTimer(60000)
                    progressDialog = createProgressDialog("Sending a Verification Code", false)
                    progressDialog.show()
                } else {
                    Toast.makeText(
                        baseContext,
                        "Sorry, You Can't request new code now, Please wait ...)",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    }

    private fun resendVerificationCode(phoneNumber: String,mResendToken:PhoneAuthProvider.ForceResendingToken?) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(phoneNumber,
        60,TimeUnit.SECONDS,this,callbacks,mResendToken)
    }

    fun Context.createProgressDialog(message: String, isCancelable: Boolean): ProgressDialog {
        return ProgressDialog(this).apply {
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            setMessage(message)
        }

    }
}