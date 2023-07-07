package elfak.mosis.housebuilder.screens

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import elfak.mosis.housebuilder.R
import elfak.mosis.housebuilder.databinding.FragmentEditProfileBinding
import elfak.mosis.housebuilder.helpers.ActionState
import elfak.mosis.housebuilder.models.LoginAndSignupViewModel
import elfak.mosis.housebuilder.models.data.User

class EditProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private val userViewModel: LoginAndSignupViewModel by viewModels()
    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    var SELECT_PICTURE: Int = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_edit_profile, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.userViewModel = userViewModel

        auth = Firebase.auth

        val database = Firebase.database("https://house-builder-7dd6e-default-rtdb.firebaseio.com/")
            .reference.child("users").child(auth.currentUser?.uid.toString())

        val userListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue<User>()
                binding.inputFirstname.text = user?.firstname?.toEditable()
                binding.inputLastname.text = user?.lastname?.toEditable()
                binding.inputPhone.text = user?.phone?.toEditable()
                binding.inputUsername.text = user?.username?.substringBefore("@")?.toEditable()
                binding.inputPassword.text = user?.password?.toEditable()
                binding.changeImage.setImageURI(user?.image?.toUri())
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        database.addValueEventListener(userListener)

        binding.buttonChangeUsername.setOnClickListener{userViewModel.updateUsername()}
        binding.buttonChangePassword.setOnClickListener{userViewModel.updatePassword()}
        binding.buttonAddpicture.setOnClickListener{chooseImage()}
        binding.buttonChangepicture.setOnClickListener{userViewModel.updateImage()}

        checkActionState()
    }

    private fun chooseImage(){
        val i: Intent = Intent()
        i.setType("image/*")
        i.setAction(Intent.ACTION_GET_CONTENT)
        startActivityForResult(Intent.createChooser(i, "Add picture"), SELECT_PICTURE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK){
            if(requestCode == SELECT_PICTURE){
                val selectedImageUri: Uri = data?.getData()!!
                binding.changeImage.setImageURI(selectedImageUri)
                binding.changeImage.isDrawingCacheEnabled = true
                binding.changeImage.buildDrawingCache()
                val bitmap: Bitmap = (binding.changeImage.drawable as BitmapDrawable).bitmap
                userViewModel.setImage(bitmap)
            }
        }
    }

    private fun checkActionState(){
        val actionState = Observer<ActionState> { state ->
            if(state == ActionState.Success){
                Toast.makeText(view?.context, "Successfully updated", Toast.LENGTH_SHORT).show()
            }
            else if(state is ActionState.ActionError){
                Toast.makeText(view?.context, state.message, Toast.LENGTH_SHORT).show()
            }
        }
        userViewModel.actionState.observe(viewLifecycleOwner, actionState)
    }

    fun String.toEditable(): Editable =  Editable.Factory.getInstance().newEditable(this)
}