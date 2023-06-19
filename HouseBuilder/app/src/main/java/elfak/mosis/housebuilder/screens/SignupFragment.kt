package elfak.mosis.housebuilder.screens

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import elfak.mosis.housebuilder.R
import elfak.mosis.housebuilder.databinding.FragmentSignupBinding
import elfak.mosis.housebuilder.models.LoginAndSignupViewModel

class SignupFragment : Fragment() {

    private val userViewModel: LoginAndSignupViewModel by viewModels()
    private var _binding: FragmentSignupBinding? = null
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

        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.userViewModel = userViewModel

        binding.buttonSignup.setOnClickListener{userViewModel.createAccount()}
        binding.buttonLogin.setOnClickListener{findNavController().navigate(R.id.action_SignupFragment_to_LoginFragment)}
        binding.buttonAddpicture.setOnClickListener{chooseImage()}
    }

    private fun chooseImage(){
        val i: Intent = Intent()
        i.setType("image/*")
        i.setAction(Intent.ACTION_GET_CONTENT)
        startActivityForResult(Intent.createChooser(i, "Add picture"), SELECT_PICTURE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == RESULT_OK){
            if(requestCode == SELECT_PICTURE){
                val selectedImageUri: Uri = data?.getData()!!
                binding.uploadImage.setImageURI(selectedImageUri)
                binding.uploadImage.isDrawingCacheEnabled = true
                binding.uploadImage.buildDrawingCache()
                val bitmap: Bitmap = (binding.uploadImage.drawable as BitmapDrawable).bitmap
                userViewModel.setImage(bitmap)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}