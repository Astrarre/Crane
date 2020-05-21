package crane

import crane.codegeneration.Visibility
import crane.descriptor.Descriptor
import crane.descriptor.FieldDescriptor
import crane.descriptor.MethodDescriptor


/**
 * ApiClass use dot.separated.format for the packageName always!
 */
data class ApiClass(
    val packageName: String,
    val className: String,
    val type: Type,
    val methods: Set<Method>,
    val fields: Set<Field>,
    val innerClasses: Set<ApiClass>
) {
    companion object {
//        val None = ApiClass("", "", Type.Interface, setOf(), setOf(), setOf())
    }

    val fullyQualifiedName get() = "$packageName.$className"

    enum class Type {
        Interface,
        Baseclass
    }

    val isInterface get() = type == Type.Interface
    val isBaseclass get() = type == Type.Baseclass

    abstract class Member {
        abstract val name: String
        abstract val descriptor: Descriptor
        abstract val static: Boolean
        abstract val visibility: Visibility

        val isPublicApi get() = visibility == Visibility.Public || visibility == Visibility.Protected
    }


    data class Method(
        override val name: String,
        override val descriptor: MethodDescriptor,
        val parameterNames: List<String>,
        override val visibility: Visibility,
        override val static: Boolean
    ) : Member() {
        override fun toString() = "${if (static) "static " else ""}$name$descriptor"

        val isConstructor get() = name == "<init>"
    }

    data class Field(
        override val name: String,
        override val descriptor: FieldDescriptor,
        override val static: Boolean,
        override val visibility: Visibility
    ) : Member() {
        override fun toString() = "${if (static) "static " else ""}$name: $descriptor"
    }

    override fun toString(): String {
        return "Class {\nmethods: [\n" + methods.joinToString("\n") { "\t$it" } +
                "\n]\nfields: [\n" + fields.joinToString("\n") { "\t$it" } + "\n]\n}"
    }
}

