package isimm.ing1.carpoolingstudents.ui.rides.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import isimm.ing1.carpoolingstudents.databinding.ItemRideBinding
import isimm.ing1.carpoolingstudents.model.Ride
import isimm.ing1.carpoolingstudents.model.utils.DateUtils

class RideAdapter(
    private val onRideClick: (Ride) -> Unit
) : ListAdapter<Ride, RideAdapter.RideViewHolder>(RideDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RideViewHolder {
        val binding = ItemRideBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RideViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RideViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RideViewHolder(
        private val binding: ItemRideBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(ride: Ride) {
            binding.apply {

                fromText.text = ride.from
                toText.text = ride.to
                timeText.text = DateUtils.formatTime(ride.departureTime)
                dateText.text = DateUtils.formatDate(ride.departureTime)
                driverNameText.text = ride.driverName
                seatsText.text = "${ride.availableSeats} places"
                priceText.text = "${ride.pricePerSeat} DT"

                root.setOnClickListener {
                    onRideClick(ride)
                }
            }
        }
    }

    class RideDiffCallback : DiffUtil.ItemCallback<Ride>() {
        override fun areItemsTheSame(oldItem: Ride, newItem: Ride): Boolean {
            return oldItem.rideId == newItem.rideId
        }

        override fun areContentsTheSame(oldItem: Ride, newItem: Ride): Boolean {
            return oldItem == newItem
        }
    }
}