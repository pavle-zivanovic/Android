package elfak.mosis.housebuilder.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import elfak.mosis.housebuilder.R
import elfak.mosis.housebuilder.databinding.FragmentLoginBinding
import elfak.mosis.housebuilder.models.LoginAndSignupViewModel

class LoginFragment : Fragment() {

    private val userViewModel: LoginAndSignupViewModel by viewModels()
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.userViewModel = userViewModel

        binding.buttonLogin.setOnClickListener{userViewModel.login()}
        binding.buttonSignup.setOnClickListener{findNavController().navigate(R.id.action_LoginFragment_to_SignupFragment)}
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}