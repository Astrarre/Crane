package crane

import api.*
import asm.isInterface
import asm.isStatic
import asm.visibility
import codegeneration.JavaCodeGenerator
import codegeneration.JavaGeneratedClass
import codegeneration.JavaGeneratedMethod
import codegeneration.Visibility
import descriptor.*
import exists
import isDirectory
import openJar
import org.objectweb.asm.tree.MethodNode
import readToClassNode
import walk
import java.nio.file.Path
import java.nio.file.Paths

fun buildBridge(old: Path, new: Path, dest: Path, newVersion: String) {
    require(dest.parent.exists()) { "The chosen destination path '$dest' is not in any existing directory." }
    require(dest.parent.isDirectory()) { "The parent of the chosen destination path '$dest' is not a directory." }

    dest.toFile().deleteRecursively()

    old.openJar { oldFs ->
        new.openJar { newFs ->
            oldFs.getPath("/").walk { oldPath ->
                if (oldPath.toString() == "/") return@walk

                if (oldPath.toString().endsWith(".class")) {
                    // Get the path of the same class in the new api version
                    val matchingPath = Paths.get(newVersion).resolve(
                        oldPath.toString().removePrefix("/").let { it.substring(it.indexOf("/") + 1) }
                    )
                    val newPath = newFs.getPath(matchingPath.toString())
                    assert(oldPath.exists())
                    val oldApi = ApiClass.readFrom(oldPath)
                    val newApi = if (newPath.exists()) ApiClass.readFrom(newPath) else null
                    buildApiBridge(oldApi, newApi, destination = dest)
                }


            }
        }
    }

}

fun main() {
    val x = 2
}

private val MethodNode.paramsSafe get() = parameters ?: listOf()



private fun buildApiBridge(old: ApiClass, new: ApiClass?, destination: Path) {
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

private fun JavaGeneratedClass.buildBridgeClass(oldClass: ApiClass, newClass: ApiClass?) {
    if (oldClass.isBaseclass && newClass != null) {
        setSuperclass(ObjectType.dotQualified(newClass.fullyQualifiedName))
    }

    for (method in oldClass.methods) {
        if (!method.isPublicApi) continue
        val newMethods = newClass?.methods ?: listOf<ApiClass.Method>()
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
    newClass: ApiClass?,
    oldMethod: ApiClass.Method
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

