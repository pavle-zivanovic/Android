package elfak.mosis.housebuilder.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import elfak.mosis.housebuilder.R
import elfak.mosis.housebuilder.models.data.ListItems

class ListItemsAdapter(context: Context, list: ArrayList<ListItems>) :
    ArrayAdapter<ListItems>(context, R.layout.itemlist_list_item, R.id.listItems_tmp, list){

    private lateinit var cView: View

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item: ListItems? = getItem(position)

        cView = LayoutInflater.from(context).inflate(R.layout.itemlist_list_item, parent, false)

        val image: ImageView? = cView.findViewById(R.id.listItems_image)
        val name: TextView? = cView.findViewById(R.id.listItems_name)
        val author: TextView? = cView.findViewById(R.id.listItems_author)
        val points: TextView? = cView.findViewById(R.id.listItems_points)
        val date: TextView? = cView.findViewById(R.id.listItems_date)
        val lat: TextView? = cView.findViewById(R.id.listItems_latitude)
        val long: TextView? = cView.findViewById(R.id.listItems_longitude)

        image?.setImageDrawable(item?.image)
        name?.text = item?.name
        author?.text = item?.author
        points?.text = item?.points
        date?.text = item?.date
        lat?.text = item?.latitude
        long?.text = item?.longitude

        return super.getView(position, cView, parent)
    }
}