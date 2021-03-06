package cat.cristina.pep.jbcnconffeedback.fragment

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.Fragment
import android.support.v7.preference.PreferenceManager
import android.util.Log
import android.view.*
import android.widget.ProgressBar
import android.widget.Toast
import cat.cristina.pep.jbcnconffeedback.R
import cat.cristina.pep.jbcnconffeedback.activity.MainActivity
import cat.cristina.pep.jbcnconffeedback.model.DatabaseHelper
import cat.cristina.pep.jbcnconffeedback.model.UtilDAOImpl
import cat.cristina.pep.jbcnconffeedback.utils.PreferenceKeys
import cat.cristina.pep.jbcnconffeedback.utils.shortenName
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.j256.ormlite.android.apptools.OpenHelperManager
import com.opencsv.CSVWriter
import com.opencsv.bean.ColumnPositionMappingStrategy
import com.opencsv.bean.StatefulBeanToCsvBuilder
import kotlinx.android.synthetic.main.fragment_statistics.*
import java.io.File
import java.io.FileWriter
import java.util.*

// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
private val TAG = StatisticsFragment::class.java.name

/**
 */
class StatisticsFragment : Fragment(), OnChartValueSelectedListener {

    private val TAG = StatisticsFragment::class.java.name

    private var param1: String? = null
    private var param2: String? = null
    private var listenerStatistics: StatisticsFragmentListener? = null
    private var dataFromFirestore: Map<Long?, List<QueryDocumentSnapshot>>? = null
    private lateinit var databaseHelper: DatabaseHelper
    lateinit var sharedPreferences: SharedPreferences
    private var bestTalks: Boolean = true
    // List<Pair<title, avg>>
    private lateinit var talksToDisplay: List<Triple<Long?, String, Double>>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        setHasOptionsMenu(true)
        databaseHelper = OpenHelperManager.getHelper(activity, DatabaseHelper::class.java)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        bestTalks = sharedPreferences.getBoolean(PreferenceKeys.STATISTICS_BEST_TALKS_KEY, true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_statistics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        downloadScoring()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater?.inflate(R.menu.statistics_fragment, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return true
    }

    /* This method downloads the Scoring collection made up of documents(id_talk, score, date) */
    private fun downloadScoring(): Unit {

        if (!(context as MainActivity).isDeviceConnectedToWifiOrData().first) {
            (activity as MainActivity).toast(R.string.sorry_no_graphic_available)
            //Toast.makeText(context, R.string.sorry_no_graphic_available, Toast.LENGTH_LONG).show()
            return
        }

//        dialog = ProgressDialog(activity, ProgressDialog.THEME_HOLO_LIGHT)
//        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
//        dialog.setMessage(resources.getString(R.string.loading))
//        dialog.isIndeterminate = true
//        dialog.setCanceledOnTouchOutside(false)
//        dialog.show()

        FirebaseFirestore.getInstance()
                .collection(MainActivity.FIREBASE_COLLECTION)
                .get()
                .addOnSuccessListener {
                    if (isVisible ) {
                        dataFromFirestore = it.groupBy {
                            it.getLong(MainActivity.FIREBASE_COLLECTION_FIELD_TALK_ID)
                        }
                        val numTalks = Integer
                                .parseInt(sharedPreferences!!.getString(PreferenceKeys.STATISTICS_TALK_LIMIT_KEY, resources.getString(R.string.pref_default_statistics_talk_limit)))
                        setupGraphTopNTalks(numTalks)
                    }
                }
                .addOnFailureListener {
                    if (isVisible) {
                        progressBar.visibility = ProgressBar.GONE
                        (activity as MainActivity).toast(R.string.sorry_no_graphic_available)
                        //Toast.makeText(context, R.string.sorry_no_graphic_available, Toast.LENGTH_LONG).show()
                    }
                }

    }

    private fun isLargeDevice(): Boolean =
            resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK == Configuration.SCREENLAYOUT_SIZE_LARGE

    private fun setupGraphTopNTalks(limit: Int) {

        try {
            // Pair<title, avg>
            val listOfIdTitleAndAvg = ArrayList<Triple<Long?, String, Double>>()
            val labels = ArrayList<String>()
            val entries = ArrayList<BarEntry>()
            var index = 0.0F
            //val talkDao: Dao<Talk, Int> = databaseHelper.getTalkDao()
            val utilDAOImpl = UtilDAOImpl(context!!, databaseHelper)

            dataFromFirestore
                    ?.asSequence()
                    ?.sortedBy {
                        it.key
                    }
                    ?.forEach {
                        val votes = it.value.size

                        val avg: Double? = dataFromFirestore
                                ?.get(it.key)
                                ?.asSequence()
                                ?.map { doc ->
                                    doc.get(MainActivity.FIREBASE_COLLECTION_FIELD_SCORE) as Long
                                }?.average()

                        try {
                            val talk = utilDAOImpl.lookupTalkByGivenId(it.key!!.toInt())
                            var title: String = talk.title
                            val refAuthor = talk.speakers?.get(0)
                            var author = utilDAOImpl.lookupSpeakerByRef(refAuthor!!).name

                            /* Si el nombre es demasiado largo se saldra de la barra  */
                            author = shortenName(author)
                            title = if (title.length > 35) "'${StringBuilder(title.substring(0, 35)).toString()}...' by $author. ($votes votes)"
                            else "'$title' by $author. ($votes votes)"
                            listOfIdTitleAndAvg.add(Triple(it.key, title, avg!!))
                            // Log.d(TAG, "************************************ $title $avg")
                        } catch (error: Exception) {
                            Log.d(TAG, "***** datafromfirestori ******************* conflicting key  ${it.key}  ${error.printStackTrace(System.err)}")
                        }
                    }

            val firstNTalks = if (bestTalks) {
                listOfIdTitleAndAvg
                        .sortedWith(compareBy { it.third })
                        .asReversed()
                        // .sortedByDescending {it.second }
                        .subList(0, Math.min(limit, listOfIdTitleAndAvg.size))
            } else {
                listOfIdTitleAndAvg
                        .sortedWith(compareBy { it.third })
                        //.asReversed()
                        // .sortedByDescending {it.second }
                        .subList(0, Math.min(limit, listOfIdTitleAndAvg.size))
            }


            talksToDisplay = firstNTalks

            val maxAvg = if (bestTalks) firstNTalks.first().third else firstNTalks.last().third

            for (triple in firstNTalks) {
                //Log.d(TAG, "************************************ ${pair.first} ${pair.second}")
                entries.add(BarEntry(index++, triple.third.toFloat()))
                var title: String = triple.second
                //title = StringBuilder(title).append(" (${String.format("%.2f", pair.second)})").toString()
                labels.add(title)
            }

            val barDataSet: BarDataSet = BarDataSet(entries, "Score")

            with(barDataSet) {
                colors = ColorTemplate.COLORFUL_COLORS.asList()
                barBorderColor = Color.BLACK
            }

            val barData = BarData(barDataSet)
            barData.setDrawValues(true)
            barData.setValueTextColor(Color.BLACK)
            barData.setValueTextSize(if (isLargeDevice()) 24.0F else 16.0F)


            with(barChart) {
                data = barData
                xAxis.valueFormatter = IndexAxisValueFormatter(labels) as IAxisValueFormatter?
                xAxis.textSize = if (isLargeDevice()) 18.0F else 12.0F
                xAxis.setDrawLabels(true)
                xAxis.position = XAxis.XAxisPosition.BOTTOM_INSIDE
                xAxis.xOffset = if (isLargeDevice()) 600.0F else 400.0F
                xAxis.yOffset = 100.0F
                xAxis.setLabelCount(firstNTalks!!.size, false)
                axisLeft.axisMinimum = 0.0F
                //axisLeft.axisMaximum = if (maxAvg.isPresent) maxAvg.get().second.toFloat() + .25F else 5.25F
                axisLeft.axisMaximum = maxAvg.toFloat() + 0.5F
                axisLeft.setDrawLabels(false)
                axisRight.setDrawLabels(false)
                fitScreen()
                description.isEnabled = false
                //description.text = resources.getString(R.string.chart_description)
                setNoDataText(resources.getString(R.string.sorry_no_graphic_available))
                setDrawBarShadow(true)
                //setDrawValueAboveBar(true)
                //setFitBars(true)
                setDrawBorders(true)
                setBorderColor(Color.BLACK)
                setTouchEnabled(true)
                //onChartGestureListener = this@StatisticsFragment
                animateXY(2_500, 5_000)
                legend.isEnabled = false
//            legend.textColor = Color.GRAY
//            legend.textSize = 15F
                setOnChartValueSelectedListener(this@StatisticsFragment)
                notifyDataSetChanged()
                invalidate()
            }

        } catch (error: Exception) {
            //Toast.makeText(context, error.message, Toast.LENGTH_SHORT).show()
            Log.d(TAG, "************************************ ${error.printStackTrace(System.err)}")
        } finally {
            progressBar.visibility = ProgressBar.GONE
        }

    }

    /*
    * No usar.
    * CSV From list of objects
    * No va, tiene una depedencia externa y da error missing "java.beans.Introspector"
    * https://code.google.com/archive/p/openbeans/downloads
    *
    * Lo añado al proyecto como dependencia externa pero sigue sin ir y cuando
    * hago un rebuild del proyecto elimina el directorio libs y el contenido
    * */
    private fun createCVSFromStatisticsByObject(fileName: String): Unit {
        data class Statistic(val id: Long, val title: String, val score: Int, val date: Date)

        val file = File(context?.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
        val fileWriter = FileWriter(file)
        val statistics = arrayListOf<Statistic>()

        dataFromFirestore
                ?.asSequence()
                ?.forEach {
                    dataFromFirestore?.get(it.key)
                            ?.asSequence()
                            ?.map { doc ->
                                val id = doc.getLong("id_talk")
                                val title = databaseHelper.getTalkDao().queryForId(id?.toInt()).title
                                val score = doc.get("score") as Int
                                val date = doc.getDate("date") as Date
                                statistics.add(Statistic(id!!, title, score, date))
                            }
                }

        val mappingStrategy =
                ColumnPositionMappingStrategy<Statistic>()
                        .apply {
                            type = Statistic::class.java
                            setColumnMapping("id_talk", "title", "score", "date")
                        }

        var beanToCsv = StatefulBeanToCsvBuilder<Statistic>(fileWriter)
                .withMappingStrategy(mappingStrategy)
                .withSeparator(CSVWriter.DEFAULT_SEPARATOR)
                .withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
                .withEscapechar(CSVWriter.DEFAULT_ESCAPE_CHARACTER)
                .withLineEnd(CSVWriter.DEFAULT_LINE_END)
                .build()

        beanToCsv.write(statistics)
        Log.d(TAG, fileWriter.toString())
    }

    fun onButtonPressed(givenTalkId: Long?) {
        listenerStatistics?.onStatisticsFragmentInteraction(givenTalkId)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is StatisticsFragmentListener) {
            listenerStatistics = context
        } else {
            throw RuntimeException(context.toString() + " must implement StatisticsFragmentListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listenerStatistics = null
    }

    /**
     */
    interface StatisticsFragmentListener {
        fun onStatisticsFragmentInteraction(givenTalkId: Long?)
    }

    /**
     * Called when nothing has been selected or an "un-select" has been made.
     */
    override fun onNothingSelected() {
        Log.d(TAG, "onNothingSelected")
    }

    /**
     * Called when a value has been selected inside the chart.
     *
     * @param entry The selected Entry
     * @param highlight The corresponding highlight object that contains information
     * about the highlighted position such as dataSetIndex, ...
     */
    override fun onValueSelected(entry: Entry?, highlight: Highlight?) {
        val index = entry?.x?.toInt()!!
        val tripleIdTitleAvg = talksToDisplay[index]
        //Log.d(TAG, "id ${ttd.first} title ${ttd.second} avg ${ttd.third}")
        onButtonPressed(tripleIdTitleAvg.first)
    }


    companion object {
        /**
         */
        @JvmStatic
        fun newInstance(param1: String = "param1", param2: String = "param2") =
                StatisticsFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
                }
    }

}
