package crane.descriptor





private val primitiveStringsToObjects = mapOf(
    "byte" to PrimitiveType.Byte,
    "char" to PrimitiveType.Char,
    "double" to PrimitiveType.Double,
    "float" to PrimitiveType.Float,
    "int" to PrimitiveType.Int,
    "long" to PrimitiveType.Long,
    "short" to PrimitiveType.Short,
    "boolean" to PrimitiveType.Boolean
)



//fun MethodDescriptor.toReadableString() =  "(${parameterDescriptors.joinToString(", ")})"
//fun FieldDescriptor.toReadableString() =  "(${parameterDescriptors.joinToString(", ")})"