package elfak.mosis.housebuilder.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import elfak.mosis.housebuilder.R
import elfak.mosis.housebuilder.models.data.User

class HomeFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth
        val usernameText: TextView = requireView().findViewById(R.id.home_username_textView)
        val pointsText: TextView = requireView().findViewById(R.id.home_points_textView)
        val houseNumberText: TextView = requireView().findViewById(R.id.home_houseNumber_textView)
        usernameText.text = auth.currentUser?.email?.substringBefore("@")

        val database = Firebase.database("https://house-builder-7dd6e-default-rtdb.firebaseio.com/")
            .reference.child("users").child(auth.currentUser?.uid.toString())

        val userListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue<User>()
                val pTxt = "Points: " + user?.points.toString()
                val hnTxt = "Built houses: " + user?.houseNumber.toString()
                pointsText.text = pTxt
                houseNumberText.text = hnTxt
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        database.addValueEventListener(userListener)
    }
}