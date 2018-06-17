package cat.cristina.pep.jbcnconffeedback.fragment.dialogs

import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View

import cat.cristina.pep.jbcnconffeedback.R
import cat.cristina.pep.jbcnconffeedback.activity.MainActivity
import cat.cristina.pep.jbcnconffeedback.model.DatabaseHelper
import cat.cristina.pep.jbcnconffeedback.model.UtilDAOImpl
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.j256.ormlite.android.apptools.OpenHelperManager


private val TAG = PieChartDialogFragment::class.java.name

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_ID = "given_id"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [PieChartDialogFragment.PieChartDialogFragmentListener] interface
 * to handle interaction events.
 * Use the [PieChartDialogFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class PieChartDialogFragment : DialogFragment() {

    private var param2: String? = null
    private var dataFromFirestore: Map<Long?, List<QueryDocumentSnapshot>>? = null
    private var listenerPieChartDialog: PieChartDialogFragmentListener? = null
    private lateinit var pieChart: PieChart
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var utilDAOImpl: UtilDAOImpl
    private lateinit var dialog: ProgressDialog
    private var givenTalkId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            givenTalkId = it.getLong(ARG_ID)
            param2 = it.getString(ARG_PARAM2)
        }
        databaseHelper = OpenHelperManager.getHelper(activity, DatabaseHelper::class.java)
        utilDAOImpl = UtilDAOImpl(context!!, databaseHelper)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity!!)
        // Get the layout inflater
        val inflater = activity!!.layoutInflater

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        val view = inflater.inflate(R.layout.fragment_pie_chart_dialog, null)

        val dialog = builder.setView(view)
                // Add action buttons
                .setPositiveButton(R.string.ok) { dialog, id ->
                    onButtonPressed("")
                }.create()


        pieChart = view.findViewById<PieChart>(R.id.pie_chart_id)

        dialog.window.setBackgroundDrawableResource(android.R.drawable.dialog_holo_light_frame)

        return dialog

    }

    override fun onStart() {
        super.onStart()
        downloadFirebaseData()
    }

    private fun downloadFirebaseData() {

        dialog = ProgressDialog(context, ProgressDialog.THEME_HOLO_LIGHT)
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        dialog.setMessage(resources.getString(R.string.loading))
        dialog.isIndeterminate = true
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()

        FirebaseFirestore.getInstance()
                .collection(MainActivity.FIREBASE_COLLECTION)
                .get()
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        dataFromFirestore = it.result.groupBy {
                            it.getLong(MainActivity.FIREBASE_COLLECTION_FIELD_TALK_ID)
                        }
                        setupPieChart()
                    } else {
                        if (dialog.isShowing)
                            dialog.dismiss()
                        //Toast.makeText(this, R.string.sorry_no_graphic_available, Toast.LENGTH_LONG).show()
                        //Log.d(TAG, "*** Error *** ${it.exception?.message}")
                    }
                }

    }

    private fun setupPieChart() {

        try {
            val description = Description()
            with(description) {
                text = "A title"
            }
            pieChart.description = description
            pieChart.isRotationEnabled = true
            //pieChart.setUsePercentValues(true)
            //pieChart.setHoleColor(Color.BLUE)
            //pieChart.setCenterTextColor(Color.BLACK
            pieChart.holeRadius = 25f
            pieChart.setTransparentCircleAlpha(0)
            pieChart.centerText = "Super Cool Chart"
            pieChart.setCenterTextSize(10.0F)
            //pieChart.setDrawEntryLabels(true);
            //pieChart.setEntryLabelTextSize(20);
            addDataSet()
        } catch (error: Exception) {
        } finally {
            if (dialog.isShowing)
                dialog.dismiss()
        }

    }

    private fun addDataSet() {

        Log.d(TAG, "addDataSet started")

        try {

            val yData = floatArrayOf(25.3f, 10.6f, 66.76f, 44.32f, 46.01f)
            val xData = arrayOf("Bad", "Better", "OK", "Good", "Great")

            val yEntrys = ArrayList<PieEntry>()
            val xEntrys = ArrayList<String>()

            for (i in 0 until yData.size) {
                yEntrys.add(PieEntry(yData[i], i))
            }

            for (i in 1 until xData.size) {
                xEntrys.add(xData[i])
            }

            //create the data set
            val pieDataSet = PieDataSet(yEntrys, "Scoring")
            pieDataSet.sliceSpace = 2f
            pieDataSet.valueTextSize = 12f

            //add colors to dataset
            val colors = ArrayList<Int>()
            colors.add(Color.GRAY)
            colors.add(Color.BLUE)
            colors.add(Color.RED)
            colors.add(Color.GREEN)
            colors.add(Color.CYAN)
            colors.add(Color.YELLOW)
            colors.add(Color.MAGENTA)

            pieDataSet.colors = colors

            //add legend to chart
            val legend = pieChart.legend
            legend.form = Legend.LegendForm.CIRCLE
            legend.position = Legend.LegendPosition.LEFT_OF_CHART

            //create pie data object
            val pieData = PieData(pieDataSet)
            pieChart.data = pieData
            pieChart.invalidate()
        } catch (error: Exception) {
        } finally {
            if (dialog.isShowing)
                dialog.dismiss()
        }
    }

    fun onButtonPressed(msg: String) {
        listenerPieChartDialog?.onPieChartDialogFragmentInteraction(msg)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is PieChartDialogFragmentListener) {
            listenerPieChartDialog = context
        } else {
            throw RuntimeException(context.toString() + " must implement PieChartDialogFragmentListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listenerPieChartDialog = null
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
    interface PieChartDialogFragmentListener {
        fun onPieChartDialogFragmentInteraction(msg: String)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment PieChartDialogFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(id: Long, param2: String? = null) =
                PieChartDialogFragment().apply {
                    arguments = Bundle().apply {
                        putLong(ARG_ID, id)
                        putString(ARG_PARAM2, param2)
                    }
                }
    }
}
