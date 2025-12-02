import java.io.Serializable

// In a new file, e.g., DllApiModels.kt

// For Step 2 data
data class DllReference(
    val date: String,
    val reference_title: String,
    val reference_text: String
)

// For Step 3 data
data class DllProcedure(
    val date: String,
    val review: String,
    val purpose: String,
    val example: String,
    val discussion_proper: String,
    val developing_mastery: String,
    val application: String,
    val generalization: String,
    val evaluation: String,
    val additional_act: String
)

// For Step 4 data (Note: `etReview` in Kotlin maps to `remark` in PHP)
data class DllReflection(
    val date: String,
    val remark: String, // Matches the 'remark' field in your PHP code
    val reflection: String
)

// The final complete API request body
data class CreateDllRequest(
    // ⚠️ IMPORTANT: Get this from the user's login/selected class
    val course_id: Int,

    // Step 1 data
    val quarter_number: String,
    val week_number: String,
    val available_from: String,
    val available_until: String,
    val content_standard: String,
    val performance_standard: String,
    val learning_comp: String,

    // Nested lists (Step 2, 3, 4 data)
    val dll_reference: List<DllReference>,
    val dll_procedure: List<DllProcedure>,
    val dll_reflection: List<DllReflection>
)


///for displaying dll///
// In a file like DllResponseModels.kt

// Data for dll_main
data class DllMain(
    val id: Int,
    val quarter_number: String,
    val week_number: String,
    val available_from: String,
    val available_until: String,
    val content_standard: String,
    val performance_standard: String,
    val learning_comp: String,
    // Add other fields as needed
) : java.io.Serializable

// Data for dll_reference
data class DllReferenceDisplay(
    val id: Int,
    val date: String,
    val reference_title: String,
    val reference_text: String
) : Serializable

// Data for dll_procedure (Mapping to procedures)
data class DllProcedureDisplay(
    val id: Int,
    val date: String,
    val review: String,
    val purpose: String,
    val example: String,
    val discussion_proper: String,
    val developing_mastery: String,
    val application: String,
    val generalization: String,
    val evaluation: String,
    val additional_act: String
) : Serializable

// Data for dll_reflection
data class DllReflectionDisplay(
    val id: Int,
    val date: String,
    val remark: String,
    val reflection: String
) : Serializable

// The complete API response structure
data class DllDisplayResponse(
    val main: DllMain,
    val references: List<DllReferenceDisplay>,
    val procedures: List<DllProcedureDisplay>,
    val reflections: List<DllReflectionDisplay>
)

data class CompleteDllEntry(
    val main: DllMain, // (Assuming DllMain is already defined for main table fields)
    val references: List<DllReferenceDisplay>,
    val procedures: List<DllProcedureDisplay>,
    val reflections: List<DllReflectionDisplay>
) : java.io.Serializable

// Model for the new API list response
data class DllListResponse(
    val success: Boolean,
    val dll: List<CompleteDllEntry> // This holds all DLLs for the course
) : java.io.Serializable