package cat.cristina.pep.jbcnconffeedback.fragment

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import cat.cristina.pep.jbcnconffeedback.R
import cat.cristina.pep.jbcnconffeedback.activity.MainActivity
import cat.cristina.pep.jbcnconffeedback.utils.ScheduleContentProvider
import cat.cristina.pep.jbcnconffeedback.utils.VenueContentProvider

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [AssetsManagerFragment.AssetsManagerFragmentListener] interface
 * to handle interaction events.
 * Use the [AssetsManagerFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class AssetsManagerFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var listener: AssetsManagerFragmentListener? = null

    lateinit var scheduleContentProvider: ScheduleContentProvider
    lateinit var venueContentProvider: VenueContentProvider

    /*
    * Retain this fragment across configuration changes.
    * */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        retainInstance = true

        scheduleContentProvider = ScheduleContentProvider(context!!, MainActivity.JBCNCONF_JSON_FAKE_SCHEDULES_FILE_NAME)
        venueContentProvider = VenueContentProvider(context!!, MainActivity.JBCNCONF_JSON_VENUES_FILE_NAME)

    }

    /* No GUI Fragment, must return null */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return null
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(msg: String) {
        listener?.onAssetsManagerFragmentInteraction(msg)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is AssetsManagerFragmentListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement AssetsManagerFragmentListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface AssetsManagerFragmentListener {
        fun onAssetsManagerFragmentInteraction(msg: String)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment AssetsManagerFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String = "not", param2: String = "used") =
                AssetsManagerFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
                }
    }
}
