package io.github.mrpotatosse.merkator.modules

import org.koin.dsl.module

val merkatorModules = module {
    includes(
        databaseModule,
        hiboukinModule
    )
}