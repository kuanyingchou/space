package com.ken.space.view

import android.content.Intent
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.ken.space.R
import com.ken.space.model.Launch
import com.ken.space.model.LaunchesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import java.util.*

class LaunchesAdapter(lifecycleOwner: LifecycleOwner, val model: LaunchesViewModel): RecyclerView.Adapter<LaunchesAdapter.VH>() {

    // Somehow LiveData has some delay and we don't get the list immediately after
    // configuration changes. We would lose scroll position if we don't update the list here.
    var launches: List<Launch> = model.filteredLaunches.value ?: emptyList()

    init {
        model.filteredLaunches.observe(lifecycleOwner, androidx.lifecycle.Observer { newLaunches ->
            lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                val diff = withContext(Dispatchers.IO) {
                    DiffUtil.calculateDiff(
                        DiffCallback(
                            launches,
                            newLaunches
                        )
                    )
                }
                diff.dispatchUpdatesTo(this@LaunchesAdapter)
                launches = newLaunches
            }
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.launch, parent, false)
        return VH(view)
    }

    override fun getItemCount(): Int {
        return launches.size
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val launch = launches[position]
        holder.nameTextView.text = Html.fromHtml("<b>${launch.name}</b>")
        holder.agencyTextView.text = launch?.launch_service_provider?.name
        holder.padTextView.text = launch?.pad?.name
        holder.netTextView.text =
            DateTimeFormat
                .forStyle("MM")
                .withLocale(Locale.getDefault())
                .print(launch.net.withZone(DateTimeZone.getDefault()))
        holder.imageView.setImageURI(launch.image ?: "")
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, DetailsActivity::class.java)
            intent.putExtra(KEY_LAUNCH_ID, launch.id)
            context.startActivity(intent)
        }
    }

    class VH(itemView: View): RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.name_text_view)
        val agencyTextView: TextView = itemView.findViewById(R.id.agency_text_view)
        val padTextView: TextView = itemView.findViewById(R.id.pad_text_view)
        val netTextView: TextView = itemView.findViewById(R.id.net_text_view)
        val imageView: SimpleDraweeView = itemView.findViewById(R.id.image_view)
    }

    class DiffCallback(private val oldList: List<Launch>, private val newList: List<Launch>): DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }

        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
            return null
        }
    }
}