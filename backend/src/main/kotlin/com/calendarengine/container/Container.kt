package com.calendarengine.container

interface Container {
    fun <T : Any> factory(type: Class<T>, factory: Container.() -> T): Container
    fun <T : Any> singleton(type: Class<T>, factory: Container.() -> T): Container
    fun <T : Any> resolve(type: Class<T>): T
    fun register(vararg providers: ServiceProvider): Container
}

inline fun <reified T : Any> Container.factory(noinline factory: Container.() -> T): Container =
    factory(T::class.java, factory)

inline fun <reified T : Any> Container.singleton(noinline factory: Container.() -> T): Container =
    singleton(T::class.java, factory)

inline fun <reified T : Any> Container.resolve(): T =
    resolve(T::class.java)
