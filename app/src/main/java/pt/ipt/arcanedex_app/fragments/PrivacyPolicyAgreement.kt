package pt.ipt.arcanedex_app.fragments

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import pt.ipt.arcanedex_app.R
import pt.ipt.arcanedex_app.activities.MainActivity
import pt.ipt.arcanedex_app.data.utils.SharedPreferencesHelper

class PrivacyPolicyAgreement : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy_policy_agreement)

        val acceptButton = findViewById<Button>(R.id.acceptButton)
        acceptButton.setOnClickListener {
            // Mark terms as accepted
            SharedPreferencesHelper.setHasAcceptedTerms(this, true)

            // Redirect to MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Close PrivacyPolicyAgreement
        }
    }
}
