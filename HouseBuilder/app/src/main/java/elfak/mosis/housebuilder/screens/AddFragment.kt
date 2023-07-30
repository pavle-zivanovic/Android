package elfak.mosis.housebuilder.screens

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import elfak.mosis.housebuilder.R
import elfak.mosis.housebuilder.models.LocationViewModel
import elfak.mosis.housebuilder.models.data.Item
import elfak.mosis.housebuilder.models.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AddFragment : DialogFragment() {

    private var auth: FirebaseAuth = Firebase.auth
    private var db = Firebase.firestore
    private var names: ArrayList<String> = ArrayList<String>()
    private var receivedItems: ArrayList<Item> = ArrayList()
    private var itemName: String = ""
    private val locationViewModel: LocationViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getReceivedItems()

        val postBtn: Button = requireView().findViewById(R.id.button_post)
        postBtn.setOnClickListener{postItem()}
    }

    override fun onStart() {
        super.onStart()
        if(resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
            dialog?.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        }
        else{
            dialog?.window?.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun getReceivedItems() {
        val userID: String = auth.currentUser?.uid ?: ""
        viewLifecycleOwner.lifecycleScope.launch {
            try{
                val result = withContext(Dispatchers.IO) {
                    db.collection("receivedItems")
                        .whereEqualTo("userID", userID)
                        .get()
                        .await()
                }

                if(result != null) {
                    for(d in result.documents){
                        val item = Item(d.get("name").toString(),
                            d.get("points").toString().toInt(),
                            d.get("longitude").toString(),
                            d.get("latitude").toString(),
                            d.get("userID").toString())
                        receivedItems.add(item)
                    }

                    for(i in 0 until receivedItems.size){
                        names.add(receivedItems[i].name.toString())
                    }
                    names = names.distinct() as ArrayList<String>

                    val spinner: AutoCompleteTextView = requireView().findViewById(R.id.spinner)
                    val adapter = ArrayAdapter(requireView().context, android.R.layout.simple_spinner_dropdown_item, names as List<Any?>)
                    spinner.setAdapter(adapter)
                    spinner.setOnItemClickListener { adapterview, view, i, l ->
                        val txtInputLayoutSpinner: TextInputLayout = requireView().findViewById(R.id.textInputLayoutSpinner)
                        itemName = adapterview.getItemAtPosition(i).toString()
                        when(itemName){
                            "concrete" -> txtInputLayoutSpinner.setStartIconDrawable(R.drawable.baseline_concrete)
                            "brick" -> txtInputLayoutSpinner.setStartIconDrawable(R.drawable.baseline_brick)
                            "door" -> txtInputLayoutSpinner.setStartIconDrawable(R.drawable.baseline_door)
                            "window" ->txtInputLayoutSpinner.setStartIconDrawable(R.drawable.baseline_window)
                            "roof" -> txtInputLayoutSpinner.setStartIconDrawable(R.drawable.baseline_roof)
                            "chimney" -> txtInputLayoutSpinner.setStartIconDrawable(R.drawable.baseline_chimney)
                        }
                    }
                }
            }
            catch (e: Exception) {
                Log.w("ITEM", "ERROR", e)
            }
        }
    }

    private fun postItem(){
        val myLoc =  GeoLocation(
            locationViewModel.latitude.value!!.toDouble(),
            locationViewModel.longitude.value!!.toDouble())
        val radius = 5.0

        val bounds = GeoFireUtils.getGeoHashQueryBounds(myLoc, radius)
        val tasks: MutableList<Task<QuerySnapshot>> = ArrayList()
        for (b in bounds) {
            val q = db.collection("markers")
                .orderBy("hash")
                .startAt(b.startHash)
                .endAt(b.endHash)
            tasks.add(q.get())
        }

        Tasks.whenAllComplete(tasks)
            .addOnCompleteListener {
                val matchingDocs: MutableList<DocumentSnapshot> = ArrayList()
                for (task in tasks) {
                    val snap = task.result
                    for (doc in snap!!.documents) {
                        val lat = doc.get("latitude").toString().toDouble()
                        val lng = doc.get("longitude").toString().toDouble()

                        val docLocation = GeoLocation(lat, lng)
                        val distanceInM = GeoFireUtils.getDistanceBetween(docLocation, myLoc)
                        if (distanceInM <= radius) {
                            matchingDocs.add(doc)
                        }
                    }
                }

                if(matchingDocs.isEmpty()){
                    val userID: String = auth.currentUser?.uid ?: ""
                    viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            val result = withContext(Dispatchers.IO) {
                                db.collection("receivedItems")
                                    .whereEqualTo("userID", userID)
                                    .whereEqualTo("name", itemName)
                                    .get()
                                    .await()
                            }

                            if(result != null){
                                val itemId = result.documents[0].id
                                val item = Item(result.documents[0].get("name").toString(),
                                    result.documents[0].get("points").toString().toInt(),
                                    result.documents[0].get("longitude").toString(),
                                    result.documents[0].get("latitude").toString(),
                                    result.documents[0].get("userID").toString())

                                db.collection("receivedItems").document(itemId).delete()
                                    .addOnSuccessListener { Log.d("ITEM", "Successfully deleted from receivedItems") }
                                    .addOnFailureListener{ Log.d("ITEM", "Failed to delete in receivedItems") }

                                item.longitude = locationViewModel.longitude.value
                                item.latitude = locationViewModel.latitude.value

                                db.collection("postedItems").add(item)
                                    .addOnSuccessListener { Log.d("ITEM", "Success writing into postedItems") }
                                    .addOnFailureListener{ Log.d("ITEM", "Fail writing into postedItems") }

                                item.points?.let { locationViewModel.setPoints(it) }
                                item.name?.let { locationViewModel.setItemName(it) }

                                val database = Firebase.database("https://house-builder-7dd6e-default-rtdb.firebaseio.com/")
                                    .reference.child("users").child(userID)

                                database.get()
                                    .addOnSuccessListener { result ->
                                        val user = result.getValue<User>()
                                        val userItems = user?.items?.minus(1)
                                        database.child("items").setValue(userItems)
                                    }
                                    .addOnFailureListener{
                                        Log.d("USER", "Fail to update!") }

                                findNavController().navigate(R.id.MapFragment)
                            }
                        }
                        catch (e: Exception) {
                            Log.w("ITEM", "ERROR", e)
                        }
                    }
                }
                else{
                    Toast.makeText(view?.context, "There is already an item at that location!", Toast.LENGTH_SHORT).show()
                }
            }
    }
}