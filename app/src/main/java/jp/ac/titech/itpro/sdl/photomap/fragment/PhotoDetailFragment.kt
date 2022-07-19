package jp.ac.titech.itpro.sdl.photomap.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import jp.ac.titech.itpro.sdl.photomap.R
import jp.ac.titech.itpro.sdl.photomap.databinding.FragmentPhotoDetailBinding
import jp.ac.titech.itpro.sdl.photomap.viewmodel.PhotoDetailViewModel
import java.text.SimpleDateFormat
import java.util.*

class PhotoDetailFragment : Fragment() {
    private val photoDetailViewModel: PhotoDetailViewModel by activityViewModels()
    private lateinit var photoDetailFragmentBinding: FragmentPhotoDetailBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        photoDetailFragmentBinding = FragmentPhotoDetailBinding.inflate(inflater, container, false)
        return photoDetailFragmentBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val latlng = photoDetailViewModel.latLng
        val location = photoDetailViewModel.location
        val photos = photoDetailViewModel.photos
        val photosFrame = photoDetailFragmentBinding.photoDetails
        val sdf = SimpleDateFormat(FORMAT, Locale.JAPAN)

        Log.d(TAG, "onViewCreated")

        photoDetailFragmentBinding.detailLocation.text =
            if (location.isNullOrEmpty()) {
                "lat: ${latlng?.latitude}, lng: ${latlng?.longitude}"
            } else {
                location
            }
        photos?.let {
            it.forEach{ photo ->
                Log.d(TAG, "Add view")
                val elem = requireActivity().layoutInflater.inflate(R.layout.photo_detail_element, null)
                elem.findViewById<ImageView>(R.id.photo_detail_image).setImageBitmap(photo.image)
                elem.findViewById<TextView>(R.id.detail_date).text = sdf.format(photo.date)
                if (!photo.memo.isNullOrEmpty()) {
                    elem.findViewById<TextView>(R.id.detail_memo).text = photo.memo
                }
                photosFrame.addView(elem)
            }
        } ?: run {
            Log.d(TAG, "view model contains null")
        }
    }

    companion object {
        private const val FORMAT = "yyyy年MM月dd日E曜日 HH時mm分"
        private const val TAG = "Photo Detail Fragment"
    }
}