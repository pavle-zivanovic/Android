package elfak.mosis.housebuilder.screens

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import elfak.mosis.housebuilder.R
import elfak.mosis.housebuilder.helpers.ListItemsAdapter
import elfak.mosis.housebuilder.models.ItemsListViewModel
import elfak.mosis.housebuilder.models.data.ListItems
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ItemsTableFragment : Fragment() {

    private lateinit var itemsArrayList: ArrayList<ListItems>
    private lateinit var arrayListWithHeader: ArrayList<ListItems>
    private var db = Firebase.firestore
    private val itemsListViewModel: ItemsListViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_items_table, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val listView: ListView = requireView().findViewById(R.id.listItems)
        val imageHeader: Drawable? = ResourcesCompat.getDrawable(resources, R.mipmap.ic_launcher, null)
        val headerEl = ListItems("Name", "Author", "Points", imageHeader, "Date")
        arrayListWithHeader = arrayListOf(headerEl)
        itemsArrayList = arrayListOf()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    db.collection("markers")
                        .get()
                        .await()
                }

                if(result != null){
                    var drawableImage: Int? = null
                    for(i in result.documents){
                        when(i.get("name").toString()){
                            "concrete" -> drawableImage = R.drawable.concrete
                            "brick" -> drawableImage = R.drawable.brick
                            "door" -> drawableImage = R.drawable.door
                            "window" -> drawableImage = R.drawable.window
                            "roof" -> drawableImage = R.drawable.roof
                            "chimney" -> drawableImage = R.drawable.chimney
                        }
                        val image: Drawable? = ResourcesCompat.getDrawable(resources, drawableImage!!, null)

                        val user = i.get("user").toString().substringBefore("@")
                        val itemList = ListItems(i.get("name").toString(), user,
                            i.get("points").toString(), image, i.get("dateCreated").toString(),
                        i.get("latitude").toString(), i.get("longitude").toString())
                        itemsArrayList += listOf(itemList)
                    }

                    arrayListWithHeader += itemsArrayList
                    val listAdapter = ListItemsAdapter(view.context, arrayListWithHeader)
                    listView.adapter = listAdapter
                    listView.isClickable = true
                    listView.setOnItemClickListener { parent, view, position, id ->
                        if(position != 0){
                            val latTxt: TextView = view.findViewById(R.id.listItems_latitude)
                            val longTxt: TextView = view.findViewById(R.id.listItems_longitude)
                            itemsListViewModel.setLatLong(latTxt.text.toString(), longTxt.text.toString())
                            itemsListViewModel.setFlag("no")
                            findNavController().navigate(R.id.action_ItemsTableFragment_to_MapFragment)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("ITEM", "ERROR", e)
            }
        }
    }
}