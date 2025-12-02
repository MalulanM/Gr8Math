package com.example.gr8math

import android.os.Bundle
import android.text.Html
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.appbar.MaterialToolbar

class PrivacyPolicyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy_policy)

        // 1. Setup Toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        // 2. Populate Text Sections
        // Use "findViewById" to safely get the views
        val tvPart1 = findViewById<TextView>(R.id.tvPart1)
        val tvPart2 = findViewById<TextView>(R.id.tvPart2)
        val tvPart3 = findViewById<TextView>(R.id.tvPart3)

        tvPart1.text = Html.fromHtml(textPart1, Html.FROM_HTML_MODE_COMPACT)
        tvPart2.text = Html.fromHtml(textPart2, Html.FROM_HTML_MODE_COMPACT)
        tvPart3.text = Html.fromHtml(textPart3, Html.FROM_HTML_MODE_COMPACT)

        // 3. Build Table 1 (Identity)
        val table1 = findViewById<TableLayout>(R.id.tableIdentity)
        if (table1 != null) {
            addHeaderRow(table1, listOf("Information", "Details"))
            addDataRow(table1, "Personal Information Controller (PIC) / Organization", "Color Rush Studios")
            addDataRow(table1, "Business Address", "Color Rush Building, 17th Ave., Fort Bonifacio, Taguig City")
            addDataRow(table1, "Contact Number / Official Email", "09********* / main@colorrush.com")
            addDataRow(table1, "Data Protection Officer (DPO)", "Name: Hannah Mae Reyes\nContact Details: dpo@colorrush.com")
        }

        // 4. Build Table 2 (Purposes)
        val table2 = findViewById<TableLayout>(R.id.tablePurposes)
        if (table2 != null) {
            addHeaderRow(table2, listOf("Purpose of Processing", "Description/Scope of Processing", "Legal Basis (DPA Section)"))
            addDataRow(table2, "Service Provision / Contract Fulfillment", "To register your account, manage access, process transactions, and provide the core functions (LMS/courseware) of Gr8 Math", "Contractual Obligation (Sec. 21(b)) or Consent (Sec. 21(a))")
            addDataRow(table2, "Product Improvement / Data Analytics", "To monitor usage, conduct statistical research, analyze trends, and improve the user experience.", "Legitimate Interest (Sec. 21(g)) or Consent (Sec. 21(a))")
            addDataRow(table2, "Direct Marketing", "To send promotional materials, special offers, and tailored advertisements.", "Specific and Informed Consent (Sec. 21(a))")
            addDataRow(table2, "Compliance and Security", "To respond to legal obligations, detect fraud, enforce our Terms and Conditions, and manage personal data breaches.", "Legal Obligation (Sec. 21(c)) or Public Safety (Sec. 21(e))")
        }
    }

    // --- Helper Functions for Dynamic Tables ---

    private fun addHeaderRow(table: TableLayout, headers: List<String>) {
        val row = TableRow(this)
        headers.forEach { text ->
            val tv = createCell(text, isHeader = true)
            row.addView(tv)
        }
        table.addView(row)
    }

    private fun addDataRow(table: TableLayout, vararg values: String) {
        val row = TableRow(this)
        values.forEach { text ->
            val tv = createCell(text, isHeader = false)
            row.addView(tv)
        }
        table.addView(row)
    }

    private fun createCell(text: String, isHeader: Boolean): TextView {
        val tv = TextView(this)
        tv.text = text
        tv.setPadding(16, 16, 16, 16)
        tv.setTextColor(ContextCompat.getColor(this, R.color.colorText))

        // Use the border drawable
        tv.setBackgroundResource(R.drawable.bg_privacy_table_border)

        try {
            val typeface = ResourcesCompat.getFont(this, R.font.lexend)
            tv.typeface = typeface
            if (isHeader) {
                tv.setTypeface(typeface, android.graphics.Typeface.BOLD)
                tv.setBackgroundColor(ContextCompat.getColor(this, R.color.colorAccent)) // Gray header bg
            }
        } catch (e: Exception) {}

        // Layout params to make cells stretch properly
        val params = TableRow.LayoutParams(0, TableRow.LayoutParams.MATCH_PARENT, 1f)
        tv.layoutParams = params

        return tv
    }

    // --- HTML STRINGS (FULL CONTENT) ---

    private val textPart1 = """
        <b>Gr8 Math Privacy Policy</b><br>
        Effective Date: <b>Dec. 2, 2025</b><br><br>

        This Privacy Policy explains how <b>Color Rush Studios</b>, acting as the Personal Information Controller (PIC), collects, uses, protects, and handles your Personal Data in compliance with the <b>Republic Act No. 10173</b>, otherwise known as the Data Privacy Act of 2012 (DPA), and its Implementing Rules and Regulations (IRR).<br><br>
        
        <b>Note on Format (Layered Notice Requirement):</b> In compliance with NPC guidelines, we adopt the layered privacy notice approach, providing key information upfront and directing you to this comprehensive notice. This ensures you are informed at the exact point of data processing (Just-in-Time Notice).<br><br>

        <b>I. Identity of the Personal Information Controller (PIC) and Contact Details</b><br>
    """

    private val textPart2 = """
        <br><b>II. Personal Data Collected</b><br>
        We collect Personal Data that is adequate, relevant, suitable, necessary, and not excessive in relation to our declared purpose, upholding the principle of <b>Proportionality</b>.<br><br>
        You must specify the data collected by <b>Gr8 Math</b>:<br>
        1. <b>Identity Data:</b> Full Name, Date of Birth, Gender, Learner’s Reference Number.<br>
        2. <b>Contact Data:</b> Email Address, Phone Number.<br>
        3. <b>Technical Data/Usage Data:</b> IP address, device type, operating system, usage logs, browser type.<br>
        4. <b>User Content:</b> Assignments, quizzes, daily lesson logs, lessons, and other similar educational materials submitted by Clients for the purpose of instruction, assessment, and record-keeping on the Gr8 Math platform.<br><br>
        
        <b>Minor User Data:</b> For users below 18, we require the <b>verifiable consent of the Parent/Guardian</b> to process the Minor User’s personal data, in compliance with R.A. 10173.<br><br>

        <b>III. Purposes and Legal Basis for Processing</b><br>
    """

    private val textPart3 = """
        <br><b>IV. Data Subject Rights and Consent Mechanisms</b><br><br>

        <b>A. Data Subject Rights (Rule VIII)</b><br>
        As a Data Subject, you are entitled to exercise your rights under the DPA:<br>
        1. <b>Right to be Informed:</b> You have the right to be informed whether your personal data is being processed, including profiling and automated decision-making. We must furnish you with all information contained in this policy (identity of PIC, purposes, recipients, retention period, etc.).<br>
        2. <b>Right to Object:</b> You have the right to object to the processing of your personal data, including processing for direct marketing, automated processing, or profiling. When you object, we shall stop processing the data unless the processing is pursuant to a subpoena, necessary for a contract/service, or a legal obligation.<br>
        3. <b>Right to Access:</b> You have the right to reasonable access, upon demand, to the contents of your processed data, the sources from which the data was obtained, the recipients, the manner of processing, and information regarding automated processes.<br>
        4. <b>Right to Rectification/Correction:</b> You have the right to dispute the inaccuracy or error in your personal data and have the PIC correct it immediately.<br>
        5. <b>Right to Erasure or Blocking:</b> You have the right to suspend, withdraw, or order the blocking, removal, or destruction of your personal data when, for example, the data is incomplete, unlawfully obtained, or no longer necessary for the purpose of collection, or you withdraw consent and there is no other legal basis for processing.<br>
        6. <b>Right to Data Portability:</b> Where your data is processed by electronic means and in a structured format, you have the right to obtain a copy of such data in a format that allows for further use.<br>
        7. <b>Right to Damages:</b> You shall be indemnified for damages sustained due to inaccurate, incomplete, or unauthorized use of personal data.<br><br>

        <b>B. Mechanism for Obtaining Valid Consent</b><br>
        Consent is a specific, informed indication of will and must be evidenced by written, electronic, or recorded means.<br>
        • <b>No Implied Consent:</b> Implied or inferred consent is generally prohibited.<br>
        • <b>Clear Assenting Action:</b> Consent must be given through a clear assenting action, such as clicking a dedicated button. Silence or pre-ticked boxes do not constitute consent.<br>
        • <b>Granularity:</b> If the data is processed for multiple unrelated purposes (e.g., service provision, and separately, for direct marketing), consent must be given specifically for each purpose.<br><br>

        <b>C. Withdrawal of Consent</b><br>
        Consent can be withdrawn at any time.<br>
        • <b>Ease of Withdrawal:</b> Withdrawing consent must be as easy as, if not easier than, giving consent.<br>
        • <b>Interface:</b> If a service-specific user interface (like a log-in account) was used to obtain consent, that same interface should be used for withdrawing consent.<br>
        • <b>Consequences:</b> Upon withdrawal, we are obliged to implement procedures to suspend, withdraw, or order the blocking, removal, or destruction of your personal data from our systems.<br><br>

        <b>VI. Data Retention and Disposal</b><br>
        You must adhere to the principle that Personal Data shall not be retained longer than necessary.<br>
        1. <b>Retention Period:</b> Personal Data is retained only for the period necessary to fulfill the purpose for which it was collected, or as required by law. Specifically:<br>
           a. <b>Active Account Data:</b> Retained for the duration the user maintains an active <b>Gr8 Math</b> account.<br>
           b. <b>Post-Closure Data:</b> Key Identity and User Content data will be archived for an <b>audit period of three (3) years</b> following account closure, after which it will be securely disposed of.<br>
           c. <b>Consent and Transaction Records:</b> Retained for <b>ten (10) years</b> after the last activity, to comply with Philippine statutes of limitations for contractual claims.<br>
        2. <b>Disposal:</b> Upon termination of the processing, personal data will be disposed of or discarded in a <b>secure manner</b> that prevents further processing, unauthorized access, or disclosure.<br><br>

        <b>VII. Security Measures and Breach Notification</b><br>
        We implement reasonable and appropriate organizational, physical, and technical security measures to maintain the availability, integrity, and confidentiality of your personal data against accidental or unlawful destruction, alteration, and disclosure.<br>
        Technical measures include:<br>
        • <b>Encryption</b> of personal data during storage and while in transit.<br>
        • <b>Authentication</b> processes.<br>
        • <b>Regular Monitoring</b> for security breaches and testing/evaluation of security effectiveness.<br><br>
        <b>Personal Data Breach Notification:</b> In the event of a personal data breach, we shall notify the National Privacy Commission and the affected Data Subjects within <b>seventy-two (72) hours</b> upon knowledge that a breach requiring notification has occurred. Notification is required if sensitive personal information or information enabling identity fraud is acquired by an unauthorized person, and this acquisition is likely to give rise to a real risk of serious harm to you.<br><br>

        <b>VIII. Updates to this Policy</b><br>
        We may update this Privacy Policy from time to time. Any changes will be effective immediately upon posting the revised Policy. We will notify you of any substantial changes, and if the processing purpose changes, you will be given the opportunity to withhold consent.
    """
}