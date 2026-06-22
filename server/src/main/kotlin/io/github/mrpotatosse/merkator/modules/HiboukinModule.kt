package io.github.mrpotatosse.merkator.modules

import io.github.mrpotatosse.merkator.hiboukin.services.HiboukinFileReaderService
import io.github.mrpotatosse.merkator.hiboukin.services.HiboukinFileService
import io.github.mrpotatosse.merkator.hiboukin.services.HiboukinMapService
import org.koin.dsl.module

val hiboukinModule = module {
    single { HiboukinFileService() }
    single { HiboukinFileReaderService() }

    single { HiboukinMapService() }
}