package elfak.mosis.housebuilder.models


import android.content.Context
import android.graphics.Bitmap
import android.text.Editable
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.NavHostFragment.Companion.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import elfak.mosis.housebuilder.R
import elfak.mosis.housebuilder.models.data.User
import java.io.ByteArrayOutputStream

class LoginAndSignupViewModel : ViewModel(){

    private var auth : FirebaseAuth = Firebase.auth

    private val _image = MutableLiveData<Bitmap>()
    val image: LiveData<Bitmap> = _image

    private val _firstname = MutableLiveData<String>()
    val firstname : LiveData<String> = _firstname

    private val _lastname = MutableLiveData<String>()
    val lastname : LiveData<String> = _lastname

    private val _username = MutableLiveData<String>()
    val username : LiveData<String> = _username

    private val _password = MutableLiveData<String>()
    val password : LiveData<String> = _password

    private val _phone = MutableLiveData<String>()
    val phone : LiveData<String> = _phone

    fun setImage(image: Bitmap){
        _image.value = image
    }

    fun onFirstNameTextChanged(fName: Editable?){
        _firstname.value = fName.toString()
    }

    fun onLastNameTextChanged(lName: Editable?){
        _lastname.value = lName.toString()
    }

    fun onUsernameTextChanged(username: Editable?){
        _username.value = username.toString()
    }

    fun onPasswordTextChanged(pass: Editable?){
        _password.value = pass.toString()
    }

    fun onPhoneTextChanged(phone: Editable?){
        _phone.value = phone.toString()
    }

    fun createAccount(context: Context, fragment: Fragment){
        if(checkInfo(false)){
            val username = "${username.value}"+"@gmail.com"
            auth.createUserWithEmailAndPassword(username, password.value!!)
                .addOnCompleteListener() { task ->
                    if(task.isSuccessful){
                        UploadInfo(context, fragment)
                    }
                    else{
                        Toast.makeText(context, "User account is not created.", Toast.LENGTH_SHORT).show()
                    }
                }
        }
        else{
            Toast.makeText(context, "Not all fields are filled!", Toast.LENGTH_SHORT).show()
        }
    }

    fun login(context: Context){
        if(checkInfo(true)){
            val username = "${username.value}"+"@gmail.com"
            auth.signInWithEmailAndPassword(username, password.value!!)
                .addOnCompleteListener() { task ->
                    if(task.isSuccessful){
                        Log.d("LOGIN", "Login success.")
                    }
                    else{
                        Toast.makeText(context, "Login failed.", Toast.LENGTH_SHORT).show()
                    }
                }
        }
        else{
            Toast.makeText(context, "Not all fields are filled!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun UploadInfo(context: Context, fragment: Fragment){
        val userID: String = auth.currentUser?.uid ?: ""
        if(userID == ""){
            Log.w("UerID", "Error!")
        }

        var storage = Firebase.storage
        var imageRef: StorageReference? = storage.reference.child("users").child(userID).child("${username.value}.jpg")
        val baos = ByteArrayOutputStream()
        val bitmap = image.value
        bitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()
        val uploadTask = imageRef!!.putBytes(data)

        val urlTask = uploadTask.continueWithTask{ task ->
            if(!task.isSuccessful) {
                task.exception?.let {
                    val user = auth.currentUser
                    user!!.delete()
                        .addOnCompleteListener{ task ->
                            if(task.isSuccessful){
                                Log.d("SIGNUP", "User account deleted.")
                            }
                        }
                }
            }
            imageRef.downloadUrl
        }.addOnCompleteListener{ task->
            if(task.isSuccessful){
                val imageUrl = task.result.toString()
                val user = User(username.value, password.value, firstname.value, lastname.value, phone.value, imageUrl)
                val database = Firebase.database("https://house-builder-7dd6e-default-rtdb.firebaseio.com/")
                database.reference.child("users").child(userID).setValue(user)
                Log.d("SIGNUP", "User account created.")
                findNavController(fragment).navigate(R.id.action_SignupFragment_to_LoginFragment)
            }
        }
    }

    private fun checkInfo(login: Boolean): Boolean {
        var checked = true

        if(login){
            if(username.value == null){
                checked = false
            }
            else if(password.value == null){
                checked = false
            }
        }

        else{
            if(firstname.value == null){
                checked = false
            }
            else if(lastname.value == null){
                checked = false
            }
            else if(phone.value == null){
                checked = false
            }
            else if(username.value == null){
                checked = false
            }
            else if(password.value == null){
                checked = false
            }
            else if(image.value == null){
                checked = false
            }
        }

        return checked
    }
}