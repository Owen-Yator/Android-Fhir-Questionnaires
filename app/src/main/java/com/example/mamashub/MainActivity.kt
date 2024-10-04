package com.example.quetion

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.example.mamashub.R
import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Questionnaire

class MainActivity : AppCompatActivity() {

    var questionnaireJsonString: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        // Load the questionnaire JSON from assets
        questionnaireJsonString = getStringFromAssets("questionnaire.json")

        // If there is no saved instance state, commit the fragment to the container
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                val fragment = QuestionnaireFragment.builder()
                    .setQuestionnaire(questionnaireJsonString!!)
                    .build()
                fragment.setHasOptionsMenu(false) // Ensure the fragment does not handle the menu
                add(R.id.fragment_container_view, fragment)
            }
        }
    }

    // Inflate the menu with the submit button
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.submit_menu, menu)
        Log.d("MainActivity", "Menu created") // Log to confirm menu creation
        return true
    }

    // Handle menu item clicks
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.submit -> {
                // Call the submit function when the submit button is clicked
                submitQuestionnaire()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Function to submit the questionnaire and extract FHIR resources
    private fun submitQuestionnaire() =
        lifecycleScope.launch {
            try {
                // Step 1: Get the QuestionnaireFragment
                val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container_view)
                        as QuestionnaireFragment

                // Step 2: Get the questionnaire response
                val questionnaireResponse = fragment.getQuestionnaireResponse()

                // Step 3: Log the questionnaire response in JSON format
                val jsonParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
                val questionnaireResponseString =
                    jsonParser.encodeResourceToString(questionnaireResponse)
                Log.d("response", questionnaireResponseString)

                // Step 4: Parse the questionnaire JSON string into a Questionnaire object
                val questionnaire = jsonParser.parseResource(questionnaireJsonString) as Questionnaire

                // Step 5: Use ResourceMapper to extract FHIR resources from the Questionnaire and QuestionnaireResponse
                val bundle = ResourceMapper.extract(questionnaire, questionnaireResponse)

                // Step 6: Log the extraction result in JSON format
                Log.d("extraction result", jsonParser.encodeResourceToString(bundle))

            } catch (e: Exception) {
                Log.e("MainActivity", "Error during FHIR resource extraction", e)
            }
        }

    // Function to load the questionnaire JSON from the assets folder
    private fun getStringFromAssets(fileName: String): String {
        return assets.open(fileName).bufferedReader().use { it.readText() }
    }
}