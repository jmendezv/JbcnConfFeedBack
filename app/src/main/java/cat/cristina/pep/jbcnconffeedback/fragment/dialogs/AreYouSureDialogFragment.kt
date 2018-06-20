package cat.cristina.pep.jbcnconffeedback.fragment.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.widget.TextView
import cat.cristina.pep.jbcnconffeedback.R
import cat.cristina.pep.jbcnconffeedback.activity.MainActivity

private const val ARG_QUESTION = "param1"
private const val ARG_ACTION = "param2"

/**
 *
 */

class AreYouSureDialogFragment : DialogFragment() {

    private val TAG = AreYouSureDialogFragment::class.java.name

    private var question: String? = null
    private var action: Int? = null
    private var listener: AreYouSureDialogFragmentListener? = null
    private var tvQuestion: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            question = it.getString(ARG_QUESTION)
            action = it.getInt(ARG_ACTION)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity!!)
        val inflater = activity!!.layoutInflater

        val view = inflater.inflate(R.layout.fragment_are_you_sure_dialog, null)

        tvQuestion = view.findViewById(R.id.tv_question)
        tvQuestion?.text = question

        val dialog = builder.setView(view)
                // Add action buttons
                .setPositiveButton(R.string.ok) { dialog, id ->
                    if (action == MainActivity.ARE_YOU_SURE_DIALOG_EXIT_APPLICATION_ACTION)
                        onButtonPressed(MainActivity.ARE_YOU_SURE_DIALOG_EXIT_APPLICATION_RESPONSE)
                    else if (action == MainActivity.ARE_YOU_SURE_DIALOG_FRESH_START_ACTION)
                        onButtonPressed(MainActivity.ARE_YOU_SURE_DIALOG_FRESH_START_RESPONSE)
                }
                .setNegativeButton(R.string.cancel) { _, _ ->
                    onButtonPressed(MainActivity.ARE_YOU_SURE_DIALOG_CANCEL_RESPONSE)
                }.create()

        dialog.window.setBackgroundDrawableResource(android.R.drawable.dialog_holo_light_frame)

        return dialog
    }

    private fun onButtonPressed(resp: Int) {
        listener?.onAreYouSureDialogFragmentInteraction(resp)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is AreYouSureDialogFragmentListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement AreYouSureDialogFragmentListener")
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
     */
    interface AreYouSureDialogFragmentListener {
        fun onAreYouSureDialogFragmentInteraction(resp: Int)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         */
        @JvmStatic
        fun newInstance(question: String, action: Int) =
                AreYouSureDialogFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_QUESTION, question)
                        putInt(ARG_ACTION, action)
                    }
                }
    }
}
