package com.example.gr8math.Activity

import android.os.Bundle
import android.text.Html
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.gr8math.R
import com.google.android.material.appbar.MaterialToolbar

class TermsAndConditionsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terms_and_conditions)

        // 1. Setup Toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        // 2. Setup Text Content
        val tvContent = findViewById<TextView>(R.id.tvContent)

        // We use Html.fromHtml to render bold text and paragraphs
        tvContent.text = Html.fromHtml(termsText, Html.FROM_HTML_MODE_COMPACT)
    }

    // The Formatted Text Content
    private val termsText = """
        <b>TERMS AND CONDITIONS OF SERVICE</b><br>
        <b>Gr8 Math</b><br>
        Last Updated: <b>Dec 2, 2025</b><br><br>

        <b>Important Notice:</b> This General Terms and Conditions document (referred to herein as "T&Cs" or "Terms") is a legal agreement between you ("Client" or "User") and Color Rush Studios, operating the learning management system/service <b>Gr8 Math</b> (the "Company" or "We").<br><br>
        
        By <b>checking the box</b> and proceeding with the registration or login, or otherwise accessing or using the Service, you confirm that you have read, understood, and <b>irrevocably agreed</b> to be bound by these T&Cs and the separate Privacy Policy.<br><br>

        <b>1. Definitions and Scope</b><br><br>
        
        <b>1.1. Introduction and Scope</b><br>
        These T&Cs set out the general terms and conditions applicable to your use of the <b>Gr8 Math</b> mobile application, and all related services, content, and materials (the "Service").<br><br>

        <b>1.2. Key Definitions</b><br>
        For clarity, capitalized terms used in this document have the following meanings:<br><br>
        • <b>"Applicable Laws"</b> shall mean all laws and regulations in the <b>Philippines</b> that are applicable to the transactions between the Client and the Company.<br><br>
        • <b>"Electronic Signatures"</b> refer to the required electronic signature, One-Time Password ("OTP"), and other identification factors used for verification.<br><br>
        • <b>"Personal Data"</b> refers to all types of personal information and sensitive personal information collected and processed.<br><br>
        • <b>"User Content"</b> means any content, including comments, messages, assignments, or images, contributed by the Client to the Service.<br><br>
        • <b>"Minor User"</b> refers to any individual using the Service who is <b>below eighteen (18) years of age</b> and, as such, is not yet of legal age to enter into a binding contract under the laws of the Republic of the Philippines.<br><br>
        • <b>"Parent/Guardian"</b> refers to a legally authorized adult who provides <b>verifiable consent</b> for a Minor User to access and use the Service and assumes responsibility for the Minor User’s compliance with these T&Cs.<br><br>

        <b>2. Access and Use of the Service</b><br><br>

        <b>2.1. Eligibility and Parental Consent</b><br>
        The Service is intended for use by individuals who are <b>18 years of age or older</b> ("Adult User").<br><br>
        
        <b>If you are below 18 years of age (a "Minor User")</b>, you may only use the Service if: a) You have the express permission and supervision of a parent or legal guardian; AND b) Your parent or legal guardian <b>reads, understands, and agrees</b> to these T&Cs on your behalf. <b>Your use of the Service is deemed ratification by your parent or legal guardian of these Terms and Conditions.</b><br><br>

        <b>2.2. Account Creation and Maintenance</b><br>
        To use the Service, you must create an account ("Account"). You must provide accurate and current information, including your email and mobile number.<br><br>
        1. <b>Client Responsibility:</b> You are responsible for maintaining the confidentiality of your credentials (username, passwords, OTPs) and are solely liable for all activities that occur under your Account. You agree to hold the Company harmless from any losses, damages, or claims that may result from the wrongful use of the Electronic Signatures, provided the Company is not at fault or negligent.<br><br>
        2. <b>Notification of Changes:</b> You must promptly notify the Company of any material change affecting your registered email or mobile number.<br><br>

        <b>2.3. User Conduct and Content Standards</b><br>
        You agree to use the Service strictly for <b>personal, non-commercial, educational purposes</b> as intended. You agree <b>not</b> to use the Service to post or transmit content that is illegal, defamatory, obscene, abusive, invasive of privacy, or <b>infringes on someone else's intellectual property rights</b>. The Company reserves the right <b>to remove any User Content</b> that violates these T&Cs at its sole discretion, which may also result in suspending or terminating the user's account.<br><br>

        <b>3. Intellectual Property (IP) Rights and User Content</b><br><br>

        <b>3.1. Company Ownership</b><br>
        The Company owns all content and materials on the Service, including the courseware, code, graphics, design, software, copyrights, trademarks, and patents. IP protection is governed primarily by the <b>Intellectual Property Code of the Philippines (Republic Act No. 8293)</b>.<br><br>

        <b>3.2. User License Grant</b><br>
        The Company grants the Client a <b>limited, non-exclusive, non-transferable, and revocable license</b> to use the Service and view the content for personal, generally non-commercial purposes. You are <b>prohibited</b> from copying, reproducing, modifying, distributing, or creating derivative works based on the Company's content without explicit permission.<br><br>

        <b>3.3. User Content License</b><br>
        If you contribute User Content, you acknowledge that:<br>
        1. <b>Responsibility:</b> You are responsible for the content you post, including any legal consequences arising from claims of defamation or infringement.<br>
        2. <b>License to Company:</b> You grant the Company a non-exclusive, royalty-free, sublicensable, and transferable license to use, display, reproduce, distribute, and exploit that User Content for the purpose of operating, promoting, and improving the Services.<br><br>

        <b>4. Privacy, Data Protection, and Communication</b><br><br>

        <b>4.1. Privacy Policy Reference</b><br>
        The Client (or the Parent/Guardian, in the case of a Minor User) explicitly <b>consents to</b> and confirms having read and understood the Company's separate <b>Privacy Notice/Policy</b>, available at PRIVACY POLICY. This policy ensures compliance with the <b>Philippines' Republic Act (R.A.) 10173 (Data Privacy Act of 2012)</b>.<br><br>
        
        <b>For Minor Users (under 18):</b> The Company requires the <b>verifiable consent of the Parent/Guardian to process the Minor User’s personal data</b>, in compliance with R.A. 10173.<br><br>

        <b>4.2. Electronic Communications</b><br>
        The Client agrees that the Company may send communications, confirmations, and notices regarding the Service or changes to these T&Cs via "Alternative Communication Channels," including the Client's registered email or mobile number, website, or mobile application.<br><br>

        <b>4.3. Record of Transactions and Communications</b><br>
        The Client authorizes the Company to record and store transaction details, system logs, and any communications related to the Service. The Company may use such records and logs as conclusive evidence of the Client's acceptance of terms, transactions, and for use in any judicial, administrative, or arbitration proceeding.<br><br>

        <b>5. Electronic Signatures and Validity</b><br><br>

        <b>5.1. Binding Effect of Electronic Signatures</b><br>
        The Client irrevocably and unconditionally accepts to be bound by all relevant contracts and documents upon the Client's submission of the valid and applicable OTP or other required identification factors (Electronic Signatures).<br><br>

        <b>5.2. Legal Recognition</b><br>
        The Client hereby declares their intention to use the Electronic Signatures as their valid and binding electronic signatures as allowed by Philippines law (R.A. No. 8792, the Electronic Commerce Act of 2000). The Electronic Signatures shall bind the Client as though the same were duly signed in person with wet ink.<br><br>

        <b>5.3. Prohibition to Contest</b><br>
        The Client shall not contest the validity or enforceability of any Communications, contracts, and transactions on the ground that they were electronically signed via the Electronic Signatures.<br><br>

        <b>6. Disclaimers, Liability, and Indemnification</b><br><br>

        <b>6.1. No Warranty</b><br>
        The Service is provided <b>"as is"</b>. The Company makes <b>no warranties</b> or representations about the quality, reliability, availability, or functionality of the Service. The user uses the Service at their own risk.<br><br>

        <b>6.2. Limitation of Liability</b><br>
        The Company restricts the types and amounts of damages that a user can claim from the service provider. However, the Client acknowledges that a <b>waiver or limitation of liability for fraud and gross negligence is void under Philippine law</b>.<br><br>

        <b>6.3. Indemnification</b><br>
        The Client agrees to protect and hold the Company harmless against any claims, damages, losses, or expenses that arise from the Client's actions or violation of these T&Cs.<br><br>

        <b>7. Dispute Resolution and Governing Law</b><br><br>

        <b>7.1. Governing Law</b><br>
        The Applicable Agreement, Specific T&Cs, General T&Cs, and other documents shall be governed by the laws of the <b>Republic of the Philippines</b>.<br><br>

        <b>7.2. Exclusive Venue (Litigation)</b><br>
        Any controversy or dispute that may arise between the Company and the Client shall be brought exclusively in the proper courts of the Philippines which have jurisdiction over the <b>Company's registered principal place of business</b> (i.e., the proper courts of Taguig City), to the exclusion of all other courts.<br><br>

        <b>7.3. Alternative Dispute Resolution (ADR)</b><br>
        Any dispute, controversy, or claim arising out of or relating to these T&Cs shall be settled by <b>binding arbitration</b>. The arbitration agreement should state the number of arbitrators, the designated independent third party who shall appoint them, the procedure for appointment, and the period within which the arbitrator/s should be appointed. If the parties fail to agree on the place of arbitration, the venue shall be <b>Metro Manila</b>.<br><br>

        <b>8. Miscellaneous Provisions</b><br><br>

        <b>8.1. Updates and Amendments</b><br>
        The Company reserves the right to update or amend these General T&Cs at any time. Such amendments shall <b>bind the Client from the date such changes are published</b> through the Company's website or via Alternative Communication Channels.<br><br>

        <b>8.2. Severability</b><br>
        If any provision of these T&Cs is held to be invalid, illegal, or unenforceable, the invalidity shall not affect any other provisions, which shall be reformed, construed, and enforced to the fullest extent possible.<br><br>

        <b>8.3. Confirmation</b><br>
        The Client confirms having read and understood the entire Applicable Agreement, Specific T&Cs, and General T&Cs, and that the same have been <b>explained to the Client in the language understood by the Client</b>.
    """
}