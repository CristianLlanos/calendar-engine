package com.calendarengine.container

class Dependencies private constructor() : Container {

    companion object {
        fun make(): Container = Dependencies()
    }

    private val definitions = mutableMapOf<Class<*>, Container.() -> Any>()
    private val singletons = mutableMapOf<Class<*>, Any>()

    override fun <T : Any> factory(type: Class<T>, factory: Container.() -> T): Container {
        definitions[type] = factory
        return this
    }

    override fun <T : Any> singleton(type: Class<T>, factory: Container.() -> T): Container {
        definitions[type] = {
            @Suppress("UNCHECKED_CAST")
            singletons.getOrPut(type) { factory() } as T
        }
        return this
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> resolve(type: Class<T>): T {
        val factory = definitions[type]
            ?: throw IllegalArgumentException("Unable to resolve dependency [${type.simpleName}]")
        return factory(this) as T
    }

    override fun register(vararg providers: ServiceProvider): Container {
        providers.forEach { it.register(this) }
        return this
    }
}
