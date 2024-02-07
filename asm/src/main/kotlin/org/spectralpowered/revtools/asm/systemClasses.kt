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

package org.spectralpowered.revtools.asm

import org.spectralpowered.revtools.asm.type.SystemTypeNames


val ClassGroup.classClass
    get() = this[SystemTypeNames.classClass]
val ClassGroup.stringClass
    get() = this[SystemTypeNames.stringClass]
val ClassGroup.objectClass
    get() = this[SystemTypeNames.objectClass]
val ClassGroup.boolWrapper
    get() = this[SystemTypeNames.booleanClass]
val ClassGroup.byteWrapper
    get() = this[SystemTypeNames.byteClass]
val ClassGroup.charWrapper
    get() = this[SystemTypeNames.charClass]
val ClassGroup.shortWrapper
    get() = this[SystemTypeNames.shortClass]
val ClassGroup.intWrapper
    get() = this[SystemTypeNames.integerClass]
val ClassGroup.longWrapper
    get() = this[SystemTypeNames.longClass]
val ClassGroup.floatWrapper
    get() = this[SystemTypeNames.floatClass]
val ClassGroup.doubleWrapper
    get() = this[SystemTypeNames.doubleClass]
val ClassGroup.collectionClass
    get() = this[SystemTypeNames.collectionClass]
val ClassGroup.listClass
    get() = this[SystemTypeNames.listClass]
val ClassGroup.arrayListClass
    get() = this[SystemTypeNames.arrayListClass]
val ClassGroup.linkedListClass
    get() = this[SystemTypeNames.linkedListClass]
val ClassGroup.queueClass
    get() = this[SystemTypeNames.queueClass]
val ClassGroup.dequeClass
    get() = this[SystemTypeNames.dequeClass]
val ClassGroup.arrayDequeClass
    get() = this[SystemTypeNames.arrayDequeClass]
val ClassGroup.setClass
    get() = this[SystemTypeNames.setClass]
val ClassGroup.sortedSetClass
    get() = this[SystemTypeNames.sortedSetClass]
val ClassGroup.navigableSetClass
    get() = this[SystemTypeNames.navigableSetClass]
val ClassGroup.hashSetClass
    get() = this[SystemTypeNames.hashSetClass]
val ClassGroup.treeSetClass
    get() = this[SystemTypeNames.treeSetClass]
val ClassGroup.mapClass
    get() = this[SystemTypeNames.mapClass]
val ClassGroup.sortedMapClass
    get() = this[SystemTypeNames.sortedMapClass]
val ClassGroup.navigableMapClass
    get() = this[SystemTypeNames.navigableMapClass]
val ClassGroup.hashMapClass
    get() = this[SystemTypeNames.hashMapClass]
val ClassGroup.treeMapClass
    get() = this[SystemTypeNames.treeMapClass]
val ClassGroup.classLoaderClass
    get() = this[SystemTypeNames.classLoader]
val ClassGroup.stringBuilderClass
    get() = this[SystemTypeNames.stringBuilder]
val ClassGroup.stringBufferClass
    get() = this[SystemTypeNames.stringBuffer]
val ClassGroup.linkedHashSetClass
    get() = this[SystemTypeNames.linkedHashSet]
val ClassGroup.linkedHashMapClass
    get() = this[SystemTypeNames.linkedHashMap]
val ClassGroup.abstractCollectionClass
    get() = this[SystemTypeNames.abstractCollectionClass]
val ClassGroup.abstractListClass
    get() = this[SystemTypeNames.abstractListClass]
val ClassGroup.abstractQueueClass
    get() = this[SystemTypeNames.abstractQueueClass]
val ClassGroup.abstractSetClass
    get() = this[SystemTypeNames.abstractSetClass]
val ClassGroup.abstractMapClass
    get() = this[SystemTypeNames.abstractMapClass]
val ClassGroup.nullptrClass
    get() = this[SystemTypeNames.nullptrClass]
val ClassGroup.arrayIndexOOBClass
    get() = this[SystemTypeNames.arrayIndexOOBClass]
val ClassGroup.negativeArrayClass
    get() = this[SystemTypeNames.negativeArrayClass]
val ClassGroup.classCastClass
    get() = this[SystemTypeNames.classCastClass]
val ClassGroup.stringIndexOOB
    get() = this[SystemTypeNames.stringIndexOOB]
val ClassGroup.numberFormatClass
    get() = this[SystemTypeNames.numberFormatClass]
val ClassGroup.illegalArgumentClass
    get() = this[SystemTypeNames.illegalArgumentClass]
val ClassGroup.runtimeException
    get() = this[SystemTypeNames.runtimeException]
val ClassGroup.unmodifiableCollection
    get() = this[SystemTypeNames.unmodifiableCollection]
val ClassGroup.unmodifiableList
    get() = this[SystemTypeNames.unmodifiableList]
val ClassGroup.unmodifiableSet
    get() = this[SystemTypeNames.unmodifiableSet]
val ClassGroup.unmodifiableMap
    get() = this[SystemTypeNames.unmodifiableMap]
val ClassGroup.charSequence
    get() = this[SystemTypeNames.charSequence]
val ClassGroup.abstractStringBuilderClass
    get() = this[SystemTypeNames.abstractStringBuilderClass]
val ClassGroup.numberClass
    get() = this[SystemTypeNames.numberClass]
val ClassGroup.atomicBooleanClass
    get() = this[SystemTypeNames.atomicBooleanClass]
val ClassGroup.atomicIntegerClass
    get() = this[SystemTypeNames.atomicIntegerClass]
val ClassGroup.atomicIntegerArrayClass
    get() = this[SystemTypeNames.atomicIntegerArrayClass]
val ClassGroup.atomicLongClass
    get() = this[SystemTypeNames.atomicLongClass]
val ClassGroup.atomicLongArrayClass
    get() = this[SystemTypeNames.atomicLongArrayClass]
val ClassGroup.atomicReferenceClass
    get() = this[SystemTypeNames.atomicReferenceClass]
val ClassGroup.atomicReferenceArrayClass
    get() = this[SystemTypeNames.atomicReferenceArrayClass]
val ClassGroup.atomicStampedReferenceClass
    get() = this[SystemTypeNames.atomicStampedReferenceClass]
