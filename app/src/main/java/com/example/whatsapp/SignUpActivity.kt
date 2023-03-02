package com.example.whatsapp

import android.Manifest
import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.UserHandle
import android.util.Log
import android.widget.Toast
import com.example.whatsapp.databinding.ActivitySignUpBinding
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.auth.User
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.storage
import java.security.Permission

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private val storage by lazy { FirebaseStorage.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
  private val database by lazy { FirebaseFirestore.getInstance()}
    private  lateinit var downloadUrl:String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        binding.userImgView.setOnClickListener{
            checkPermissionForImage()
        }
        binding.nextBtn.setOnClickListener {
            val name = binding.nameEt.text.toString()
            if (!::downloadUrl.isInitialized) {
                toast("Photo cannot be empty")
            } else if (name.isEmpty()) {
                toast("Name cannot  be empty")
            } else {
                val user = com.example.whatsapp.models.User(name, downloadUrl, downloadUrl/*Needs to thumbnai url*/, auth.uid!!)
                Log.d("SignUpActivity", user.toString())
                database.collection("users").document(auth.uid!!).set(user).addOnSuccessListener {//user name into databse collection                    Toast.makeText(this,"Successfully Registered", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }.addOnFailureListener {
                    binding.nextBtn.isEnabled = true
                    Toast.makeText(this,it.message, Toast.LENGTH_SHORT).show()

            }
        }

        }
    }

    private fun checkPermissionForImage() {
        if ((checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)== PackageManager.PERMISSION_DENIED)
            &&(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)== PackageManager.PERMISSION_DENIED)
        ) {
            Toast.makeText(this, "If case", Toast.LENGTH_SHORT).show()
            val permission = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            val permissionStorage= arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            val permissionForImages= arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
            requestPermissions(permission,1001)
            requestPermissions(permissionStorage,1002)
            requestPermissions(permissionForImages,1003)
            pickImageFromGallery()
            }
        else{
            pickImageFromGallery()
        }

    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type= "image/*"
        startActivityForResult(intent,1000)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode==Activity.RESULT_OK&&requestCode==1000){
            data?.data?.let {
                binding.userImgView.setImageURI(it)
                startUpload(it)
            }
        }
    }

    private fun startUpload(filepath: Uri) {
        binding.nextBtn.isEnabled= false
        val ref = storage.reference.child("uploads/" +auth.uid.toString())
        val uploadTask = ref.putFile(filepath)
uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>>{ task ->
    if (!task.isSuccessful){
        task.exception?.let {
            throw it
        }
    }
return@Continuation ref.downloadUrl
}
).addOnCompleteListener { task ->
    if (task.isSuccessful) {
        downloadUrl = task.result.toString()
        binding.nextBtn.isEnabled = true
    } else {
        binding.nextBtn.isEnabled = true
    }
}.addOnFailureListener{

}
    }
}

