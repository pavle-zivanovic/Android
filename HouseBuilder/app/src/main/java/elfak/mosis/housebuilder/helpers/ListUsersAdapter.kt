package elfak.mosis.housebuilder.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import elfak.mosis.housebuilder.R
import elfak.mosis.housebuilder.models.data.ListUsers

class ListUsersAdapter(context: Context, list: ArrayList<ListUsers>) :
    ArrayAdapter<ListUsers>(context, R.layout.leaderboard_list_item, R.id.leaderboard_tmp, list) {

    private lateinit var cView: View

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val user: ListUsers? = getItem(position)

        cView = LayoutInflater.from(context).inflate(R.layout.leaderboard_list_item, parent, false)

        val image: ImageView? = cView.findViewById(R.id.leaderboard_image)
        val userName: TextView? = cView.findViewById(R.id.leaderboard_username)
        val houses: TextView? = cView.findViewById(R.id.leaderboard_houseNumber)
        val points: TextView? = cView.findViewById(R.id.leaderboard_points)

        if(user?.username == "Username"){
            image?.setImageResource(user.image!!.toInt())
        }
        else{
            Glide.with(context).load(user?.image).into(image!!)
        }

        userName?.text = user?.username
        houses?.text = user?.houseNumber
        points?.text = user?.points

        return super.getView(position, cView, parent)
    }
}