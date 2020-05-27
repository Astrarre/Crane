package crane

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
                    val oldApi = parseApi(oldPath)
                    val newApi = if (newPath.exists()) parseApi(newPath) else null
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

private fun parseApi(classPath: Path): ApiClass {
    val classNode = readToClassNode(classPath)
    val methods = classNode.methods.map { method ->
        val descriptor = MethodDescriptor.read(method.desc)
        val nonThisLocals = method.localVariables.filter { it.name != "this" }
        check(nonThisLocals.size >= descriptor.parameterDescriptors.size) {
            "There was not enough (${method.localVariables.size}) local variable debug information for all parameters" +
                    " (${descriptor.parameterDescriptors.size} of them) in method ${method.name}"
        }

        ApiClass.Method(
            name = method.name, descriptor = descriptor, static = method.isStatic,
            parameterNames = nonThisLocals.take(descriptor.parameterDescriptors.size).map { it.name },
            visibility = method.visibility
        )
    }
    val fields = classNode.fields.map {
        ApiClass.Field(it.name, FieldDescriptor.read(it.desc), it.isStatic, it.visibility)
    }

    val fullClassName = classNode.name
    val packageSplit = fullClassName.lastIndexOf("/")
    val packageName = fullClassName.substring(0, packageSplit).replace("/", ".")
    val className = fullClassName.substring(packageSplit + 1, fullClassName.length)

    //TODO inner classes
    return ApiClass(
        packageName = packageName, className = className, methods = methods.toSet(), fields = fields.toSet(),
        innerClasses = setOf(),
        type = if (classNode.isInterface) ApiClass.Type.Interface else ApiClass.Type.Baseclass
    )
}

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

