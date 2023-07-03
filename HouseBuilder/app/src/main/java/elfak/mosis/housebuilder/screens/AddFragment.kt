package elfak.mosis.housebuilder.screens

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import elfak.mosis.housebuilder.R
import elfak.mosis.housebuilder.models.data.Item
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AddFragment : DialogFragment() {

    private var auth : FirebaseAuth = Firebase.auth
    private var db = Firebase.firestore
    private var names: ArrayList<String> = ArrayList<String>()
    var receivedItems: ArrayList<Item> = ArrayList()

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
    }

    private fun getReceivedItems() {
        val userID: String = auth.currentUser?.uid ?: ""
        Log.d("USERID", userID)
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
                        val item = Item(d.get("name").toString(), d.get("image").toString(), d.get("points").toString().toInt(),
                            d.get("longitude").toString(), d.get("latitude").toString(), d.get("userID").toString())
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
                        Toast.makeText(requireView().context, adapterview.getItemAtPosition(i).toString(), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            catch (e: Exception) {
                Log.w("ITEM", "ERROR", e)
            }
        }
    }
}