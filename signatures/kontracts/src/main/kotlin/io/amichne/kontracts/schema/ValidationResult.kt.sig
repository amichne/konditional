file=kontracts/src/main/kotlin/io/amichne/kontracts/schema/ValidationResult.kt
package=io.amichne.kontracts.schema
type=io.amichne.kontracts.schema.ValidationResult|kind=class|decl=sealed class ValidationResult
type=io.amichne.kontracts.schema.Valid|kind=object|decl=object Valid : ValidationResult()
type=io.amichne.kontracts.schema.Invalid|kind=class|decl=data class Invalid(val message: String) : ValidationResult()
fields:
- val isValid: Boolean get()
- val isInvalid: Boolean get()
methods:
- fun getErrorMessage(): String?
