package com.example.gr8math // Make sure this matches your package name

import android.content.Intent // <-- NEW IMPORT
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputLayout
import com.example.gr8math.TeacherAddClassActivity // <-- NEW IMPORT
import com.example.gr8math.adapter.ClassAdapter
import com.example.gr8math.api.ConnectURL
import com.example.gr8math.dataObject.TeacherClass
import com.example.gr8math.utils.ShowToast
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.text.isNullOrEmpty

class TeacherClassManagerActivity : AppCompatActivity() {

    // Declare all the views
    private lateinit var defaultView: ConstraintLayout
    private lateinit var searchView: ConstraintLayout
    private lateinit var mainToolbar: MaterialToolbar
    private lateinit var tilSearch: TextInputLayout
    private lateinit var searchIcon: ImageView

    private lateinit var etSearch : TextInputEditText
    private lateinit var addClassesButton: Button

    //for search suggestions //remember!!!!!!
    private lateinit var llPastSearches : LinearLayout

    private var currentCall: Call<ResponseBody>? = null // <-- track active API call

    private lateinit var tvNoResults : TextView
    private lateinit var pastSearch: TextView
    private lateinit var space : View
    private lateinit var searchLayout : RecyclerView
    private lateinit var classAdapter: ClassAdapter

    private lateinit var role: String
    private var id: Int = 0
    private lateinit var addClassLauncher: ActivityResultLauncher<Intent>
    private lateinit var parentLayout : LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_class_manager_teacher)

        id = intent.getIntExtra("id", 0)
            role = intent.getStringExtra("role")?:""

        if (!role.isNullOrEmpty() && id > 0) {
            inflateClasses(role, id)
        }
        val toastMsg = intent.getStringExtra("toast_msg")
        if(!toastMsg.isNullOrEmpty()){
            ShowToast.showMessage(this, toastMsg)
        }


        parentLayout = findViewById(R.id.class_list_container)
        defaultView = findViewById(R.id.default_view)
        searchView = findViewById(R.id.search_view)
        mainToolbar = findViewById(R.id.toolbar_main)
        tilSearch = findViewById(R.id.tilSearch)
        searchIcon = findViewById(R.id.iv_search)
        addClassesButton = findViewById(R.id.btnAddClasses)
        etSearch = findViewById(R.id.etSearch)
        space = findViewById(R.id.space)
        pastSearch = findViewById(R.id.pastSearch)
        tvNoResults = findViewById(R.id.tvNoResults)
        llPastSearches = findViewById(R.id.llPastSearches)
        // Setup RecyclerView and Adapter
        searchLayout = findViewById(R.id.rvSearchResults) // or your actual ID
        classAdapter = ClassAdapter(mutableListOf()) { selectedClass ->

            val intent = Intent(this, TeacherClassPageActivity::class.java)
            intent.putExtra("id", id)
            intent.putExtra("role", role)
            intent.putExtra("courseId", selectedClass.courseId)
            intent.putExtra("sectionName", selectedClass.sectionName)
            startActivity(intent)
        }

        searchLayout.adapter = classAdapter
        searchLayout.layoutManager = LinearLayoutManager(this)


        // --- Set up Click Listeners ---

        // 1. Main toolbar's profile icon
        mainToolbar.setNavigationOnClickListener {
            finish()
        }

//        // 2. "Add Classes" button
//        addClassesButton.setOnClickListener {
//            val intent = Intent(this@TeacherClassManagerActivity, TeacherAddClassActivity::class.java)
//            intent.putExtra("id", id)
//            startActivity(intent)
//        }

        // 3. Search Icon in the main toolbar (This is correct)
        searchIcon.setOnClickListener {
            // Hide the default view and show the search view
            defaultView.visibility = View.GONE
            searchView.visibility = View.VISIBLE
            inflatePastSearches(id)
        }

        // 4. Back Arrow in the NEW search bar
        tilSearch.setStartIconOnClickListener {
            // Hide the search view and show the default view
            searchView.visibility = View.GONE
            defaultView.visibility = View.VISIBLE
            etSearch.setText("")
            classAdapter.updateData(mutableListOf())
            searchLayout.visibility = View.GONE
            tvNoResults.visibility = View.GONE
            llPastSearches.visibility = View.VISIBLE
            inflatePastSearches(id)
        }

        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                val searchText = etSearch.text.toString().trim()
                if (searchText.isNotEmpty()) {
                    recordSearch(id)
                    inflateSearchResults(role, id, searchText)
                }
                true
            } else {
                false
            }
        }

        var searchJob: Job? = null

        etSearch.addTextChangedListener { text ->
            val query = text.toString().trim()

            searchJob?.cancel()
            searchJob = CoroutineScope(Dispatchers.Main).launch {
                delay(200) // small debounce

                if (query.isEmpty()) {
                    llPastSearches.visibility = View.VISIBLE
                    tvNoResults.visibility = View.GONE
                    searchLayout.visibility = View.GONE
                    currentCall?.cancel() // cancel any ongoing API call
                } else {
                    llPastSearches.visibility = View.GONE
                    inflateSearchResults(role, id, query)
                }
            }
        }



        addClassLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val role = intent.getStringExtra("role") ?: return@registerForActivityResult
                val id = intent.getIntExtra("id", 0)
                inflateClasses(role, id)
            }
        }

        addClassesButton.setOnClickListener {
            val intent = Intent(this@TeacherClassManagerActivity, TeacherAddClassActivity::class.java)
            intent.putExtra("id", id)
            addClassLauncher.launch(intent)
        }

        llPastSearches.removeAllViews()

        inflatePastSearches(id)
        llPastSearches.visibility = View.VISIBLE
    }

    // This handles the Android system back button (at the bottom of the phone)
    override fun onBackPressed() {
        if (searchView.visibility == View.VISIBLE) {
            searchView.visibility = View.GONE
            tvNoResults.visibility = View.GONE
            defaultView.visibility = View.VISIBLE
            etSearch.setText("")
            classAdapter.updateData(mutableListOf())
            inflatePastSearches(id)
        } else {
            // Otherwise, close the page as normal.
            super.onBackPressed()
        }
    }

    fun recordSearch(id: Int){
        val searchTerm = etSearch.text.toString().trim()

        val apiService = ConnectURL.api
        val call = apiService.recordSearch(userId= id, searchTerm= searchTerm)
        call.enqueue(object : retrofit2.Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                val responseString = response.body()?.string() ?: response.errorBody()?.string()

                if (responseString.isNullOrEmpty()) {
                    Log.e("API_ERROR", "Empty response")
                    return
                }

                try {
                    val jsonObj = org.json.JSONObject(responseString)
                    val success = jsonObj.optBoolean("success", false)
                    val message = jsonObj.optString("message", "No message")
                    val dataArray = jsonObj.optJSONArray("data") ?: org.json.JSONArray()

                    Log.e("API_RESPONSE", message)



                } catch (e: Exception) {
                    Log.e("API_ERROR", "Failed to parse response: ${e.localizedMessage}", e)
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("API_ERROR", "Request failed: ${t.localizedMessage}", t)
            }
        })
    }

    fun inflatePastSearches(id: Int) {
        val apiService = ConnectURL.api
        val call = apiService.getSearchHistory(userId = id)

        call.enqueue(object : retrofit2.Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                val responseString = response.body()?.string() ?: response.errorBody()?.string() ?: ""
                if (!responseString.trim().startsWith("{")) {
                    Log.e("API_ERROR", "Invalid JSON: $responseString")
                    return
                }

                llPastSearches.removeAllViews()

                try {
                    val jsonObj = JSONObject(responseString)
                    val data = jsonObj.optJSONArray("data") ?: return

                    for (i in 0 until data.length()) {
                        addPastSearch(data.getString(i) )
                    }

                    llPastSearches.visibility = if (data.length() > 0) View.VISIBLE else View.GONE

                } catch (e: Exception) {
                    Log.e("API_ERROR", "Parse error: ${e.localizedMessage}", e)
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("API_ERROR", "Request failed: ${t.localizedMessage}", t)
            }
        })
    }


    fun addPastSearch(text: String) {
        // Create TextView
        val tv = TextView(this)
        tv.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        tv.setPadding(16, 16, 16, 16)
        tv.text = text
        tv.setTextColor(resources.getColor(R.color.colorSubtleText))
        tv.textSize = 14f

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            tv.typeface = resources.getFont(R.font.lexend)
        }

        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
        tv.setBackgroundResource(typedValue.resourceId)

        //ADD DIVIDER ONLY IF THERE IS ALREADY AT LEAST 1 ITEM
        if (llPastSearches.childCount > 0) {
            val divider = View(this)
            divider.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    1f,
                    resources.displayMetrics
                ).toInt()
            )
            divider.setBackgroundResource(R.drawable.past_search_divider)
            llPastSearches.addView(divider)
        }

        // ADD TEXT
        llPastSearches.addView(tv)

        // MAKE IT CLICKABLE
        tv.setOnClickListener {
            etSearch.setText(text)
            inflateSearchResults(role,id,text)
        }
    }

    fun inflateSearchResults(role: String, id: Int, searchTerm: String) {
        llPastSearches.visibility = View.GONE
        tvNoResults.visibility = View.GONE
        searchLayout.visibility = View.GONE
        classAdapter.updateData(mutableListOf())

        // cancel previous call if it’s still running
        currentCall?.cancel()

        val apiService = ConnectURL.api
        val call = apiService.getClasses(id, role, searchTerm)
        currentCall = call // store reference

        call.enqueue(object : retrofit2.Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                // ignore old responses
                if (call.isCanceled) return

                val responseString = response.body()?.string() ?: response.errorBody()?.string() ?: ""
                if (responseString.isEmpty()) {
                    Log.e("API_ERROR", "Empty response")
                    return
                }

                try {
                    val jsonObj = JSONObject(responseString)
                    val dataArray = jsonObj.optJSONArray("data") ?: return
                    val inputFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

                    val searchResults = mutableListOf<TeacherClass>()
                    for (i in 0 until dataArray.length()) {
                        val item = dataArray.getJSONObject(i)
                        val sectionName = item.optString("class_name", "Unknown Class")
                        val courseId = item.optInt("course_content_id", 0)
                        if (!sectionName.contains(searchTerm, ignoreCase = true)) continue

                        val studentCount = item.optInt("class_size", 0)
                        val arrival = item.optString("arrival_time", "-")
                        val dismiss = item.optString("dismissal_time", "-")

                        val schedule = if (arrival != "-" && dismiss != "-") {
                            "${outputFormat.format(inputFormat.parse(arrival))}-${outputFormat.format(inputFormat.parse(dismiss))}"
                        } else "-"

                        searchResults.add(TeacherClass(sectionName, schedule, studentCount, courseId))
                    }

                    runOnUiThread {
                        if (searchResults.isEmpty()) {
                            tvNoResults.visibility = View.VISIBLE
                            searchLayout.visibility = View.GONE
                        } else {
                            tvNoResults.visibility = View.GONE
                            searchLayout.visibility = View.VISIBLE
                            classAdapter.updateData(searchResults)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("API_ERROR", "Parse error: ${e.localizedMessage}", e)
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                if (call.isCanceled) return // ignore canceled calls
                Log.e("API_ERROR", "Request failed: ${t.localizedMessage}", t)
            }
        })
    }

    fun inflateClasses(role: String, id: Int, searchTerm: String? = null) {
        val apiService = ConnectURL.api
        val call = apiService.getClasses(id, role)

        call.enqueue(object : retrofit2.Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                val responseString = response.body()?.string() ?: response.errorBody()?.string()

                if (responseString.isNullOrEmpty()) {
                    Log.e("API_ERROR", "Empty response")
                    return
                }

                try {
                    val jsonObj = org.json.JSONObject(responseString)
                    val success = jsonObj.optBoolean("success", false)
                    val message = jsonObj.optString("message", "No message")
                    val dataArray = jsonObj.optJSONArray("data") ?: org.json.JSONArray()

                    Log.e("API_RESPONSE", dataArray.toString())

                    parentLayout.removeAllViews()

                    val inputFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

                    var anyMatch = false

                    for (i in 0 until dataArray.length()) {
                        val item = dataArray.getJSONObject(i)
                        val sectionName = item.optString("class_name", "Unknown Class")


                        if (!searchTerm.isNullOrEmpty() && !sectionName.contains(searchTerm, ignoreCase = true)) {
                            continue
                        }

                        anyMatch = true
                        val studentCount = item.optInt("class_size", 0)
                        val arrival = item.optString("arrival_time", "-")
                        val dismiss = item.optString("dismissal_time", "-")
                        val courseId = item.optInt("course_content_id", 0)
                        val schedule = if (arrival != "-" && dismiss != "-") {
                            "${outputFormat.format(inputFormat.parse(arrival))}-${outputFormat.format(inputFormat.parse(dismiss))}"
                        } else "-"

                        val itemView = layoutInflater.inflate(
                            R.layout.item_teacher_class_card,
                            parentLayout,
                            false
                        )

                        itemView.isClickable = true
                        itemView.setOnClickListener {
                            val intent = Intent(this@TeacherClassManagerActivity,
                                TeacherClassPageActivity::class.java)
                            intent.putExtra("id", id)
                            intent.putExtra("role", role)
                            intent.putExtra("courseId", courseId)
                            intent.putExtra("sectionName", sectionName)
                            startActivity(intent)
                        }


                        itemView.findViewById<TextView>(R.id.tvSectionName).text = sectionName
                        itemView.findViewById<TextView>(R.id.tvSchedule).text = schedule
                        itemView.findViewById<TextView>(R.id.tvStudentCount).text = "$studentCount students"

                        parentLayout.addView(itemView)
                    }

                    tvNoResults.visibility = if (!anyMatch) View.VISIBLE else View.GONE

                } catch (e: Exception) {
                    Log.e("API_ERROR", "Failed to parse response: ${e.localizedMessage}", e)
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("API_ERROR", "Request failed: ${t.localizedMessage}", t)
            }
        })
    }


}