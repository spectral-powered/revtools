/*
 * Copyright (C) 2024 Spectral Powered <https://github.com/spectral-powered>
 * @author Kyle Escobar <https://github.com/kyle-escobar>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

@file:Suppress("unused")

package org.spectralpowered.revtools.asm.type

import org.spectralpowered.revtools.asm.ClassGroup
import org.spectralpowered.revtools.asm.Package
import org.spectralpowered.revtools.asm.helper.assert.unreachable
import org.spectralpowered.revtools.asm.ir.Class
import java.lang.Class as JClass

class TypeFactory internal constructor(val group: ClassGroup) {
    val voidType: Type
        get() = VoidType

    val boolType: PrimitiveType
        get() = BoolType

    val byteType: PrimitiveType
        get() = ByteType

    val shortType: PrimitiveType
        get() = ShortType

    val intType: PrimitiveType
        get() = IntType

    val longType: PrimitiveType
        get() = LongType

    val charType: PrimitiveType
        get() = CharType

    val floatType: PrimitiveType
        get() = FloatType

    val doubleType: PrimitiveType
        get() = DoubleType

    @Suppress("MemberVisibilityCanBePrivate")
    val primitiveTypes: Set<PrimitiveType> by lazy {
        setOf(
            boolType,
            byteType,
            shortType,
            intType,
            longType,
            charType,
            floatType,
            doubleType
        )
    }

    val primitiveWrapperTypes: Set<Type>
        get() = primitiveTypes.mapTo(mutableSetOf()) { getWrapper(it) }

    val nullType: Type
        get() = NullType

    fun getRefType(cname: Class): Type = cname.asType
    fun getRefType(cname: String): Type = getRefType(group[cname])
    fun getArrayType(component: Type): Type = component.asArray

    @Suppress("MemberVisibilityCanBePrivate")
    fun getWrapper(type: PrimitiveType): Type = when (type) {
        is BoolType -> boolWrapper
        is ByteType -> byteWrapper
        is CharType -> charWrapper
        is ShortType -> shortWrapper
        is IntType -> intWrapper
        is LongType -> longWrapper
        is FloatType -> floatWrapper
        is DoubleType -> doubleWrapper
    }

    fun getUnwrapped(type: Type): PrimitiveType? = when (type) {
        boolWrapper -> boolType
        byteWrapper -> byteType
        charWrapper -> charType
        shortWrapper -> shortType
        intWrapper -> intType
        longWrapper -> longType
        floatWrapper -> floatType
        doubleWrapper -> doubleType
        else -> null
    }

    fun get(klass: JClass<*>): Type = when {
        klass.isPrimitive -> when (klass) {
            Void::class.java -> voidType
            Boolean::class.javaPrimitiveType -> boolType
            Byte::class.javaPrimitiveType -> byteType
            Char::class.javaPrimitiveType -> charType
            Short::class.javaPrimitiveType -> shortType
            Int::class.javaPrimitiveType -> intType
            Long::class.javaPrimitiveType -> longType
            Float::class.javaPrimitiveType -> floatType
            Double::class.javaPrimitiveType -> doubleType
            else -> unreachable("Unknown primary type $klass")
        }

        klass.isArray -> getArrayType(get(klass.componentType))
        else -> getRefType(group[klass.name.replace(Package.CANONICAL_SEPARATOR, Package.SEPARATOR)])
    }

}

val TypeFactory.classType
    get() = getRefType(SystemTypeNames.classClass)

val TypeFactory.stringType
    get() = getRefType(SystemTypeNames.stringClass)

val TypeFactory.objectType
    get() = getRefType(SystemTypeNames.objectClass)

val TypeFactory.boolWrapper: Type
    get() = getRefType(SystemTypeNames.booleanClass)

val TypeFactory.byteWrapper: Type
    get() = getRefType(SystemTypeNames.byteClass)

val TypeFactory.charWrapper: Type
    get() = getRefType(SystemTypeNames.charClass)

val TypeFactory.shortWrapper: Type
    get() = getRefType(SystemTypeNames.shortClass)

val TypeFactory.intWrapper: Type
    get() = getRefType(SystemTypeNames.integerClass)

val TypeFactory.longWrapper: Type
    get() = getRefType(SystemTypeNames.longClass)

val TypeFactory.floatWrapper: Type
    get() = getRefType(SystemTypeNames.floatClass)

val TypeFactory.doubleWrapper: Type
    get() = getRefType(SystemTypeNames.doubleClass)

val TypeFactory.collectionType: Type
    get() = getRefType(SystemTypeNames.collectionClass)

val TypeFactory.listType: Type
    get() = getRefType(SystemTypeNames.listClass)

val TypeFactory.arrayListType: Type
    get() = getRefType(SystemTypeNames.arrayListClass)

val TypeFactory.linkedListType: Type
    get() = getRefType(SystemTypeNames.linkedListClass)

val TypeFactory.setType: Type
    get() = getRefType(SystemTypeNames.setClass)

val TypeFactory.hashSetType: Type
    get() = getRefType(SystemTypeNames.hashSetClass)

val TypeFactory.treeSetType: Type
    get() = getRefType(SystemTypeNames.treeSetClass)

val TypeFactory.mapType: Type
    get() = getRefType(SystemTypeNames.setClass)

val TypeFactory.hashMapType: Type
    get() = getRefType(SystemTypeNames.hashMapClass)

val TypeFactory.treeMapType: Type
    get() = getRefType(SystemTypeNames.treeMapClass)

val TypeFactory.objectArrayClass
    get() = getRefType(group["$objectType[]"])

val TypeFactory.classLoaderType
    get() = getRefType(SystemTypeNames.classLoader)
