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

class CollectedItemsFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_collected_items, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth
        val concreteText: TextView = requireView().findViewById(R.id.concreteNumber_textView)
        val brickText: TextView = requireView().findViewById(R.id.brickNumber_textView)
        val roofText: TextView = requireView().findViewById(R.id.roofNumber_textView)
        val doorText: TextView = requireView().findViewById(R.id.doorNumber_textView)
        val windowText: TextView = requireView().findViewById(R.id.windowNumber_textView)
        val chimneyText: TextView = requireView().findViewById(R.id.chimneyNumber_textView)

        val database = Firebase.database("https://house-builder-7dd6e-default-rtdb.firebaseio.com/")
            .reference.child("users").child(auth.currentUser?.uid.toString())

        val userListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue<User>()
                val cTxt = " Concrete: " + user?.concreteNumber.toString() + "/12"
                val bTxt = " Brick: " + user?.brickNumber.toString() + "/8"
                val rTxt = " Roof: " + user?.roofNumber.toString() + "/4"
                val dTxt = " Door: " + user?.doorNumber.toString() + "/1"
                val wTxt = " Window: " + user?.windowNumber.toString() + "/4"
                val chTxt = " Chimney: " + user?.chimneyNumber.toString() + "/1"
                concreteText.text = cTxt
                brickText.text = bTxt
                roofText.text = rTxt
                doorText.text = dTxt
                windowText.text = wTxt
                chimneyText.text = chTxt
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        database.addValueEventListener(userListener)
    }
}