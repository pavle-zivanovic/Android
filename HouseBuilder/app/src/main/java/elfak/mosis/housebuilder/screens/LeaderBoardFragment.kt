package elfak.mosis.housebuilder.screens

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import elfak.mosis.housebuilder.R
import elfak.mosis.housebuilder.helpers.ListUsersAdapter
import elfak.mosis.housebuilder.models.data.ListUsers
import elfak.mosis.housebuilder.models.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LeaderBoardFragment : Fragment() {

    private lateinit var usersArrayList: ArrayList<ListUsers>
    private lateinit var arrayListWithHeader: ArrayList<ListUsers>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_leader_board, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val listView: ListView = requireView().findViewById(R.id.leaderboard)
        val headerEl = ListUsers("Username", "Houses", "Points", R.mipmap.ic_launcher.toString())
        arrayListWithHeader = arrayListOf(headerEl)
        usersArrayList = arrayListOf()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    Firebase.database("https://house-builder-7dd6e-default-rtdb.firebaseio.com/")
                        .reference.child("users")
                        .get()
                        .await()
                }

                for(user in result.children){
                    val u = user.getValue<User>()
                    val userList = ListUsers(u?.username, u?.houseNumber.toString(), u?.points.toString(), u?.image)
                    usersArrayList += listOf(userList)
                }

                usersArrayList.sortWith(compareBy({it.houseNumber}, {it.points}))
                usersArrayList.reverse()
                arrayListWithHeader += usersArrayList
                val listAdapter = ListUsersAdapter(view.context, arrayListWithHeader)
                listView.adapter = listAdapter

            } catch (e: Exception) {
                Log.w("USER", "ERROR", e)
            }
        }
    }
}