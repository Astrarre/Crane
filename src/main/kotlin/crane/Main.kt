package crane

import api.*
import codegeneration.JavaCodeGenerator
import codegeneration.JavaGeneratedClass
import codegeneration.JavaGeneratedMethod
import codegeneration.Visibility
import descriptor.ObjectType
import descriptor.ReturnDescriptor
import exists
import isDirectory
import java.nio.file.Path

fun buildBridge(old: Path, new: Path, dest: Path, newVersion: String) {
    require(dest.parent.exists()) { "The chosen destination path '$dest' is not in any existing directory." }
    require(dest.parent.isDirectory()) { "The parent of the chosen destination path '$dest' is not a directory." }

    dest.toFile().deleteRecursively()

    val newClassesByName = ClassApi.readFromJar(new).map { it.fullyQualifiedName to it }.toMap()
    for (oldClass in ClassApi.readFromJar(old)) {
        val newClassName = "$newVersion." + oldClass.fullyQualifiedName.substringAfter(".")
        val newClass = newClassesByName[newClassName]

        buildApiBridge(oldClass, newClass, dest)
    }

}

fun main() {
    val x = 2
}

val ClassApi.isBaseclass get() = type == ClassApi.Type.AbstractClass

private fun buildApiBridge(old: ClassApi, new: ClassApi?, destination: Path) {
    assert(new == null || old.type == new.type)
    JavaCodeGenerator.writeClass(
        old.packageName,
        old.className,
        destination,
        isInterface = old.isInterface,
        isAbstract = old.isBaseclass
    ) {
        buildBridgeClass(old, new)
    }
}

private fun JavaGeneratedClass.buildBridgeClass(oldClass: ClassApi, newClass: ClassApi?) {
    if (oldClass.isBaseclass && newClass != null) {
        setSuperclass(ObjectType.dotQualified(newClass.fullyQualifiedName))
    }

    for (method in oldClass.methods) {
        if (!method.isPublicApi) continue
        val newMethods = newClass?.methods ?: listOf<ClassApi.Method>()
        val bodyNeeded = method.static || method !in newMethods || method.isConstructor
        if (oldClass.isBaseclass && !bodyNeeded) continue
        addMethod(
            name = method.name,
            static = method.static,
            final = false,
            abstract = !bodyNeeded,
            visibility = Visibility.Public,
            returnType = if (method.isConstructor) null else method.descriptor.returnDescriptor,
            parameters = method.parameterNames.zip(method.descriptor.parameterDescriptors)
                .map { (name, desc) -> name to desc }
        ) {
            if (bodyNeeded) generateMethodBody(newClass, method)
            if (newMethods.none { it.name == method.name }) addComment("TODO")
        }
    }


    for (field in oldClass.fields) {
        if (!field.isPublicApi) continue

        addField(
            name = field.name,
            static = field.static,
            visibility = Visibility.Public,
            final = true,
            type = field.descriptor
        ) {
            if (newClass != null) setInitializer(
                "\$T.${field.name}",
                ObjectType.dotQualified(newClass.fullyQualifiedName)
            )
        }

    }
}

private fun JavaGeneratedMethod.generateMethodBody(
    newClass: ClassApi?,
    oldMethod: ClassApi.Method
) {
    val newMethod = newClass?.methods?.find { it.name == oldMethod.name }
    if (newMethod != null) {
        val newFunctionName = if (oldMethod.isConstructor) "" else oldMethod.name
        val call = "$newFunctionName(${oldMethod.parameterNames.joinToString(", ")})"
        val returnCall = if (oldMethod.descriptor.returnDescriptor == ReturnDescriptor.Void) ""
        else "return"

        val receiver = when {
            oldMethod.static -> "\$T."
            oldMethod.isConstructor -> "super"
            newClass.isBaseclass -> "super."
            else -> "((\$T)this)."
        }
        addStatement("$returnCall $receiver$call", ObjectType.dotQualified(newClass.fullyQualifiedName))
    }

}

