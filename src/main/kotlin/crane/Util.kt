package crane

import crane.codegeneration.Visibility
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarOutputStream

fun Path.exists() = Files.exists(this)
fun Path.deleteIfExists() = Files.deleteIfExists(this)
fun Path.openJar(usage: (FileSystem) -> Unit) = FileSystems.newFileSystem(this, null).use(usage)
fun Path.walk(usage: (Path) -> Unit) = Files.walk(this).forEach(usage)
fun Path.createJar() = JarOutputStream(Files.newOutputStream(this)).close()
fun Path.isDirectory() = Files.isDirectory(this)
fun Path.createDirectory() = Files.createDirectory(this)
fun Path.createDirectories() = Files.createDirectories(this)
fun Path.inputStream() = Files.newInputStream(this)
fun Path.writeBytes(bytes: ByteArray) = Files.write(this, bytes)
fun readToClassNode(classFile: Path): ClassNode = classFile.inputStream().use { stream ->
    ClassNode().also { ClassReader(stream).accept(it, 0) }
}

private infix fun Int.opCode(code: Int): Boolean = (this and code) != 0

infix fun MethodNode.opCode(code: Int) = access opCode code
infix fun FieldNode.opCode(code: Int) = access opCode code
infix fun ClassNode.opCode(code: Int) = access opCode code

val MethodNode.isStatic get() = opCode(Opcodes.ACC_STATIC)
val FieldNode.isStatic get() = opCode(Opcodes.ACC_STATIC)
val ClassNode.isInterface get() = opCode(Opcodes.ACC_INTERFACE)
val MethodNode.isPrivate get() = opCode(Opcodes.ACC_PRIVATE)
val MethodNode.isProtected get() = opCode(Opcodes.ACC_PROTECTED)
val MethodNode.isPublic get() = opCode(Opcodes.ACC_PUBLIC)
val MethodNode.isPackagePrivate get() = !isPrivate && !isProtected && !isPublic
val MethodNode.visibility : Visibility
    get() = when {
        isPrivate -> Visibility.Private
        isProtected -> Visibility.Protected
        isPublic -> Visibility.Public
        isPackagePrivate -> Visibility.Package
        else -> error("MethodNode $name is unexpectedly not private, protected, public, or package private...")
    }

val FieldNode.isPrivate get() = opCode(Opcodes.ACC_PRIVATE)
val FieldNode.isProtected get() = opCode(Opcodes.ACC_PROTECTED)
val FieldNode.isPublic get() = opCode(Opcodes.ACC_PUBLIC)
val FieldNode.isPackagePrivate get() = !isPrivate && !isProtected && !isPublic
val FieldNode.visibility : Visibility
    get() = when {
        isPrivate -> Visibility.Private
        isProtected -> Visibility.Protected
        isPublic -> Visibility.Public
        isPackagePrivate -> Visibility.Package
        else -> error("MethodNode $name is unexpectedly not private, protected, public, or package private...")
    }