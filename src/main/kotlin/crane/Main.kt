package crane

import api.*
import codegeneration.*
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

        val body: JavaGeneratedMethod.() -> Unit = {
            if (bodyNeeded) generateMethodBody(newClass, method)
            if (newMethods.none { it.name == method.name }) addComment("TODO")
        }

        val visibility = Visibility.Public
        val parameters = method.parameterNames.zip(method.descriptor.parameterDescriptors)
            .map { (name, desc) -> name to desc }
        if (method.isConstructor) {
            addConstructor(visibility, parameters, body)
        } else {
            addMethod(
                name = method.name,
                static = method.static,
                final = false,
                abstract = !bodyNeeded,
                visibility = visibility,
                returnType = if (method.isConstructor) null else method.descriptor.returnDescriptor,
                parameters = parameters,
                body = body
            )
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
                Expression.Value.Field(
                    owner = Expression.Class(ObjectType.dotQualified(newClass.fullyQualifiedName)),
                    name = field.name
                )
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
        val newFunctionName = oldMethod.name
        val returnCall = oldMethod.descriptor.returnDescriptor != ReturnDescriptor.Void

        val newClassType = ObjectType.dotQualified(newClass.fullyQualifiedName)
        val receiver = when {
            oldMethod.static -> Expression.Class(newClassType)
            newClass.isBaseclass -> Expression.Super
            else -> Expression.Value.This.castTo(newClassType)
        }

        val parameters = oldMethod.parameterNames.map { Expression.Value.Variable(it) }
        if (oldMethod.isConstructor) {
            addSelfConstructorCall(SelfConstructorType.Super, parameters)
        } else {
            addFunctionCall(receiver, newFunctionName, parameters, returnCall)
        }
    }

}

